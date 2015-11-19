package models;

import java.util.Observable;

public class PeersManager extends Observable implements Runnable {

	private static PeersManager instance;

	@Override
	public void run() {

	}

	public static PeersManager getInstance() {
		if (instance == null) {
			instance = new PeersManager();
		}
		return instance;
	}
}