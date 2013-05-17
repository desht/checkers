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
import me.desht.checkers.CheckersValidate;
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
import me.desht.dhutils.Duration;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

public class CheckersGame implements CheckersPersistable {
	public enum GameResult {
		WHITE_WINS, BLACK_WINS, DRAW_AGREED, ABANDONED, NOT_FINISHED,
	}
	public enum GameState {
		SETTING_UP, RUNNING, FINISHED,
	}

	public static final String OPEN_INVITATION = "*";

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
	private long lastOpenInvite;

	public CheckersGame(String gameName, String creatorName, PlayerColour colour) {
		this.gameName = gameName;
		this.position = new SimplePosition();
		this.state = GameState.SETTING_UP;
		this.invited = "";
		this.stake = 0.0;
		this.created = System.currentTimeMillis();
		this.result = GameResult.NOT_FINISHED;
		if (creatorName != null) {
			players[colour.getIndex()] = createPlayer(colour, creatorName);
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

		players[PlayerColour.WHITE.getIndex()] = createPlayer(PlayerColour.WHITE, conf.getString("playerWhite"));
		players[PlayerColour.BLACK.getIndex()] = createPlayer(PlayerColour.BLACK, conf.getString("playerBlack"));

		// replay the saved move history
		List<Integer> encoded = conf.getIntegerList("moves");
		for (int m : encoded) {
			Move move = new Move(m);
			position.makeMove(move);
		}
	}

	private CheckersPlayer createPlayer(PlayerColour colour, String playerName) {
		if (playerName == null) {
			// TODO: new random free AI
			return null;
		} else if (CheckersAI.isAI(playerName)) {
			// TODO: new named AI
			return null;
		} else if (playerName.isEmpty()) {
			// no player for this slot yet
			return null;
		} else {
			return new HumanCheckersPlayer(playerName, this, colour);
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

	public CheckersPlayer getPlayer(String playerName) {
		if (playerName.equalsIgnoreCase(getPlayerName(PlayerColour.WHITE))) {
			return getPlayer(PlayerColour.WHITE);
		} else if (playerName.equalsIgnoreCase(getPlayerName(PlayerColour.BLACK))) {
			return getPlayer(PlayerColour.BLACK);
		} else {
			return null;
		}
	}

	public String getPlayerName(PlayerColour colour) {
		return players[colour.getIndex()] != null ? players[colour.getIndex()].getName() : "";
	}

	public boolean isPlayerInGame(String playerName) {
		return getPlayerName(PlayerColour.WHITE).equalsIgnoreCase(playerName) || getPlayerName(PlayerColour.BLACK).equalsIgnoreCase(playerName);
	}

	public boolean isFull() {
		return getPlayer(PlayerColour.WHITE) != null && getPlayer(PlayerColour.BLACK) != null;
	}

	public double getStake() {
		if (CheckersPlugin.getInstance().getEconomy() == null) {
			return 0.0;
		} else {
			return stake;
		}
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

	public void alert(Player player, String message) {
		MiscUtil.alertMessage(player, Messages.getString("Game.alertPrefix", getName()) + message);
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
			System.out.println("invited [" + getInvited() + "]");
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

	public boolean playerAllowedToDelete(String playerName) {
		return getState() == GameState.SETTING_UP && isPlayerInGame(playerName);
	}

	public void invitePlayer(String inviterName, String inviteeName) {
		inviteSanityCheck(inviterName);

		if (inviteeName == null) {
			inviteOpen(inviterName);
			return;
		}

		Player player = Bukkit.getServer().getPlayer(inviteeName);
		if (player != null) {
			inviteeName = player.getName();
			alert(player, Messages.getString("Game.youAreInvited", inviterName));
			if (getStake() > 0.0) {
				alert(player, Messages.getString("Game.gameHasStake", CheckersUtils.formatStakeStr(getStake())));
			}
			alert(player, Messages.getString("Game.joinPrompt"));
			if (!invited.isEmpty() && !invited.equals(inviteeName)) {
				alert(invited, Messages.getString("Game.inviteWithdrawn"));
			}
			invited = inviteeName;
			alert(inviterName, Messages.getString("Game.inviteSent", invited));
		} else {
			// TODO: add an AI player
			throw new CheckersException("Unknown player: " + inviteeName);
		}
		save();
	}

	public void inviteOpen(String inviterName) {
		long now = System.currentTimeMillis();
		Duration cooldown = new Duration(CheckersPlugin.getInstance().getConfig().getString("open_invite_cooldown", "3 mins"));
		long remaining = (cooldown.getTotalDuration() - (now - lastOpenInvite)) / 1000;
		CheckersValidate.isTrue(remaining > 0, Messages.getString("Game.inviteCooldown", remaining));

		MiscUtil.broadcastMessage((Messages.getString("Game.openInviteCreated", inviterName)));
		if (getStake() > 0.0) {
			MiscUtil.broadcastMessage(Messages.getString("Game.gameHasStake", CheckersUtils.formatStakeStr(getStake())));
		}
		MiscUtil.broadcastMessage(Messages.getString("Game.joinPromptGlobal", getName()));
		invited = OPEN_INVITATION;
		lastOpenInvite = now;
		save();
	}

	public void clearInvitation() {
		invited = "";
	}

	public void addPlayer(String playerName) {
		ensureGameInState(GameState.SETTING_UP);

		if (isFull()) {
			// this could happen if autostart is disabled and two players have already joined
			throw new CheckersException(Messages.getString("Game.gameIsFull"));
		}

		CheckersPlayer p = fillEmptyPlayerSlot(playerName);

		//		getView().getControlPanel().repaintControls();
		clearInvitation();

		for (GameListener l : listeners) {
			l.playerAdded(this, p);
		}

		if (isFull()) {
			if (CheckersPlugin.getInstance().getConfig().getBoolean("autostart", true)) {
				start(playerName);
			} else {
				alert(Messages.getString("Game.startPrompt"));
			}
		}
	}

	public void start(String playerName) {
		ensurePlayerInGame(playerName);
		ensureGameInState(GameState.SETTING_UP);
		if (!isFull()) {
			// TODO game started with only one player - add an AI player
			//			fillEmptyPlayerSlot(null);
			throw new CheckersException("game needs two players!");
		}

		if (stake > 0.0 && !getPlayerName(PlayerColour.WHITE).equalsIgnoreCase(getPlayerName(PlayerColour.BLACK))) {
			// just in case stake.max got adjusted after game creation...
			double max = CheckersPlugin.getInstance().getConfig().getDouble("stake.max");
			if (max >= 0 && stake > max) {
				stake = max;
			}
			getPlayer(PlayerColour.WHITE).validateAffordability("Game.cantAffordToStart");
			getPlayer(PlayerColour.WHITE).withdrawFunds(stake);
			getPlayer(PlayerColour.BLACK).validateAffordability("Game.cantAffordToStart");
			getPlayer(PlayerColour.BLACK).withdrawFunds(stake);
		}

		clearInvitation();
		started = lastMoved = System.currentTimeMillis();
		state = GameState.RUNNING;

		getPlayer(PlayerColour.WHITE).promptForFirstMove();

		save();

		for (GameListener l : listeners) {
			l.gameStarted(this);
		}
	}

	private void inviteSanityCheck(String inviterName) {
		ensurePlayerInGame(inviterName);
		ensureGameInState(GameState.SETTING_UP);
	}

	private void ensureGameInState(GameState state) {
		CheckersValidate.isTrue(getState() == state, Messages.getString("Game.shouldBeState", state));
	}

	private void ensurePlayerInGame(String playerName) {
		CheckersValidate.isTrue(getPlayerName(PlayerColour.WHITE).equals(playerName) || getPlayerName(PlayerColour.BLACK).equals(playerName),
		                        Messages.getString("Game.notInGame"));
	}

	private CheckersPlayer fillEmptyPlayerSlot(String playerName) {
		PlayerColour colour;
		if (hasPlayer(PlayerColour.WHITE)) {
			colour = PlayerColour.BLACK;
		} else {
			colour = PlayerColour.WHITE;
		}
		CheckersPlayer checkersPlayer = createPlayer(colour, playerName);
		checkersPlayer.validateInvited("Game.notInvited");
		checkersPlayer.validateAffordability("Game.cantAffordToJoin");
		players[colour.getIndex()] = checkersPlayer;

		PlayerColour otherColour = colour.getOtherColour();
		if (hasPlayer(otherColour)) {
			getPlayer(otherColour).alert(Messages.getString("Game.playerJoined", getPlayer(colour).getDisplayName()));
		}

		return checkersPlayer;
	}

	private void handlePayout() {
		// TODO Auto-generated method stub
	}

}
