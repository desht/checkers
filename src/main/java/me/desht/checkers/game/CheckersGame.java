package me.desht.checkers.game;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPersistable;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.DirectoryStructure;
import me.desht.checkers.Messages;
import me.desht.checkers.ai.CheckersAI;
import me.desht.checkers.model.Move;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.Position;
import me.desht.checkers.model.SimplePosition;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.checkers.player.HumanCheckersPlayer;
import me.desht.checkers.util.CheckersUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;

public class CheckersGame implements CheckersPersistable {
	public enum GameResult {
		WHITE_WINS, BLACK_WINS, DRAW_AGREED, ABANDONED, NOT_FINISHED,
	}
	public enum GameState {
		SETTING_UP, RUNNING, FINISHED,
	}

	public static final Object OPEN_INVITATION = "*";

	private final List<GameListener> listeners = new ArrayList<GameListener>();

	private final String gameName;
	private final Position position;
	private final CheckersPlayer[] players = new CheckersPlayer[2];
	private final long created;

	private GameState state;
	private String invited;
	private double stake;
	private long started, finished, lastMoved;
	private GameResult result;

	public CheckersGame(String gameName, String creatorName, PlayerColour colour) {
		this.gameName = gameName;
		this.position = new SimplePosition();
		this.state = GameState.SETTING_UP;
		this.invited = "";
		this.stake = 0.0;
		this.created = System.currentTimeMillis();
		this.result = GameResult.NOT_FINISHED;
		if (creatorName != null) {
			createPlayer(colour, creatorName);
		}
	}

	public CheckersGame(Configuration conf) {
		this.gameName = conf.getString("name");
		this.position = new SimplePosition();
		this.state = GameState.valueOf(conf.getString("state"));
		this.invited = conf.getString("invited");
		this.created = conf.getLong("created");
		this.started = conf.getLong("started");
		this.finished = conf.getLong("finished", state == GameState.FINISHED ? System.currentTimeMillis() : 0);
		this.lastMoved = conf.getLong("lastMoved");
		this.result = GameResult.valueOf(conf.getString("result"));
		this.stake = conf.getDouble("stake");

		createPlayer(PlayerColour.WHITE, conf.getString("playerWhite"));
		createPlayer(PlayerColour.BLACK, conf.getString("playerBlack"));

		// replay the saved move history
		List<Integer> encoded = conf.getIntegerList("moves");
		for (int m : encoded) {
			Move move = new Move(m);
			position.makeMove(move);
		}
	}

	private void createPlayer(PlayerColour colour, String playerName) {
		int idx = colour.getIndex();
		if (playerName == null) {
			// TODO: new random free AI
		} else if (CheckersAI.isAI(playerName)) {
			// TODO: new named AI
		} else if (playerName.isEmpty()) {
			// no player for this slot yet
			players[idx] = null;
		} else {
			players[idx] = new HumanCheckersPlayer(playerName, this, colour);
		}
	}

	public void save() {
		CheckersPlugin.getInstance().getPersistenceHandler().savePersistable("game", this);
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();

		Move[] moves = getPosition().getMoveHistory();
		List<Integer> encoded = new ArrayList<Integer>(moves.length);
		for (Move move : moves) {
			encoded.add(move.encode());
		}
		map.put("name", getName());
		map.put("playerWhite", getPlayerName(PlayerColour.WHITE));
		map.put("playerBlack", getPlayerName(PlayerColour.BLACK));
		map.put("state", state.toString());
		map.put("invited", getInvited());
		map.put("moves", encoded);
		map.put("created", created);
		map.put("started", started);
		map.put("finished", finished);
		map.put("lastMoved", lastMoved);
		map.put("result", result.toString());
		map.put("stake", stake);

		return map;
	}

	public static CheckersGame deserialize(Map<String, Object> map) {
		Configuration conf = new MemoryConfiguration();
		for (Entry<String, Object> e : map.entrySet()) {
			conf.set(e.getKey(), e.getValue());
		}
		return new CheckersGame(conf);
	}

	@Override
	public File getSaveDirectory() {
		return DirectoryStructure.getGamesPersistDirectory();
	}

	public String getName() {
		return gameName;
	}

	public Position getPosition() {
		return position;
	}

	public boolean hasPlayer(PlayerColour colour) {
		return players[colour.getIndex()] != null;
	}

	public CheckersPlayer getPlayer(PlayerColour colour) {
		return players[colour.getIndex()];
	}

	public String getPlayerName(PlayerColour colour) {
		return players[colour.getIndex()] != null ? players[colour.getIndex()].getName() : "";
	}

	public double getStake() {
		return stake;
	}

	/**
	 * @return the state
	 */
	public GameState getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(GameState state) {
		this.state = state;
	}

	/**
	 * Called when a game is permanently deleted.
	 */
	public void deletePermanently() {
		CheckersPlugin.getInstance().getPersistenceHandler().unpersist(this);

		handlePayout();

		deleteCommon();

		for (GameListener l : listeners) {
			l.gameDeleted(this);
		}
	}

	public void deleteTemporary() {
		deleteCommon();
	}

	private void deleteCommon() {
		if (hasPlayer(PlayerColour.WHITE)) getPlayer(PlayerColour.WHITE).cleanup();
		if (hasPlayer(PlayerColour.BLACK)) getPlayer(PlayerColour.BLACK).cleanup();
		try {
			CheckersGameManager.getManager().unregisterGame(getName());
		} catch (CheckersException e) {
			LogUtils.warning(e.getMessage());
		}
	}

	public String getInvited() {
		return invited;
	}

	public void alert(String message) {
		for (CheckersPlayer cp : players) {
			if (cp != null) {
				cp.alert(message);
			}
		}
	}

	public void alert(String playerName, String message) {
		for (CheckersPlayer cp : players) {
			if (cp != null && cp.getName().equals(playerName)) {
				cp.alert(message);
			}
		}
	}

	public void drawn(GameResult drawAgreed) {
		// TODO Auto-generated method stub

	}

	public void swapColours() {
		// TODO Auto-generated method stub

	}

	/**
	 * Return detailed information about the game.
	 * 
	 * @return a string list of game information
	 */
	public List<String> getGameDetail() {
		List<String> res = new ArrayList<String>();

		String white = hasPlayer(PlayerColour.WHITE) ? getPlayer(PlayerColour.WHITE).getDisplayName() : "?";
		String black = hasPlayer(PlayerColour.BLACK) ? getPlayer(PlayerColour.BLACK).getDisplayName() : "?";
		String bullet = MessagePager.BULLET + ChatColor.YELLOW;

		res.add(Messages.getString("Game.gameDetail.name", getName(), getState()));
		res.add(bullet + Messages.getString("Game.gameDetail.players", white, black));
		res.add(bullet +  Messages.getString("Game.gameDetail.halfMoves", getPosition().getMoveHistory().length));
		if (CheckersPlugin.getInstance().getEconomy() != null) {
			res.add(bullet + Messages.getString("Game.gameDetail.stake", CheckersUtils.formatStakeStr(getStake())));
		}
		res.add(bullet + (getPosition().getToMove() == PlayerColour.WHITE ? 
				Messages.getString("Game.gameDetail.whiteToPlay") : 
					Messages.getString("Game.gameDetail.blackToPlay")));
		if (getInvited().equals(OPEN_INVITATION)) {
			res.add(bullet + Messages.getString("Game.gameDetail.openInvitation"));
		} else if (!getInvited().isEmpty()) {
			res.add(bullet + Messages.getString("Game.gameDetail.invitation", getInvited()));
		}
		res.add(Messages.getString("Game.gameDetail.moveHistory"));
		Move[] moves = getPosition().getMoveHistory();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < moves.length; i += 2) {
			sb.append(ChatColor.WHITE + Integer.toString((i / 2) + 1) + ". ");
			sb.append(ChatColor.YELLOW + moves[i].toString());
			if (i < moves.length - 1) {
				sb.append(" ").append(moves[i + 1].toString());
			}
			sb.append(" ");
		}
		res.add(sb.toString());

		return res;
	}

	public void addGameListener(GameListener listener) {
		listeners.add(listener);
	}

	private void handlePayout() {
		// TODO Auto-generated method stub
	}

	public boolean playerCanDelete(CommandSender sender) {
		// TODO Auto-generated method stub
		return false;
	}
}
