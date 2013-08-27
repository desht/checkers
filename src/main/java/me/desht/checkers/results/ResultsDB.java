package me.desht.checkers.results;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.DirectoryStructure;
import me.desht.dhutils.LogUtils;

import org.bukkit.configuration.Configuration;

public class ResultsDB {
	private enum SupportedDrivers {
		MYSQL,
		SQLITE;
	};

	private final Connection connection;

	ResultsDB() throws ClassNotFoundException, SQLException {
		String dbType = CheckersPlugin.getInstance().getConfig().getString("database.driver", "sqlite");
		SupportedDrivers driver = SupportedDrivers.valueOf(dbType.toUpperCase());
		switch (driver) {
		case MYSQL:
			connection = connectMySQL();
			setupTablesMySQL();
			break;
		case SQLITE:
			connection = connectSQLite();
			setupTablesSQLite();
			break;
		default:
			throw new CheckersException("unsupported database type: " + dbType);
		}
		setupTablesCommon();
		LogUtils.fine("Connected to DB: " + connection.getMetaData().getDatabaseProductName());
	}

	void shutdown() {
		try {
			if (!connection.getAutoCommit()) {
				connection.rollback();
			}
			LogUtils.fine("Closing DB connection to " + connection.getMetaData().getDatabaseProductName());
			connection.close();
		} catch (SQLException e) {
			LogUtils.warning("can't cleanly shut down DB connection: " + e.getMessage());
		}
	}

	public Connection getConnection() {
		return connection;
	}

	private Connection connectSQLite() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		File dbFile = new File(DirectoryStructure.getResultsDir(), "gameresults.db");
		return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
	}

	private Connection connectMySQL() throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		Configuration config = CheckersPlugin.getInstance().getConfig();
		String user = config.getString("database.user", "checkers");
		String pass = config.getString("database.password", "");
		String host = config.getString("database.host", "localhost");
		String dbName = config.getString("database.name", "checkers");
		int port = config.getInt("database.port", 3306);
		String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
		return DriverManager.getConnection(url, user, pass);
	}

	private void setupTablesSQLite() throws SQLException {
		createTableIfNotExists("results",
				"gameID INTEGER PRIMARY KEY," +
				"playerWhite VARCHAR(32) NOT NULL," +
				"playerBlack VARCHAR(32) NOT NULL," +
				"gameName VARCHAR(64) NOT NULL," +
				"startTime DATETIME NOT NULL," +
				"endTime DATETIME NOT NULL," +
				"result TEXT NOT NULL," +
				"pdnResult TEXT NOT NULL");
	}

	private void setupTablesMySQL() throws SQLException {
		createTableIfNotExists("results",
				"gameID INTEGER NOT NULL AUTO_INCREMENT," +
				"playerWhite VARCHAR(32) NOT NULL," +
				"playerBlack VARCHAR(32) NOT NULL," +
				"gameName VARCHAR(64) NOT NULL," +
				"startTime DATETIME NOT NULL," +
				"endTime DATETIME NOT NULL," +
				"result TEXT NOT NULL," +
				"pdnResult TEXT NOT NULL," +
				"PRIMARY KEY (gameID)");
	}

	private void setupTablesCommon() throws SQLException {
		createTableIfNotExists("ladder",
				"player VARCHAR(32) NOT NULL," +
				"score INTEGER NOT NULL," +
				"PRIMARY KEY (player)");
		createTableIfNotExists("league",
				"player VARCHAR(32) NOT NULL," +
				"score INTEGER NOT NULL," +
				"PRIMARY KEY (player)");
	}

	private void createTableIfNotExists(String tableName, String ddl) throws SQLException {
		String fullName = CheckersPlugin.getInstance().getConfig().getString("database.table_prefix", "checkers_") + tableName;
		Statement stmt = connection.createStatement();
		try {
			if (tableExists(tableName)) {
				stmt.executeUpdate("ALTER TABLE " + tableName + " RENAME TO " + fullName);
				LogUtils.info("renamed DB table " + tableName + " to " + fullName);
			} else if (!tableExists(fullName)) {
				stmt.executeUpdate("CREATE TABLE " + fullName + "(" + ddl + ")");
			}
		} catch (SQLException e) {
			LogUtils.warning("can't execute " + stmt + ": " + e.getMessage());
			throw e;
		}
	}

	private boolean tableExists(String table) throws SQLException {
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet tables = dbm.getTables(null , null, table, null);
		return tables.next();
	}
}
