package me.desht.checkers.results;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.ai.CheckersAI.PendingAction;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameResult;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.dhutils.LogUtils;

import org.bukkit.Bukkit;

public class Results {
	private static Results results = null;	// this is a singleton class

	private final ResultsDB db;
	private final List<ResultEntry> entries = new ArrayList<ResultEntry>();
	private final Map<String, ResultViewBase> views = new HashMap<String, ResultViewBase>();

	private boolean databaseLoaded = false;

	private final BlockingQueue<DatabaseSavable> pendingUpdates = new LinkedBlockingQueue<DatabaseSavable>();
	private Thread updater;

	/**
	 * Create the singleton results handler - only called from getResultsHandler once
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	private Results() throws ClassNotFoundException, SQLException {
		db = new ResultsDB();
		registerView("ladder", new Ladder(this));
		registerView("league", new League(this));
		loadEntriesFromDatabase();
		updater = new Thread(new DatabaseUpdaterTask(this));
		updater.start();
	}

	/**
	 * Register a new view type
	 *
	 * @param viewName	Name of the view
	 * @param view		Object to handle the view (must subclass ResultViewBase)
	 */
	private void registerView(String viewName, ResultViewBase view) {
		views.put(viewName, view);
	}

	/**
	 * Get the singleton results handler object
	 *
	 * @return	The results handler
	 */
	public synchronized static Results getResultsHandler() {
		if (results == null) {
			try {
				results = new Results();
			} catch (Exception e) {
				LogUtils.warning(e.getMessage());
			}
		}
		return results;
	}

	/**
	 * Check that the results handler has been initialised sucessfully.
	 *
	 * @return	true if the results handler is OK, false otherwise
	 */
	public synchronized static boolean resultsHandlerOK() {
		return results != null;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Get the database handler for the results
	 *
	 * @return	The database handler
	 */
	ResultsDB getResultsDB() {
		return db;
	}

	/**
	 * Shut down the results handler, ensuring the DB is cleanly disconnected etc.
	 * Call this when the plugin is disabled.
	 */
	public static synchronized void shutdown() {
		if (results != null) {
			if (results.db != null) {
				results.db.shutdown();
			}
			results = null;
		}
		getResultsHandler().pendingUpdates.add(null);
	}

	/**
	 * Get a results view of the given type (e.g. ladder, league...)
	 *
	 * @param viewName	Name of the view type
	 * @return			A view object
	 * @throws CheckersException	if there is no such view type
	 */
	public ResultViewBase getView(String viewName) throws CheckersException {
		if (!views.containsKey(viewName)) {
			throw new CheckersException("No such results type: " + viewName);
		}
		return views.get(viewName);
	}

	/**
	 * Return a list of all results
	 *
	 * @return	A list of ResultEntry objects
	 */
	public List<ResultEntry> getEntries() {
		return entries;
	}

	/**
	 * Get the database connection object
	 *
	 * @return	A SQL Connection object
	 */
	public Connection getConnection() {
		return db.getConnection();
	}

	/**
	 * @return the databaseLoaded
	 */
	public boolean isDatabaseLoaded() {
		return databaseLoaded;
	}

	/**
	 * Log the result for a game
	 *
	 * @param game	The game that has just finished
	 * @param rt	The outcome of the game
	 */
	public void logResult(CheckersGame game) {
		if (!databaseLoaded) {
			return;
		}
		if (game.getState() != GameState.FINISHED) {
			return;
		}
		if (game.getResult() == GameResult.ABANDONED) {
			// Abandoned games don't really have a result - we can't count it as a draw
			// since that would hurt higher-ranked players on the ladder.
			return;
		}

		final ResultEntry re = new ResultEntry(game);
		entries.add(re);
		for (ResultViewBase view : views.values()) {
			view.addResult(re);
		}

		queueDatabaseUpdate(re);

	}

	/**
	 * Asynchronously load in the result data from database.  Called at startup; results data
	 * will not be available until this has finished.
	 */
	private void loadEntriesFromDatabase() {
		Bukkit.getScheduler().runTaskAsynchronously(CheckersPlugin.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					entries.clear();
					Statement stmt = getConnection().createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM " + getTableName("results"));
					while (rs.next()) {
						ResultEntry e = new ResultEntry(rs);
						entries.add(e);
					}
					rebuildViews();
					LogUtils.fine("Results data loaded from database");
					databaseLoaded = true;
				} catch (SQLException e) {
					LogUtils.warning("SQL query failed: " + e.getMessage());
				}
			}
		});
	}

	/**
	 * Generate some random test data and put it in the results table.  This is just
	 * for testing purposes.
	 */
	public void addTestData() {
		final int N_PLAYERS = 10;
		String[] pdnResults = { "1-0", "0-1", "1/2-1/2" };

		try {
			getConnection().setAutoCommit(false);
			Statement clear = getConnection().createStatement();
			clear.executeUpdate("DELETE FROM " + getTableName("results") + " WHERE playerWhite LIKE 'testplayer%' OR playerBlack LIKE 'testplayer%'");
			Random rnd = new Random();
			for (int i = 0; i < N_PLAYERS; i++) {
				for (int j = 0; j < N_PLAYERS; j++) {
					if (i == j) {
						continue;
					}
					String plw = "testplayer" + i;
					String plb = "testplayer" + j;
					String gn = "testgame-" + i + "-" + j;
					long start = System.currentTimeMillis() - 5000;
					long end = System.currentTimeMillis() - 4000;
					String pdnRes = pdnResults[rnd.nextInt(pdnResults.length)];
					GameResult rt;
					if (pdnRes.equals("1-0") || pdnRes.equals("0-1")) {
						rt = GameResult.WIN;
					} else {
						rt = GameResult.DRAW_AGREED;
					}
					ResultEntry re = new ResultEntry(plw, plb, gn, start, end, pdnRes, rt);
					entries.add(re);
					re.saveToDatabase(getConnection());
				}
			}
			getConnection().setAutoCommit(true);
			rebuildViews();
			LogUtils.info("test data added & committed");
		} catch (SQLException e) {
			LogUtils.warning("can't put test data into DB: " + e.getMessage());
		}
	}

	public void rebuildViews() {
		for (ResultViewBase view : views.values()) {
			view.rebuild();
		}
	}

	void queueDatabaseUpdate(DatabaseSavable update) {
		pendingUpdates.add(update);
	}

	public DatabaseSavable pollDatabaseUpdate() throws InterruptedException {
		return pendingUpdates.take();
	}

	public String getTableName(String base) {
		return CheckersPlugin.getInstance().getConfig().getString("database.table_prefix", "checkers_") + base;
	}
}
