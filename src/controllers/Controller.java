package controllers;

import models.TrackersManager;
import utilities.ErrorsLog;

public class Controller {

	public Controller() {

	}

	public boolean connect(final String ip, final int port) {
		try {
			TrackersManager.getInstance().connect(ip, port);
			TrackersManager.getInstance().start();
			return true;
		} catch (Exception e) {
			ErrorsLog.getInstance().writeLog(this.getClass().getName(), new Object() {
			}.getClass().getEnclosingMethod().getName(), e.toString());
			e.printStackTrace();
			return false;
		}
	}

	public void disconnect() {
		TrackersManager.getInstance().disconnect();
	}

	public boolean isConnected() {
		return TrackersManager.getInstance().isEnable();
	}

	public int numberOfTrackers() {
		return TrackersManager.getInstance().getTotalTrackers();
	}
}