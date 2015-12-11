package utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Properties implements Serializable {

	private static final long	serialVersionUID	= -7184695896737258947L;

	private static Properties	properties;

	private String				ip;
	private int					port;

	private Properties(final String ip, final int port) {

		this.ip = ip;
		this.port = port;
	}

	private void update() {
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream("data/config.properties"));
			oos.writeObject(properties);
			oos.close();
		} catch (final IOException e) {
			e.printStackTrace();
			properties = new Properties("", 0);
		}
	}

	private static void init() {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream("data/config.properties"));
			properties = (Properties) ois.readObject();
			ois.close();
		} catch (IOException | ClassNotFoundException e) {
			if (!(e instanceof FileNotFoundException)) {
				e.printStackTrace();
			}
			properties = new Properties("", 0);
			properties.update();
		}
	}

	public static String getIp() {
		if (properties == null) {
			init();
		}
		return properties.ip;
	}

	public static void setIp(final String ip) {
		if (properties == null) {
			init();
		}
		properties.ip = ip;
		properties.update();
	}

	public static int getPort() {
		if (properties == null) {
			init();
		}
		return properties.port;
	}

	public static void setPort(final int port) {
		if (properties == null) {
			init();
		}
		properties.port = port;
		properties.update();
	}
}