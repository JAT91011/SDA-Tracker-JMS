package models;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import bitTorrent.tracker.protocol.udp.messages.AnnounceRequest;
import bitTorrent.tracker.protocol.udp.messages.AnnounceResponse;
import bitTorrent.tracker.protocol.udp.messages.BitTorrentUDPMessage;
import bitTorrent.tracker.protocol.udp.messages.BitTorrentUDPMessage.Action;
import bitTorrent.tracker.protocol.udp.messages.ConnectRequest;
import bitTorrent.tracker.protocol.udp.messages.ConnectResponse;
import bitTorrent.tracker.protocol.udp.messages.PeerInfo;
import entities.Peer;
import utilities.Database;
import utilities.ErrorsLog;

public class PeersManager extends Observable implements Runnable {

	private static PeersManager					instance;

	private static int							DATAGRAM_LENGTH	= 2048;
	private static int							INTERVAL		= 1000;

	private Thread								readingThread;
	private boolean								enable;

	private MulticastSocket						socket;
	private InetAddress							group;
	private DatagramPacket						messageIn;
	private byte[]								buffer;

	private ConcurrentHashMap<Integer, Peer>	peers;

	private PeersManager() {
		this.enable = false;
		this.peers = new ConcurrentHashMap<Integer, Peer>();
	}

	public void start(final String ip, final int port) {
		try {
			this.socket = new MulticastSocket(port);
			this.group = InetAddress.getByName(ip);
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
	 * @param message
	 *            Mensaje a enviar
	 */
	public synchronized void sendData(final BitTorrentUDPMessage message, final InetAddress ip, final int port) {
		try {
			DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, ip, port);
			socket.send(packet);
		} catch (IOException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se ha desconectado un peer
	 * 
	 * @param peer
	 *            Peer que se ha desconectado
	 */
	public synchronized void removePeer(int id) {
		try {
			this.peers.remove(id);
			setChanged();
			notifyObservers();
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se ha desconectado el peer operativo
	 * 
	 * @param peer
	 *            Peer que se ha desconectado
	 */
	public synchronized void removePeers() {
		try {
			this.peers.clear();
			setChanged();
			notifyObservers();
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se han alterado los datos de un peer
	 * 
	 * @param peer
	 */
	public synchronized void setPeer(Peer peer) {
		try {
			this.peers.put(peer.getId(), peer);
			setChanged();
			notifyObservers();
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para obtener los peers activos
	 * 
	 * @return Vector con los peers activos
	 */
	public synchronized ConcurrentHashMap<Integer, Peer> getPeers() {
		return instance.peers;
	}

	public void processData(final DatagramPacket messageIn, final InetAddress ip, final int port) {
		try {
			ByteBuffer bufferReceive = ByteBuffer.wrap(messageIn.getData());
			Action action = Action.valueOf(bufferReceive.getInt(8));
			switch (action) {
				case ANNOUNCE:
					System.out.println("Announce recibido");
					AnnounceRequest announceRequest = AnnounceRequest.parse(messageIn.getData());
					AnnounceResponse announceResponse = new AnnounceResponse();
					announceResponse.setTransactionId(announceRequest.getTransactionId());
					announceResponse.setInterval(INTERVAL);
					announceResponse.setLeechers(0);
					announceResponse.setSeeders(1);
					List<PeerInfo> lPeerInfo = new ArrayList<>();
					announceResponse.setPeers(lPeerInfo);
					sendData(announceResponse, ip, port);

					break;

				case CONNECT:
					System.out.println("Connect recibido");
					if (TrackersManager.getInstance().getCurrentTracker().isMaster()) {
						if (addPeer(ip.getHostAddress(), port)) {
							ConnectRequest connectRequest = ConnectRequest.parse(messageIn.getData());
							ConnectResponse connectResponse = new ConnectResponse();
							connectResponse.setTransactionId(connectRequest.getTransactionId());
							connectResponse.setConnectionId(connectRequest.getConnectionId());
							sendData(connectResponse, ip, port);
						}
					} else {
						addPeer(ip.getHostAddress(), port);
					}
					break;
				case ERROR:
					System.out.println("ERROR");
					break;
				case SCRAPE:
					System.out.println("SCRAPE");
					break;
				default:
					break;
			}
		} catch (Exception ex) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se ha insertado un nuevo peer
	 */
	private boolean addPeer(final String ip, final int port) {
		try {
			if (Database.getInstance().count("PEERS", "ip = '" + ip + "' AND port = " + Integer.toString(port)) == 0) {
				Database.getInstance().update("INSERT INTO PEERS (ip, port) VALUES ('" + ip + "', " + Integer.toString(port) + ")");
				int id = Database.getInstance().consult("SELECT id FROM PEERS WHERE ip = '" + ip + "' AND port = " + Integer.toString(port))
						.getInt("id");
				this.peers.put(id, new Peer(id, ip, port));
				setChanged();
				notifyObservers();
				return true;
			}
		} catch (SQLException e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Funcion para notificar que se han insertado todos los peers
	 */
	public void addAllPeers(final Vector<Peer> peers) {
		try {
			for (Peer p : peers) {
				this.peers.put(p.getId(), p);
			}
			setChanged();
			notifyObservers();
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			while (this.enable) {
				this.buffer = new byte[DATAGRAM_LENGTH];
				this.messageIn = new DatagramPacket(buffer, buffer.length);

				this.socket.receive(messageIn);
				System.out.println("Cliente: " + messageIn.getAddress().getHostAddress() + " " + messageIn.getPort());
				processData(this.messageIn, messageIn.getAddress(), messageIn.getPort());
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