package me.desht.checkers.game;

import java.util.*;

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
	// map player ID to player's active game
	private final Map<UUID,CheckersGame> activeGame = new HashMap<UUID, CheckersGame>();

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

		List<UUID> toRemove = new ArrayList<UUID>();
		for (UUID playerName : activeGame.keySet()) {
			if (activeGame.get(playerName) == game) {
				toRemove.add(playerName);
			}
		}
		for (UUID p : toRemove) {
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

	public void setCurrentGame(Player player, CheckersGame game) {
		activeGame.put(player.getUniqueId(), game);
	}

	public CheckersGame getCurrentGame(Player player) {
		return getCurrentGame(player, false);
	}

	public void setCurrentGame(UUID uuid, String gameName) {
		activeGame.put(uuid, getGame(gameName));
	}

	public CheckersGame getCurrentGame(Player player, boolean verify) {
		CheckersGame game = activeGame.get(player.getUniqueId());
		if (verify && game == null) {
			throw new CheckersException(Messages.getString("Game.noActiveGame")); //$NON-NLS-1$
		}
		return game;
	}

	public Map<UUID, String> getCurrentGames() {
		Map<UUID, String> res = new HashMap<UUID, String>();
		for (UUID s : activeGame.keySet()) {
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
	 * @param player the player
	 * @return the unique game name
	 */
	private String makeGameName(Player player) {
		String res;
		int n = 1;
		do {
			res = player.getName() + "-" + n++;
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
	public CheckersGame createGame(Player player, String gameName, String boardName, PlayerColour colour, String ruleId) {
		BoardView bv;
		if (boardName == null) {
			bv = BoardViewManager.getManager().getFreeBoard();
		} else {
			bv = BoardViewManager.getManager().getBoardView(boardName);
		}

		return createGame(player, gameName, bv, colour, ruleId);
	}

	public CheckersGame createGame(Player player, String gameName, BoardView bv, PlayerColour colour, String ruleId) {
		if (gameName == null || gameName.equals("-")) {
			gameName = makeGameName(player);
		}

		CheckersGame game = new CheckersGame(gameName, player, colour, bv.getControlPanel().getTcDefs().currentDef().getSpec(), ruleId);
		bv.setGame(game);
		registerGame(game);
		setCurrentGame(player, game);
		game.setStake(player.getUniqueId().toString(), player.getName(), bv.getDefaultStake());

		MiscUtil.statusMessage(player, Messages.getString("Game.gameCreated", game.getName(), bv.getName()));

		return game;
	}
}
