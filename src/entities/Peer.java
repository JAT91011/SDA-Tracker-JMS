package entities;

import java.util.Vector;

public class Peer {

	private int				id;
	private String			ip;
	private int				port;
	private Vector<Content>	contents;

	public Peer() {

	}

	public Peer(int id, String ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.contents = new Vector<Content>();
	}

	public Peer(int id, String ip, int port, Vector<Content> contents) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.contents = new Vector<Content>();
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

	public Vector<Content> getContents() {
		return contents;
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

	public void setContents(Vector<Content> contents) {
		this.contents = contents;
	}

	public void addContent(Content content) {
		this.contents.add(content);
	}

	@Override
	public String toString() {
		return "Peer [id=" + id + ", ip=" + ip + ", port=" + port + ", contents=" + contents + "]";
	}
}