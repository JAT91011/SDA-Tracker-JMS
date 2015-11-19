package controllers;

import models.TrackersManager;
import utilities.ErrorsLog;

public class Controller {

	private static Controller instance;

	private Controller() {

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

	public static Controller getInstance() {
		if (instance == null) {
			instance = new Controller();
		}
		return instance;
	}
}