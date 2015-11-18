package controllers;

import models.TrackersManager;
import utilities.ErrorsLog;

public class Controller {

	public Controller() {

	}

	public boolean connect() {
		try {
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
		TrackersManager.getInstance().stop();
	}

	public int numberOfTrackers() {
		return TrackersManager.getInstance().getTotalTrackers();
	}
}