package me.desht.checkers.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.event.CheckersGameCreatedEvent;
import me.desht.checkers.event.CheckersGameDeletedEvent;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CheckersGameManager {
	private static CheckersGameManager instance = null;

	// map game name to game
	private final Map<String,CheckersGame> checkersGames = new HashMap<String,CheckersGame>();
	// map player name to player's active game
	private final Map<String,CheckersGame> activeGame = new HashMap<String, CheckersGame>();

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
		if (!checkersGames.containsKey(gameName)) {
			checkersGames.put(gameName, game);
			game.save();
			Bukkit.getPluginManager().callEvent(new CheckersGameCreatedEvent(game));
		} else {
			throw new CheckersException("trying to register duplicate game " + gameName);
		}
	}

	public void unregisterGame(String gameName) {
		CheckersGame game = getGame(gameName);

		List<String> toRemove = new ArrayList<String>();
		for (String playerName : activeGame.keySet()) {
			if (activeGame.get(playerName) == game) {
				toRemove.add(playerName);
			}
		}
		for (String p : toRemove) {
			activeGame.remove(p);
		}
		checkersGames.remove(gameName);
		Bukkit.getPluginManager().callEvent(new CheckersGameDeletedEvent(game));
	}

	public boolean checkGame(String gameName) {
		return checkersGames.containsKey(gameName);
	}

	public Collection<CheckersGame> listGamesSorted() {
		SortedSet<String> sorted = new TreeSet<String>(checkersGames.keySet());
		List<CheckersGame> res = new ArrayList<CheckersGame>();
		for (String name : sorted) {
			res.add(checkersGames.get(name));
		}
		return res;
	}

	public Collection<CheckersGame> listGames() {
		return checkersGames.values();
	}

	public CheckersGame getGame(String name) {
		return getGame(name, true);
	}

	public CheckersGame getGame(String name, boolean fuzzy) {
		if (!checkersGames.containsKey(name)) {
			throw new CheckersException(Messages.getString("Game.noSuchGame", name));
		}
		return checkersGames.get(name);
	}

	public void setCurrentGame(String playerName, String gameName) {
		CheckersGame game = getGame(gameName);
		setCurrentGame(playerName, game);
	}

	public void setCurrentGame(String playerName, CheckersGame game) {
		activeGame.put(playerName, game);
	}

	public CheckersGame getCurrentGame(String playerName) {
		return getCurrentGame(playerName, false);
	}

	public CheckersGame getCurrentGame(String playerName, boolean verify) {
		CheckersGame game = activeGame.get(playerName);
		if (verify && game == null) {
			throw new CheckersException(Messages.getString("Game.noActiveGame")); //$NON-NLS-1$
		}
		return game;
	}

	public Map<String, String> getCurrentGames() {
		Map<String, String> res = new HashMap<String, String>();
		for (String s : activeGame.keySet()) {
			CheckersGame game = activeGame.get(s);
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
	 * Convenience method to create a new checkers game.
	 *
	 * @param player		The player who is creating the game
	 * @param gameName		Name of the game - may be null, in which case a name will be generated
	 * @param boardName		Name of the board for the game - may be null, in which case a free board will be picked
	 * @return	The game object
	 * @throws CheckersException	if there is any problem creating the game
	 */
	public CheckersGame createGame(Player player, String gameName, String boardName, PlayerColour colour) {
		BoardView bv;
		if (boardName == null) {
			bv = BoardViewManager.getManager().getFreeBoard();
		} else {
			bv = BoardViewManager.getManager().getBoardView(boardName);
		}

		return createGame(player, gameName, bv, colour);
	}

	public CheckersGame createGame(Player player, String gameName, BoardView bv, PlayerColour colour) {
		String playerName = player.getName();

		if (gameName == null || gameName.equals("-")) {
			gameName = makeGameName(playerName);
		}

		CheckersGame game = new CheckersGame(gameName, playerName, colour, bv.getControlPanel().getTcDefs().currentDef().getSpec());
		bv.setGame(game);
		registerGame(game);
		setCurrentGame(playerName, game);
		game.setStake(playerName, bv.getDefaultStake());

		MiscUtil.statusMessage(player, Messages.getString("Game.gameCreated", game.getName(), bv.getName()));

		return game;
	}
}
