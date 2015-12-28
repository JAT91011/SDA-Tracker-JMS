package entities;

import java.io.Serializable;

public class Datagram implements Serializable {

	private static final long	serialVersionUID	= -5877535714915804320L;

	public static int			DB_REPLICATION		= 1;
	public static int			READY_TO_SAVE		= 2;
	public static int			SAVE_DATA			= 3;
	public static int			DO_NOT_SAVE_DATA	= 4;
	public static int			KEEP_ALIVE			= 5;
	public static int			ERROR				= 9;

	public static int			SAVE_PEER			= 1;
	public static int			SAVE_CONTENT		= 2;
	public static int			ADD_RELATION		= 3;
	public static int			UPDATE_DOWNLOADED	= 4;

	private int					id;
	private int					idTrackerFrom;
	private int					idTrackerTo;
	private byte[]				content;
	private boolean				isMaster;
	private int					saveCode;
	private String[]			saveData;

	public Datagram() {

	}

	public Datagram(final int id, final int idTrackerFrom) {
		this.id = id;
		this.idTrackerFrom = idTrackerFrom;
		this.idTrackerTo = -1;
		this.content = null;
		this.isMaster = false;
	}

	public Datagram(final int id, final int idTrackerFrom, final boolean isMaster) {
		this.id = id;
		this.idTrackerFrom = idTrackerFrom;
		this.idTrackerTo = -1;
		this.content = null;
		this.isMaster = isMaster;
	}

	public Datagram(final int id, final int idTrackerFrom, final int idTrackerTo) {
		this.id = id;
		this.idTrackerFrom = idTrackerFrom;
		this.idTrackerTo = idTrackerTo;
		this.content = null;
		this.isMaster = false;
	}

	public Datagram(final int id, final int idTrackerFrom, final int idTrackerTo, final byte[] content) {
		this.id = id;
		this.idTrackerFrom = idTrackerFrom;
		this.idTrackerTo = idTrackerTo;
		this.content = content;
		this.isMaster = false;
	}

	public Datagram(final int id, final int idTrackerFrom, final int idTrackerTo, final int saveCode, final String[] saveData) {
		this.id = id;
		this.idTrackerFrom = idTrackerFrom;
		this.idTrackerTo = idTrackerTo;
		this.saveCode = saveCode;
		this.saveData = saveData;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getIdTrackerFrom() {
		return idTrackerFrom;
	}

	public void setIdTrackerFrom(int idTrackerFrom) {
		this.idTrackerFrom = idTrackerFrom;
	}

	public int getIdTrackerTo() {
		return idTrackerTo;
	}

	public void setIdTrackerTo(int idTrackerTo) {
		this.idTrackerTo = idTrackerTo;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public boolean isMaster() {
		return isMaster;
	}

	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	public int getSaveCode() {
		return saveCode;
	}

	public void setSaveCode(int saveCode) {
		this.saveCode = saveCode;
	}

	public String[] getSaveData() {
		return saveData;
	}

	public void setSaveData(String[] saveData) {
		this.saveData = saveData;
	}
}