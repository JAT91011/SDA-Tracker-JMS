package entities;

import java.util.Date;

public class Tracker {

	private int		id;
	private boolean	master;
	private Date	lastKeepAlive;
	private Date	firstConnection;

	public Tracker() {

	}

	public Tracker(int id, boolean master) {
		this.id = id;
		this.master = master;
		this.lastKeepAlive = null;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}

	public Date getLastKeepAlive() {
		return lastKeepAlive;
	}

	public void setLastKeepAlive(Date lastKeepAlive) {
		this.lastKeepAlive = lastKeepAlive;
	}

	public long getDifferenceBetweenKeepAlive() {
		if (this.lastKeepAlive != null) {
			return (new Date().getTime() - this.lastKeepAlive.getTime()) / 1000;
		} else {
			return 0;
		}
	}

	public Date getFirstConnection() {
		return firstConnection;
	}

	public void setFirstConnection(Date firstConnection) {
		this.firstConnection = firstConnection;
	}
}