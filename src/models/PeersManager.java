package models;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import bitTorrent.tracker.protocol.udp.messages.AnnounceRequest;
import bitTorrent.tracker.protocol.udp.messages.AnnounceRequest.Event;
import bitTorrent.tracker.protocol.udp.messages.AnnounceResponse;
import bitTorrent.tracker.protocol.udp.messages.BitTorrentUDPMessage;
import bitTorrent.tracker.protocol.udp.messages.BitTorrentUDPMessage.Action;
import bitTorrent.tracker.protocol.udp.messages.ConnectRequest;
import bitTorrent.tracker.protocol.udp.messages.ConnectResponse;
import bitTorrent.tracker.protocol.udp.messages.PeerInfo;
import bitTorrent.util.ByteUtils;
import entities.Content;
import entities.Datagram;
import entities.Peer;
import utilities.Database;
import utilities.ErrorsLog;

public class PeersManager extends Observable implements Runnable {

	private static PeersManager	instance;

	private static int			DATAGRAM_LENGTH			= 2048;
	private static int			INTERVAL				= 1000;
	private static int			READY_TO_SAVE_TIMEOUT	= 3000;

	public enum SaveProcess {
		BLOCK(0), SAVE(1), NOT_SAVE(2);

		public int value;

		private SaveProcess(int value) {
			this.value = value;
		}
	}

	private Thread								readingThread;
	private boolean								enable;
	private boolean								connected;

	private MulticastSocket						socket;
	private InetAddress							group;
	private DatagramPacket						messageIn;
	private byte[]								buffer;

	private ConcurrentHashMap<Integer, Peer>	peers;
	private SaveProcess							saveProcess;

	private PeersManager() {
		this.connected = false;
		this.enable = false;
		this.saveProcess = SaveProcess.BLOCK;
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
			this.socket.close();
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
					onAnnounceRequestReceived(AnnounceRequest.parse(messageIn.getData()), ip, port);
					break;

				case CONNECT:
					System.out.println("Connect recibido");
					onConnectRequestReceived(ip, port);
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
		} catch (

		Exception ex)

		{
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), ex.toString());
			ex.printStackTrace();
		}

	}

	private void onConnectRequestReceived(final InetAddress ip, final int port) {
		try {
			addPeer(ip.getHostAddress(), port);
			if (this.saveProcess == SaveProcess.SAVE) {
				ConnectRequest connectRequest = ConnectRequest.parse(messageIn.getData());
				ConnectResponse connectResponse = new ConnectResponse();
				connectResponse.setTransactionId(connectRequest.getTransactionId());
				connectResponse.setConnectionId(connectRequest.getConnectionId());
				sendData(connectResponse, ip, port);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onAnnounceRequestReceived(final AnnounceRequest announceRequest, final InetAddress ip, final int port) {
		try {
			boolean sendAnnounceResponse = false;
			// Esta descargando el contenido
			if (announceRequest.getEvent() == Event.STARTED) {
				boolean existContent = Database.getInstance().count("CONTENTS", "hash = '" + announceRequest.getHexInfoHash() + "'") != 0;

				if (!existContent) {
					// No existe el contenido y se añade
					addContent(ip.getHostAddress(), port, announceRequest.getHexInfoHash());
				} else {
					// Existe el contenido
					int idContent = Database.getInstance().consult("SELECT id FROM CONTENTS WHERE hash = '" + announceRequest.getHexInfoHash() + "'")
							.getInt("id");
					int idPeer = 0;
					for (Entry<Integer, Peer> entry : this.peers.entrySet()) {
						if (entry.getValue().getIp().equals(ip.getHostAddress()) && entry.getValue().getPort() == port) {
							idPeer = entry.getKey();
							break;
						}
					}
					if (Database.getInstance().count("PEER_CONTENT", "id_peer = " + idPeer + " AND id_content = " + idContent) == 0) {
						// No existe la relacion entre peer y contenido
						addRelation(idPeer, idContent, announceRequest.getHexInfoHash());
					} else {
						// Existe la relacion y se actualiza el porcentaje
						updateDownloaded(idPeer, idContent, announceRequest.getHexInfoHash(),
								(int) ((announceRequest.getDownloaded() * 100) / (announceRequest.getDownloaded() + announceRequest.getLeft())));
						sendAnnounceResponse = true;
					}
				}
			}
			// Esta compartiendo el contenido
			else if (announceRequest.getEvent() == Event.COMPLETED) {
				int idContent = Database.getInstance().consult("SELECT id FROM CONTENTS WHERE hash = '" + announceRequest.getHexInfoHash() + "'")
						.getInt("id");
				int idPeer = 0;
				for (Entry<Integer, Peer> entry : this.peers.entrySet()) {
					if (entry.getValue().getIp().equals(ip.getHostAddress()) && entry.getValue().getPort() == port) {
						idPeer = entry.getKey();
						break;
					}
				}
				boolean completed = false;
				for (Content content : this.peers.get(idPeer).getContents()) {
					if (content.getInfoHash().equals(announceRequest.getHexInfoHash())) {
						completed = content.getPercent() == 100;
						break;
					}
				}
				if (!completed) {
					updateDownloaded(idPeer, idContent, announceRequest.getHexInfoHash(), 100);
					sendAnnounceResponse = true;
				}
			}

			if (this.saveProcess == SaveProcess.SAVE && sendAnnounceResponse) {
				int leechers = 0;
				int seeders = 0;
				List<PeerInfo> lPeerInfo = new ArrayList<>();
				ResultSet rs = Database.getInstance().consult(
						"SELECT P.ip, P.port, PC.percent FROM PEERS P INNER JOIN PEER_CONTENT PC ON P.id = PC.id_peer INNER JOIN CONTENTS C ON PC.id_content = C.id WHERE C.hash = '"
								+ announceRequest.getHexInfoHash() + "'");
				while (rs.next()) {
					if (!rs.getString("ip").equals(ip.getHostAddress()) || rs.getInt("port") != port) {
						PeerInfo peer = new PeerInfo();
						peer.setIpAddress(ByteUtils.arrayToInt(InetAddress.getByName(rs.getString("ip")).getAddress()));
						peer.setPort(rs.getInt("port"));
						if (rs.getInt("percent") > 0) {
							seeders++;
						} else {
							leechers++;
						}
						lPeerInfo.add(peer);
					}
				}

				AnnounceResponse announceResponse = new AnnounceResponse();
				announceResponse.setTransactionId(announceRequest.getTransactionId());
				announceResponse.setInterval(INTERVAL);
				announceResponse.setLeechers(leechers);
				announceResponse.setSeeders(seeders);
				announceResponse.setPeers(lPeerInfo);
				sendData(announceResponse, ip, port);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se ha insertado un nuevo peer
	 */
	private void addPeer(final String ip, final int port) {
		try {
			if (Database.getInstance().count("PEERS", "ip = '" + ip + "' AND port = " + Integer.toString(port)) == 0) {

				this.saveProcess = SaveProcess.BLOCK;
				TrackersManager.getInstance().sendQueueMessage(Datagram.READY_TO_SAVE, TrackersManager.getInstance().getIdMaster(), null);
				int time = 0;
				System.out.println("PEERS_MANAGER\tADD PEER - Envia ready to save y se bloquea");
				while (this.saveProcess == SaveProcess.BLOCK && time < READY_TO_SAVE_TIMEOUT) {
					Thread.sleep(1);
					time += 1;
				}
				System.out.println("PEERS_MANAGER\tADD PEER - El tiempo de espera ha sido de: " + time + " segundos y el proceso de guardado es: "
						+ this.saveProcess);

				if (this.saveProcess == SaveProcess.SAVE) {
					Database.getInstance().update("INSERT INTO PEERS (ip, port) VALUES ('" + ip + "', " + Integer.toString(port) + ")");
					int id = Database.getInstance().consult("SELECT id FROM PEERS WHERE ip = '" + ip + "' AND port = " + Integer.toString(port))
							.getInt("id");
					this.peers.put(id, new Peer(id, ip, port));
					setChanged();
					notifyObservers();
				}
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Funcion para notificar que se ha insertado un nuevo contenido
	 */
	private boolean addContent(final String ip, final int port, final String info_hash) {
		try {
			this.saveProcess = SaveProcess.BLOCK;
			TrackersManager.getInstance().sendQueueMessage(Datagram.READY_TO_SAVE, TrackersManager.getInstance().getIdMaster(), null);
			int time = 0;
			System.out.println("PEERS_MANAGER\tADD CONTENT - Envia ready to save y se bloquea");
			while (this.saveProcess == SaveProcess.BLOCK && time < READY_TO_SAVE_TIMEOUT) {
				Thread.sleep(1);
				time += 1;
			}
			System.out.println("PEERS_MANAGER\tADD CONTENT - El tiempo de espera ha sido de: " + time + " segundos y el proceso de guardado es: "
					+ this.saveProcess);

			if (this.saveProcess == SaveProcess.SAVE) {
				Database.getInstance().update("INSERT INTO CONTENTS (hash) VALUES ('" + info_hash + "')");
				return true;
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Funcion para notificar que se ha insertado una nueva relacion
	 */
	private void addRelation(final int idPeer, final int idContent, final String info_hash) {
		try {
			this.saveProcess = SaveProcess.BLOCK;
			TrackersManager.getInstance().sendQueueMessage(Datagram.READY_TO_SAVE, TrackersManager.getInstance().getIdMaster(), null);
			int time = 0;
			System.out.println("PEERS_MANAGER\tADD RELATION - Envia ready to save y se bloquea");
			while (this.saveProcess == SaveProcess.BLOCK && time < READY_TO_SAVE_TIMEOUT) {
				Thread.sleep(1);
				time += 1;
			}
			System.out.println("PEERS_MANAGER\tADD RELATION - El tiempo de espera ha sido de: " + time + " segundos y el proceso de guardado es: "
					+ this.saveProcess);

			if (this.saveProcess == SaveProcess.SAVE) {
				Database.getInstance().update("INSERT INTO PEER_CONTENT (id_peer, id_content, percent) VALUES (" + idPeer + "," + idContent + ", 0)");
				peers.get(idPeer).addContent(new Content(idContent, info_hash, 0));
				setChanged();
				notifyObservers();
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Funcion para actualizar el porcentaje de descarga del contenido
	 */
	private void updateDownloaded(final int idPeer, final int idContent, final String info_hash, final int percent) {
		try {
			this.saveProcess = SaveProcess.BLOCK;
			TrackersManager.getInstance().sendQueueMessage(Datagram.READY_TO_SAVE, TrackersManager.getInstance().getIdMaster(), null);
			int time = 0;
			System.out.println("PEERS_MANAGER\tUPDATE DOWNLOADED - Envia ready to save y se bloquea");
			while (this.saveProcess == SaveProcess.BLOCK && time < READY_TO_SAVE_TIMEOUT) {
				Thread.sleep(1);
				time += 1;
			}
			System.out.println("PEERS_MANAGER\tUPDATE DOWNLOADED - El tiempo de espera ha sido de: " + time
					+ " segundos y el proceso de guardado es: " + this.saveProcess);

			if (this.saveProcess == SaveProcess.SAVE) {
				Database.getInstance()
						.update("UPDATE PEER_CONTENT SET percent = " + percent + " WHERE id_peer = " + idPeer + " AND id_content = " + idContent);

				for (Content content : this.peers.get(idPeer).getContents()) {
					if (content.getInfoHash().equals(info_hash)) {
						content.setPercent(percent);
						break;
					}
				}
				setChanged();
				notifyObservers();
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
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
				if (connected) {
					processData(this.messageIn, messageIn.getAddress(), messageIn.getPort());
				}
			}
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
		}
	}

	public void setSaveProcess(boolean save) {
		this.saveProcess = save ? SaveProcess.SAVE : SaveProcess.NOT_SAVE;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public static PeersManager getInstance() {
		if (instance == null) {
			instance = new PeersManager();
		}
		return instance;
	}
}