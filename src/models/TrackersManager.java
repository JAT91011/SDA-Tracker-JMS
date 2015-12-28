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

import entities.Content;
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
	private ConcurrentHashMap<Integer, Boolean>	trackersReadyToSave;

	private Timer								timerSendKeepAlive;
	private Timer								timerCheckKeepAlive;

	private int									idMaster;
	private int									savingCode;
	private String[]							savingData;
	private boolean								savingProcess;

	private TrackersManager() {
		try {
			this.trackers = new ConcurrentHashMap<Integer, Tracker>();
			this.trackersReadyToSave = new ConcurrentHashMap<Integer, Boolean>();

			this.context = new InitialContext();
			this.savingCode = 0;
			this.savingProcess = false;
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
				this.idMaster = this.currentTracker.getId();
				Window.getInstance().setTitle("Tracker [ID: " + this.currentTracker.getId() + "] [Mode: MASTER]");
				Database.getInstance().createDatabase(this.currentTracker.getId());
				loadDatabasePeers();
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

	private synchronized void sendKeepAlive() {
		try {
			sendTopicMessage(Datagram.KEEP_ALIVE);
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	private synchronized void sendDatabase(final int idTracker) {
		try {
			Path path = Paths.get(Constants.DATABASE_FILE_PATH.replace("#", Integer.toString(this.currentTracker.getId())));
			byte[] data = Files.readAllBytes(path);
			Datagram packet = new Datagram(Datagram.DB_REPLICATION, this.currentTracker.getId(), idTracker, data);
			sendQueueMessage(packet, idTracker);
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public synchronized void sendTopicMessage(final int typeDatagram) {
		try {
			ObjectMessage message = this.topicSession.createObjectMessage();
			message.setJMSType("ObjectMessage");
			message.setJMSPriority(1);
			message.setJMSMessageID(Integer.toString(this.currentTracker.getId()));
			message.setObject(new Datagram(typeDatagram, this.currentTracker.getId(), this.currentTracker.isMaster()));
			this.topicPublisher.publish(message);
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public synchronized void sendQueueMessage(final Datagram packet, final int toTrackerId) {
		try {
			ObjectMessage message = this.queueSession.createObjectMessage();
			message.setStringProperty("Filter", Integer.toString(toTrackerId));
			message.setObject(packet);
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
			loadDatabasePeers();
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public synchronized void loadDatabasePeers() {
		try {
			// Carga los peers de la base de datos en memoria
			Vector<Peer> peers = new Vector<Peer>();
			Database.getInstance().createConnection(this.currentTracker.getId());
			ResultSet rs = Database.getInstance().consult("SELECT * FROM PEERS ORDER BY id");
			while (rs.next()) {
				Peer peer = new Peer(rs.getInt("id"), rs.getString("ip"), rs.getInt("port"));
				ResultSet rs2 = Database.getInstance().consult(
						"SELECT C.id, C.hash, PC.percent FROM CONTENTS C INNER JOIN PEER_CONTENT PC ON C.id = PC.id_content WHERE PC.id_peer = "
								+ peer.getId() + " ORDER BY C.id");
				while (rs2.next()) {
					peer.addContent(new Content(rs2.getInt("id"), rs2.getString("hash"), rs2.getInt("percent")));
				}
				peers.add(peer);
			}
			PeersManager.getInstance().addAllPeers(peers);
			PeersManager.getInstance().setConnected(true);

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
			this.trackersReadyToSave.put(tracker.getId(), false);
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
			this.trackersReadyToSave.remove(id);
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
			this.trackersReadyToSave.clear();
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
			this.trackersReadyToSave.put(tracker.getId(), false);
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

	private synchronized void updateTrackerKeepAlive(final int idFrom, final boolean isMaster) {
		try {
			if (trackers.get(idFrom) == null) {
				if (!this.savingProcess) {
					Tracker t = new Tracker(idFrom, isMaster);
					t.setLastKeepAlive(new Date());
					t.setFirstConnection(new Date());
					addTracker(t);
					if (this.currentTracker != null && this.currentTracker.isMaster()) {
						sendDatabase(idFrom);
					}
				}
			} else {
				trackers.get(idFrom).setLastKeepAlive(new Date());
				setTracker(trackers.get(idFrom));
			}
			if (isMaster) {
				idMaster = idFrom;
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
	private synchronized void updateMaster() {
		try {
			int id = getLowerId();
			if (id == currentTracker.getId()) {
				currentTracker.setMaster(true);
				Window.getInstance().setTitle("Tracker [ID: " + this.currentTracker.getId() + "] [Mode: MASTER]");
				System.out.println("########### MASTER ###########");
			}
			idMaster = id;
			trackers.get(getLowerId()).setMaster(true);
			setTracker(trackers.get(getLowerId()));
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	private synchronized void onReadyToSaveReceived(final int idTracker, final int saveCode, final String[] saveData) {
		try {
			if (this.savingCode == 0) {
				System.out.println("El primero quiere guardar: " + saveCode);
				for (String data : saveData) {
					System.out.print(data + " , ");
				}
				System.out.println();
				this.savingCode = saveCode;
				this.savingData = saveData;
				this.trackersReadyToSave.put(idTracker, true);
			} else {
				System.out.println("Los demas quieren guardar: " + saveCode);
				for (String data : saveData) {
					System.out.print(data + " , ");
				}
				System.out.println();
				if (this.savingCode == saveCode) {
					if (this.savingData.length == saveData.length) {
						boolean areEqual = true;
						for (int i = 0; i < this.savingData.length; i++) {
							if (!this.savingData[i].equals(saveData[i])) {
								areEqual = false;
								break;
							}
						}
						if (areEqual) {
							this.trackersReadyToSave.put(idTracker, true);
						}
					}
				}
			}

			boolean allReady = true;
			for (Map.Entry<Integer, Boolean> entry : this.trackersReadyToSave.entrySet()) {
				if (!entry.getValue()) {
					allReady = false;
					break;
				}
			}

			if (allReady) {
				System.out.println("Todos los trackers estan listos para guardar");
				sendTopicMessage(Datagram.SAVE_DATA);
				for (Map.Entry<Integer, Boolean> entry : this.trackersReadyToSave.entrySet()) {
					entry.setValue(false);
				}
				this.savingCode = 0;
				this.savingProcess = false;
			}

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

	public int getIdMaster() {
		return idMaster;
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
						if (this.currentTracker.isMaster()) {
							System.out.println("El master recibe ready to save");
							this.savingProcess = true;
							onReadyToSaveReceived(datagram.getIdTrackerFrom(), datagram.getSaveCode(), datagram.getSaveData());
						}
						break;

					case 3: // SAVE_DATA
						System.out.println("Recibe un save data");
						PeersManager.getInstance().setSaveProcess(true);
						break;

					case 4: // DO_NOT_SAVE_DATA
						PeersManager.getInstance().setSaveProcess(false);
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