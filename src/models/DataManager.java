package models;

/**
 * Encapsula la funcionalidad relacionada con el almacenamiento de información
 * persistente.
 */

public class DataManager {

	private static DataManager instance;

	private DataManager() {

	}

	public static DataManager getInstance() {
		if (instance == null) {
			instance = new DataManager();
		}
		return instance;
	}
}