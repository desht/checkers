package me.desht.checkers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.checkers.event.CheckersGameCreatedEvent;
import me.desht.checkers.event.CheckersGameDeletedEvent;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CheckersGameManager {
	private static CheckersGameManager instance = null;

	private final Map<String,CheckersGame> chessGames = new HashMap<String,CheckersGame>();
	private final Map<String,CheckersGame> currentGame = new HashMap<String, CheckersGame>();

	private CheckersGameManager() {

	}

	public static synchronized CheckersGameManager getManager() {
		if (instance == null) {
			instance = new CheckersGameManager();
		}
		return instance;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public void registerGame(CheckersGame game) {
		String gameName = game.getName();
		if (!chessGames.containsKey(gameName)) {
			chessGames.put(gameName, game);
			Bukkit.getPluginManager().callEvent(new CheckersGameCreatedEvent(game));
		} else {
			throw new CheckersException("trying to register duplicate game " + gameName);
		}
	}

	public void unregisterGame(String gameName) {
		CheckersGame game = getGame(gameName);

		List<String> toRemove = new ArrayList<String>();
		for (String playerName : currentGame.keySet()) {
			if (currentGame.get(playerName) == game) {
				toRemove.add(playerName);
			}
		}
		for (String p : toRemove) {
			currentGame.remove(p);
		}
		chessGames.remove(gameName);
		Bukkit.getPluginManager().callEvent(new CheckersGameDeletedEvent(game));
	}

	public boolean checkGame(String gameName) {
		return chessGames.containsKey(gameName);
	}

	public Collection<CheckersGame> listGamesSorted() {
		SortedSet<String> sorted = new TreeSet<String>(chessGames.keySet());
		List<CheckersGame> res = new ArrayList<CheckersGame>();
		for (String name : sorted) {
			res.add(chessGames.get(name));
		}
		return res;
	}

	public Collection<CheckersGame> listGames() {
		return chessGames.values();
	}

	public CheckersGame getGame(String name) {
		return getGame(name, true);
	}
	
	public CheckersGame getGame(String name, boolean fuzzy) {
		if (!chessGames.containsKey(name)) {
			throw new CheckersException(Messages.getString("Game.noSuchGame", name));
		}
		return chessGames.get(name);
	}

	public void setCurrentGame(String playerName, String gameName) {
		CheckersGame game = getGame(gameName);
		setCurrentGame(playerName, game);
	}

	public void setCurrentGame(String playerName, CheckersGame game) {
		currentGame.put(playerName, game);
	}

	public CheckersGame getCurrentGame(String playerName) {
		return getCurrentGame(playerName, false);
	}

	public CheckersGame getCurrentGame(String playerName, boolean verify) {
		CheckersGame game = currentGame.get(playerName);
		if (verify && game == null) {
			throw new CheckersException(Messages.getString("Game.noActiveGame")); //$NON-NLS-1$
		}
		return game;
	}

	public Map<String, String> getCurrentGames() {
		Map<String, String> res = new HashMap<String, String>();
		for (String s : currentGame.keySet()) {
			CheckersGame game = currentGame.get(s);
			if (game != null) {
				res.put(s, game.getName());
			}
		}
		return res;
	}

	/**
	 * Create a unique game name based on the player's name.
	 * 
	 * @param playerName
	 * @return
	 */
	private String makeGameName(String playerName) {
		String res;
		int n = 1;
		do {
			res = playerName + "-" + n++; //$NON-NLS-1$
		} while (checkGame(res));

		return res;
	}

	/**
	 * Convenience method to create a new chess game.
	 * 
	 * @param player		The player who is creating the game
	 * @param gameName		Name of the game - may be null, in which case a name will be generated
	 * @param boardName		Name of the board for the game - may be null, in which case a free board will be picked
	 * @return	The game object
	 * @throws CheckersException	if there is any problem creating the game
	 */
	public CheckersGame createGame(Player player, String gameName, String boardName, int colour) {
		BoardView bv;
		if (boardName == null) {
			bv = BoardViewManager.getManager().getFreeBoard();
		} else {
			bv = BoardViewManager.getManager().getBoardView(boardName);
		}

		return createGame(player, gameName, bv, colour);
	}

	public CheckersGame createGame(Player player, String gameName, BoardView bv, int colour) {
		String playerName = player.getName();

		if (gameName == null || gameName.equals("-")) {
			gameName = makeGameName(playerName);
		}

		CheckersGame game = new CheckersGame(gameName, bv, playerName, colour);
		registerGame(game);
		setCurrentGame(playerName, game);
		bv.getControlPanel().repaintControls();

		game.save();

		MiscUtil.statusMessage(player, Messages.getString("CheckersCommandExecutor.gameCreated", game.getName(), game.getView().getName()));

		return game;
	}
}
