package entities;

public class Content {

	private int		id;
	private String	infoHash;
	private int		percent;

	public Content() {

	}

	public Content(int id, String infoHash, int percent) {
		super();
		this.id = id;
		this.infoHash = infoHash;
		this.percent = percent;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getInfoHash() {
		return infoHash;
	}

	public void setInfoHash(String infoHash) {
		this.infoHash = infoHash;
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}

	@Override
	public String toString() {
		return "Content [id=" + id + ", infoHash=" + infoHash + ", percent=" + percent + "]";
	}
}