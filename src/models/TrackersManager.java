package models;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

import entities.Tracker;
import utilities.Database;
import utilities.ErrorsLog;
import utilities.Properties;
import views.Window;

/**
 * Implementa la funcionalidad especifica del protocolo UDP de un tracker
 * bitTorrent.
 */

public class TrackersManager extends Observable implements Runnable {

	private static int								DATAGRAM_CONTENT_LENGTH	= 2032;
	private static int								DATAGRAM_HEADER_LENGTH	= 16;

	private String									ip;
	private int										port;
	private static TrackersManager					instance;

	private boolean									enable;
	private Tracker									currentTracker;
	private ConcurrentHashMap<Integer, Tracker>		trackers;
	private ConcurrentHashMap<Integer, byte[][]>	trackersDb;
	private ArrayList<byte[]>						databaseDatagram;
	private boolean									isDatabaseCreated;

	private Thread									readingThread;
	private Timer									timerSendKeepAlive;
	private Timer									timerCheckKeepAlive;

	private MulticastSocket							socket;
	private InetAddress								group;
	private DatagramPacket							messageIn;
	private byte[]									buffer;

	private TrackersManager() {
		this.enable = false;
		this.trackers = new ConcurrentHashMap<Integer, Tracker>();
		this.trackersDb = new ConcurrentHashMap<Integer, byte[][]>();
		this.databaseDatagram = new ArrayList<byte[]>();
		this.isDatabaseCreated = false;

		this.timerSendKeepAlive = new Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					sendData(createDatagram(5, ByteBuffer.allocate(4).putInt(currentTracker.getId()).array())[0]);
				} catch (Exception ex) {
					ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
					}.getClass().getEnclosingMethod().getName(), ex.toString());
					ex.printStackTrace();
				}
			}
		});

		this.timerCheckKeepAlive = new Timer(2000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					for (Map.Entry<Integer, Tracker> entry : trackers.entrySet()) {
						if (entry.getValue().getDifferenceBetweenKeepAlive() > 2) {
							// System.out.println("Has tardado: " +
							// entry.getValue().getId());
							removeTracker(entry.getValue().getId());
							if (entry.getValue().isMaster()) {
								updateMaster();
							}
						}
					}
				} catch (Exception ex) {
					ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
					}.getClass().getEnclosingMethod().getName(), ex.toString());
					ex.printStackTrace();
				}
			}
		});
	}

	/**
	 * Metodo para conectarse con los demas trackers
	 * 
	 * @param ip
	 *            IP Multicast
	 * @param port
	 *            Puerto de conexion
	 * @return Si se ha conectado o no
	 */
	public boolean connect(final String ip, final int port) {
		try {
			this.ip = ip;
			this.port = port;

			this.socket = new MulticastSocket(port);
			this.group = InetAddress.getByName(this.ip);
			this.socket.joinGroup(group);
			this.enable = true;
			return true;
		} catch (IOException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Metodo para cerrar la conexion
	 * 
	 * @return Si se ha desconectado o no
	 */
	public boolean disconnect() {
		try {
			this.timerSendKeepAlive.stop();
			this.timerCheckKeepAlive.stop();
			this.socket.leaveGroup(group);
			this.enable = false;
			this.removeTrackers();
			return true;
		} catch (IOException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Funcion para obtener la primera ID disponible
	 * 
	 * @return La primera ID disponible
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
			this.trackersDb.remove(id);
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

	/**
	 * Se procesan los datos recibidos
	 * 
	 * @param datos
	 *            Datos recibidos
	 */
	public void processData(final byte[] data) {
		try {
			int code = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
			// System.out.println("Codigo recibido: " + code);
			switch (code) {
				case 0: // OK
					if (this.currentTracker.isMaster()) {
						processOkMessage(data);
					}
					break;

				case 1: // DB REPLICATION
					if (!isDatabaseCreated) {
						loadDatabaseData(data);
					}
					break;

				case 2: // READY_TO_SAVE

					break;

				case 3: // SAVE_DATA

					break;

				case 4: // DO_NOT_SAVE_DATA

					break;

				case 5: // KEEP_ALIVE
					updateTrackerKeepAlive(ByteBuffer.wrap(Arrays.copyOfRange(data, 16, 20)).getInt());
					break;

				case 99: // ERR

					break;
			}
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
		// System.out.println("Datos recibidos: " + Arrays.toString(data));
	}

	private synchronized void processOkMessage(final byte[] data) {
		int length = ByteBuffer.wrap(Arrays.copyOfRange(data, 12, 16)).getInt();
		int id = ByteBuffer.wrap(Arrays.copyOfRange(data, 16, 16 + length)).getInt();

		if (length == 4) {
			this.trackersDb.remove(id);
			System.out.println("Ha concluido la transmision de la base de datos.");
		} else if (length == 8) {
			int requestPartition = ByteBuffer.wrap(Arrays.copyOfRange(data, 20, 16 + length)).getInt();
			System.out.println("Request partition: " + requestPartition);
			sendData(this.trackersDb.get(id)[requestPartition]);
			System.out.println("Te he enviado la siguiente particion, numero: " + requestPartition);
		}
	}

	private synchronized void loadDatabaseData(final byte[] data) {
		try {
			int totalPartitions = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).getInt();
			System.out.println("Total partitions: " + totalPartitions);
			int currentPartition = ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 12)).getInt();
			System.out.println("Current partition: " + currentPartition);
			int contentLength = ByteBuffer.wrap(Arrays.copyOfRange(data, 12, 16)).getInt();
			byte[] packet = Arrays.copyOfRange(data, 16, contentLength);
			this.databaseDatagram.add(packet);

			byte[] id = ByteBuffer.allocate(4).putInt(currentTracker.getId()).array();

			// Si existen mas particiones se piden
			if (currentPartition < totalPartitions) {
				byte[] requestPartition = ByteBuffer.allocate(4).putInt(currentPartition).array();
				byte[] response = new byte[id.length + requestPartition.length];

				System.arraycopy(id, 0, response, 0, id.length);
				System.arraycopy(requestPartition, 0, response, id.length, requestPartition.length);

				sendData(createDatagram(0, response)[0]);
			} else {
				sendData(createDatagram(0, id)[0]);

				int totalLength = 0;
				for (int i = 0; i < this.databaseDatagram.size(); i++) {
					totalLength += this.databaseDatagram.get(i).length;
				}

				byte[] databaseData = new byte[totalLength];

				int h = 0;
				for (int i = 0; i < this.databaseDatagram.size(); i++) {
					for (int j = 0; j < this.databaseDatagram.get(i).length; j++) {
						databaseData[h] = this.databaseDatagram.get(i)[j];
						h++;
					}
				}

				// Crea base datos
				FileOutputStream fileOuputStream = new FileOutputStream(
						Properties.getDatabasePath().replace("#", Integer.toString(this.currentTracker.getId())));
				fileOuputStream.write(databaseData);
				isDatabaseCreated = true;
			}
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	public synchronized void updateTrackerKeepAlive(final int id) {
		try {
			// System.out.println("ID Recibida: " + id);
			if (trackers.get(id) == null) {
				// System.out.println("Nuevo tracker encontrado");
				Tracker t = new Tracker(id, false);
				t.setLastKeepAlive(new Date());
				t.setFirstConnection(new Date());
				addTracker(t);
				if (this.currentTracker != null && this.currentTracker.isMaster()) {
					// Enviar fichero db
					System.out.println("Te envio fichero tracker con la id: " + t.getId());

					Path path = Paths.get(
							Properties.getDatabasePath().replace("#", Integer.toString(this.currentTracker.getId())));
					byte[] data = Files.readAllBytes(path);
					System.out.println("Tamaño fichero: " + data.length);

					byte[][] datagram = createDatagram(1, data);
					this.trackersDb.put(t.getId(), datagram);

					sendData(this.trackersDb.get(t.getId())[0]);
				}
			} else {
				trackers.get(id).setLastKeepAlive(new Date());
				setTracker(trackers.get(id));
			}
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Se crea la trama de datos para enviar con el formato correcto
	 * 
	 * @param codigo
	 *            Codigo de la trama
	 * @param datos
	 *            Datos que se van a enviar
	 * @return Array con la trama formateada se utiliza un array bidimensional
	 *         dado que la trama puede estar particionada
	 */
	public byte[][] createDatagram(int code, byte[] data) {
		byte[][] datagrams = null;
		try {
			int length = data.length;
			int partitions = 0;
			if (length < (DATAGRAM_CONTENT_LENGTH)) {
				partitions = 1;
			} else {
				partitions = length / DATAGRAM_CONTENT_LENGTH;
				if (length % DATAGRAM_CONTENT_LENGTH > 0) {
					partitions++;
				}
			}

			datagrams = new byte[partitions][DATAGRAM_CONTENT_LENGTH + DATAGRAM_HEADER_LENGTH];
			for (int i = 0; i < partitions; i++) {

				// CODE
				byte[] codeArray = ByteBuffer.allocate(4).putInt(code).array();
				datagrams[i][0] = codeArray[0];
				datagrams[i][1] = codeArray[1];
				datagrams[i][2] = codeArray[2];
				datagrams[i][3] = codeArray[3];

				// PARTITIONS
				byte[] codePartitions = ByteBuffer.allocate(4).putInt(partitions).array();
				datagrams[i][4] = codePartitions[0];
				datagrams[i][5] = codePartitions[1];
				datagrams[i][6] = codePartitions[2];
				datagrams[i][7] = codePartitions[3];

				// CURRENT PARTITION
				byte[] codeCurrentPartition = ByteBuffer.allocate(4).putInt(i + 1).array();
				datagrams[i][8] = codeCurrentPartition[0];
				datagrams[i][9] = codeCurrentPartition[1];
				datagrams[i][10] = codeCurrentPartition[2];
				datagrams[i][11] = codeCurrentPartition[3];

				// LENGTH
				if (i + 1 == partitions) {
					byte[] lengthArray = ByteBuffer.allocate(4).putInt(length).array();
					datagrams[i][12] = lengthArray[0];
					datagrams[i][13] = lengthArray[1];
					datagrams[i][14] = lengthArray[2];
					datagrams[i][15] = lengthArray[3];

					for (int j = 0; j < length - (DATAGRAM_CONTENT_LENGTH * i); j++) {
						datagrams[i][DATAGRAM_HEADER_LENGTH + j] = data[j + (DATAGRAM_CONTENT_LENGTH * i)];
					}
				} else {
					byte[] lengthArray = ByteBuffer.allocate(4).putInt(DATAGRAM_CONTENT_LENGTH).array();
					datagrams[i][12] = lengthArray[0];
					datagrams[i][13] = lengthArray[1];
					datagrams[i][14] = lengthArray[2];
					datagrams[i][15] = lengthArray[3];

					for (int j = DATAGRAM_CONTENT_LENGTH * i; j < DATAGRAM_CONTENT_LENGTH * (i + 1); j++) {
						datagrams[i][DATAGRAM_HEADER_LENGTH + (j - (i * DATAGRAM_CONTENT_LENGTH))] = data[j];
					}
				}
			}

			// System.out.println("Datagrama creado");
			// for (int i = 0; i < partitions; i++) {
			// System.out.println("Datagrama " + (i + 1) + ": " +
			// Arrays.toString(datagrams[i]));
			// }

		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}

		return datagrams;
	}

	/**
	 * Metodo para enviar un datagrama
	 * 
	 * @param data
	 *            Datagrama a enviar
	 */
	public synchronized void sendData(byte[] data) {
		try {
			DatagramPacket message = new DatagramPacket(data, data.length, group, this.port);
			socket.send(message);
		} catch (IOException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public void start() {
		try {
			this.timerCheckKeepAlive.start();
			this.readingThread = new Thread(this);
			this.readingThread.start();

			int wait = 1;
			while (wait <= 3) {
				Thread.sleep(1000);
				wait++;
			}

			int min = getLowerId();
			if (min != 0) {
				trackers.get(min).setMaster(true);
			}
			this.currentTracker = new Tracker(getAvailableId(), this.trackers.size() == 0);
			this.currentTracker.setFirstConnection(new Date());
			if (this.currentTracker.isMaster()) {
				Window.getInstance().setTitle("Tracker [ID: " + this.currentTracker.getId() + "] [Mode: MASTER]");
				System.out.println("Crea base datos");
				Database.getInstance().createDatabase(this.currentTracker.getId());
				this.isDatabaseCreated = true;
			} else {
				Window.getInstance().setTitle("Tracker [ID: " + this.currentTracker.getId() + "] [Mode: SLAVE]");
			}
			addTracker(currentTracker);
			this.timerSendKeepAlive.start();

		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
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

	/**
	 * Hilo principal para procesar y enviar informacion a los trackers
	 */
	@Override
	public void run() {
		try {
			while (this.enable) {
				this.buffer = new byte[DATAGRAM_CONTENT_LENGTH + DATAGRAM_HEADER_LENGTH];
				this.messageIn = new DatagramPacket(buffer, buffer.length);
				this.socket.receive(messageIn);
				processData(this.buffer);
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public int getTotalTrackers() {
		return trackers.size();
	}

	public static TrackersManager getInstance() {
		if (instance == null) {
			instance = new TrackersManager();
		}
		return instance;
	}

	public static void main(String[] strings) {
		TrackersManager.getInstance().connect("228.5.6.7", 5000);
		TrackersManager.getInstance().start();
	}
}