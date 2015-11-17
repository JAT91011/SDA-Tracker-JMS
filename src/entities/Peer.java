package entities;

import java.util.Vector;

public class Peer {

	private int				id;
	private String			ip;
	private int				port;
	private Vector<String>	hashmapContents;

	public Peer() {

	}

	public Peer(int id, String ip, int port, Vector<String> hashmapContents) {
		super();
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.hashmapContents = hashmapContents;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public Vector<String> getHashmapContents() {
		return hashmapContents;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setHashmapContents(Vector<String> hashmapContents) {
		this.hashmapContents = hashmapContents;
	}
}