package utilities;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

	private static Database	instance;
	private Connection		connection;

	private Database() {

	}

	public void createDatabase(final int id) {
		try {
			File file = new File(Properties.getDatabasePath().replace("#", Integer.toString(id)));
			if (file.exists()) {
				file.delete();
			}

			Class.forName("org.sqlite.JDBC");
			this.connection = DriverManager
					.getConnection("jdbc:sqlite:" + Properties.getDatabasePath().replace("#", Integer.toString(id)));
			this.update("PRAGMA encoding = \"UTF-8\";");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (numberOfTables() == 0) {
			create_tables();
		}
	}

	public void disconnect() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void create_tables() {
		try {
			this.update(Utilities.toString("data/createDB.sql"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Statement create_statement() {
		Statement statement = null;
		try {
			statement = this.connection.createStatement();
		} catch (SQLException e) {
			System.out.println("Error creating statement.");
		}
		return statement;
	}

	public ResultSet consult(String consult) {
		Statement statement = this.create_statement();
		try {
			return statement.executeQuery(consult);
		} catch (SQLException e) {
			System.out.println("Error consulting database.");
			return null;
		}
	}

	public int update(String consult) {
		Statement statement = this.create_statement();
		try {
			return statement.executeUpdate(consult);
		} catch (SQLException e) {
			System.out.println("Error updating database.\n " + e.toString());
			return 0;
		}
	}

	public int count(String table, String condition) {
		int number = 0;
		String where = condition == null ? "" : " WHERE " + condition;
		String consult = "SELECT COUNT(*) as number FROM " + table + where + ";";
		ResultSet result = consult(consult);
		try {
			while (result.next()) {
				number = result.getInt("number");
			}
		} catch (SQLException e) {
			System.out.println("Error counting tables.");
		}
		return number;
	}

	private int numberOfTables() {
		return count("sqlite_master", "type='table'");
	}

	public int getFirstIdAvailable(String table, String columnName, String condition) {
		int id = 1;
		boolean enc = false;
		String where = condition == null ? "" : " WHERE " + condition;
		ResultSet rs = Database.getInstance().consult("SELECT " + columnName + " FROM " + table + where + ";");
		try {
			while (rs.next() && !enc) {
				if (id != rs.getInt(columnName)) {
					enc = true;
				} else {
					id++;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	public static Database getInstance() {
		if (instance == null) {
			instance = new Database();
		}
		return instance;
	}

	public static void main(String[] args) {
		Database.getInstance();
	}
}