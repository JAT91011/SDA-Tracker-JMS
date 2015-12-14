package models;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Observable;

import utilities.ErrorsLog;

public class PeersManager extends Observable implements Runnable {

	private static PeersManager	instance;

	private static int			OK						= 0;
	private static int			NEW_CONNECTION			= 1;
	private static int			ANNOUNCE				= 2;
	private static int			ANNOUNCE_RESPONSE		= 3;
	private static int			ERR						= 99;

	private static int			DATAGRAM_CONTENT_LENGTH	= 2032;
	private static int			DATAGRAM_HEADER_LENGTH	= 16;

	private String				ip;
	private int					port;

	private Thread				readingThread;
	private boolean				enable;

	private MulticastSocket		socket;
	private InetAddress			group;
	private DatagramPacket		messageIn;
	private byte[]				buffer;

	private PeersManager() {
		this.enable = false;
	}

	public void start(final String ip, final int port) {
		try {
			this.ip = ip;
			this.port = port;

			this.socket = new MulticastSocket(port);
			this.group = InetAddress.getByName(this.ip);
			this.socket.joinGroup(group);
			this.enable = true;

			this.readingThread = new Thread(this);
			this.readingThread.start();

		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public boolean stop() {
		try {
			this.socket.leaveGroup(group);
			this.enable = false;
			return true;
		} catch (IOException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return false;
		}
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
			int length = data != null ? data.length : 0;
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
				if (data != null) {
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

	public void processData(final byte[] data) {
		try {
			int code = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
			System.out.println("Codigo recibido: " + code);
			switch (code) {
				case 0: // OK
					break;

				case 1: // NEW_CONNECTION
					sendData(createDatagram(OK, null)[0]);
					break;

				case 2: // ANNOUNCE

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

	public static PeersManager getInstance() {
		if (instance == null) {
			instance = new PeersManager();
		}
		return instance;
	}
}