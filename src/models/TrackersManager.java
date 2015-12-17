package models;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.Timer;

import org.apache.activemq.command.ActiveMQObjectMessage;

import entities.Datagram;
import entities.Peer;
import entities.Tracker;
import utilities.Constants;
import utilities.Database;
import utilities.ErrorsLog;
import views.Window;

/**
 * Implementa la funcionalidad especifica de un tracker bitTorrent.
 */

public class TrackersManager extends Observable implements MessageListener {

	private static TrackersManager				instance;

	private Context								context;
	private TopicConnectionFactory				topicConnectionFactory;
	private QueueConnectionFactory				queueConnectionFactory;

	private Topic								topic;
	private TopicConnection						topicConnection;
	private TopicSession						topicSession;
	private TopicSubscriber						topicSubscriber;
	private TopicPublisher						topicPublisher;

	private Queue								queue;
	private QueueConnection						queueConnection;
	private QueueSession						queueSession;
	private QueueReceiver						queueReceiver;
	private QueueSender							queueSender;

	private Tracker								currentTracker;
	private ConcurrentHashMap<Integer, Tracker>	trackers;

	private Timer								timerSendKeepAlive;
	private Timer								timerCheckKeepAlive;

	private TrackersManager() {
		try {
			this.trackers = new ConcurrentHashMap<Integer, Tracker>();
			this.context = new InitialContext();
			this.topicConnectionFactory = (TopicConnectionFactory) this.context.lookup(Constants.TOPIC_CONNECTION_FACTORY_NAME);
			this.queueConnectionFactory = (QueueConnectionFactory) this.context.lookup(Constants.QUEUE_CONNECTION_FACTORY_NAME);

			this.timerSendKeepAlive = new Timer(1000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					sendKeepAlive();
				}
			});

			this.timerCheckKeepAlive = new Timer(2000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (Map.Entry<Integer, Tracker> entry : trackers.entrySet()) {
						if (entry.getValue().getDifferenceBetweenKeepAlive() > 2) {
							removeTracker(entry.getValue().getId());
							if (entry.getValue().isMaster()) {
								updateMaster();
							}
						}
					}
				}
			});

		} catch (NamingException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}

	}

	public boolean start() {
		try {
			this.topic = (Topic) this.context.lookup(Constants.TOPIC_NAME);
			this.topicConnection = this.topicConnectionFactory.createTopicConnection();
			this.topicSession = this.topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			this.topicPublisher = this.topicSession.createPublisher(this.topic);
			this.topicSubscriber = this.topicSession.createSubscriber(this.topic);

			this.topicSubscriber.setMessageListener(this);
			this.topicConnection.start();

			this.queue = (Queue) this.context.lookup(Constants.QUEUE_NAME);
			this.queueConnection = this.queueConnectionFactory.createQueueConnection();
			this.queueSession = this.queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			this.queueSender = this.queueSession.createSender(this.queue);

			this.timerCheckKeepAlive.start();
			Thread.sleep(3000);

			this.currentTracker = new Tracker(getAvailableId(), this.trackers.size() == 0);
			this.currentTracker.setFirstConnection(new Date());
			if (this.currentTracker.isMaster()) {
				Window.getInstance().setTitle("Tracker [ID: " + this.currentTracker.getId() + "] [Mode: MASTER]");
				Database.getInstance().createDatabase(this.currentTracker.getId());
			} else {
				Window.getInstance().setTitle("Tracker [ID: " + this.currentTracker.getId() + "] [Mode: SLAVE]");
			}
			addTracker(currentTracker);

			this.queueReceiver = this.queueSession.createReceiver(this.queue, "Filter = '" + Integer.toString(this.currentTracker.getId()) + "'");
			this.queueReceiver.setMessageListener(this);
			this.queueConnection.start();

			this.timerSendKeepAlive.start();

			return true;
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return false;
		}
	}

	public boolean stop() {
		try {
			this.timerSendKeepAlive.stop();
			this.timerCheckKeepAlive.stop();

			this.topicPublisher.close();
			this.topicSubscriber.close();
			this.topicSession.close();
			this.topicConnection.close();

			this.queueSender.close();
			this.queueReceiver.close();
			this.queueSession.close();
			this.queueConnection.close();

			this.removeTrackers();

			return true;
		} catch (JMSException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return false;
		}
	}

	public synchronized void sendKeepAlive() {
		try {
			ObjectMessage message = this.topicSession.createObjectMessage();
			message.setJMSType("ObjectMessage");
			message.setJMSPriority(1);
			message.setJMSMessageID(Integer.toString(this.currentTracker.getId()));
			message.setObject(new Datagram(Datagram.KEEP_ALIVE, this.currentTracker.getId(), this.currentTracker.isMaster()));
			this.topicPublisher.publish(message);
		} catch (JMSException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public synchronized void sendDatabase(final int idTracker) {
		try {
			ObjectMessage message = this.queueSession.createObjectMessage();
			message.setStringProperty("Filter", Integer.toString(idTracker));
			Path path = Paths.get(Constants.DATABASE_FILE_PATH.replace("#", Integer.toString(this.currentTracker.getId())));
			byte[] data = Files.readAllBytes(path);
			message.setObject(new Datagram(Datagram.DB_REPLICATION, this.currentTracker.getId(), idTracker, data));
			this.queueSender.send(message);
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public synchronized void loadDatabase(final byte[] database) {
		try {
			// Crea la base de datos
			FileOutputStream fileOuputStream = new FileOutputStream(
					Constants.DATABASE_FILE_PATH.replace("#", Integer.toString(this.currentTracker.getId())));
			fileOuputStream.write(database);
			fileOuputStream.close();

			// Carga los peers de la base de datos en memoria
			Vector<Peer> peers = new Vector<Peer>();
			Database.getInstance().createConnection(this.currentTracker.getId());
			ResultSet rs = Database.getInstance().consult("SELECT * FROM PEERS ORDER BY id");
			while (rs.next()) {
				peers.add(new Peer(rs.getInt("id"), rs.getString("ip"), rs.getInt("port")));
			}
			PeersManager.getInstance().addAllPeers(peers);
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Funcion para obtener la primera ID disponible
	 * 
	 * @return La primera ID disponibleloadDatabase
	 */
	public synchronized int getAvailableId() {
		try {
			for (int id = 1; id < Integer.MAX_VALUE; id++) {
				if (!trackers.containsKey(id)) {
					return id;
				}
			}
			return -1;
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
			return -1;
		}
	}

	/**
	 * Funcion para notificar que se ha insertado un nuevo tracker
	 */
	public synchronized void addTracker(Tracker tracker) {
		try {
			this.trackers.put(tracker.getId(), tracker);
			setChanged();
			notifyObservers();
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se ha desconectado un tracker
	 * 
	 * @param tracker
	 *            Tracker que se ha desconectado
	 */
	public synchronized void removeTracker(int id) {
		try {
			this.trackers.remove(id);
			setChanged();
			notifyObservers();
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se ha desconectado el tracker operativo
	 * tracker
	 * 
	 * @param tracker
	 *            Tracker que se ha desconectado
	 */
	public synchronized void removeTrackers() {
		try {
			this.trackers.clear();
			setChanged();
			notifyObservers();
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se han alterado los datos de un tracker
	 * 
	 * @param tracker
	 */
	public synchronized void setTracker(Tracker tracker) {
		try {
			this.trackers.put(tracker.getId(), tracker);
			setChanged();
			notifyObservers();
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para obtener los trackers activos
	 * 
	 * @return Vector con los trackers activos
	 */
	public synchronized ConcurrentHashMap<Integer, Tracker> getTrackers() {
		return instance.trackers;
	}

	public synchronized void updateTrackerKeepAlive(final int idFrom, final boolean isMaster) {
		try {
			if (trackers.get(idFrom) == null) {
				Tracker t = new Tracker(idFrom, isMaster);
				t.setLastKeepAlive(new Date());
				t.setFirstConnection(new Date());
				addTracker(t);
				if (this.currentTracker != null && this.currentTracker.isMaster()) {
					sendDatabase(idFrom);
				}
			} else {
				trackers.get(idFrom).setLastKeepAlive(new Date());
				setTracker(trackers.get(idFrom));
			}
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	public synchronized int getLowerId() {
		try {
			int id = 0;
			if (!trackers.isEmpty()) {
				id = Integer.MAX_VALUE;
			}
			for (Map.Entry<Integer, Tracker> entry : trackers.entrySet()) {
				if (entry.getKey() < id) {
					id = entry.getKey();
				}
			}
			return id;
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Funcion para actualizar y nombrar el nuevo Master
	 * 
	 */
	public synchronized void updateMaster() {
		try {
			int id = getLowerId();
			if (id == currentTracker.getId()) {
				currentTracker.setMaster(true);
				Window.getInstance().setTitle("Tracker [ID: " + this.currentTracker.getId() + "] [Mode: MASTER]");
			}
			trackers.get(getLowerId()).setMaster(true);
			setTracker(trackers.get(getLowerId()));
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	public int getTotalTrackers() {
		return trackers.size();
	}

	public Tracker getCurrentTracker() {
		return currentTracker;
	}

	public static TrackersManager getInstance() {
		if (instance == null) {
			instance = new TrackersManager();
		}
		return instance;
	}

	public synchronized void onMessage(Message message) {
		try {
			if (message != null && message.getClass().getCanonicalName().equals(ActiveMQObjectMessage.class.getCanonicalName())) {
				Datagram datagram = (Datagram) ((ObjectMessage) message).getObject();
				switch (datagram.getId()) {

					case 1: // DB_REPLICATION
						loadDatabase(datagram.getContent());

						break;

					case 2: // READY_TO_SAVE

						break;

					case 3: // SAVE_DATA

						break;

					case 4: // DO_NOT_SAVE_DATA

						break;

					case 5: // KEEP_ALIVE
						updateTrackerKeepAlive(datagram.getIdTrackerFrom(), datagram.isMaster());
						break;

					case 9: // ERROR

						break;
				}
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}
}