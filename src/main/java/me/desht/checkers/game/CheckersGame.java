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
import me.desht.checkers.IllegalMoveException;
import me.desht.checkers.Messages;
import me.desht.checkers.TimeControl;
import me.desht.checkers.ai.CheckersAI;
import me.desht.checkers.model.Checkers;
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
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

public class CheckersGame implements CheckersPersistable {
	public enum GameResult {
		WIN, DRAW_AGREED, ABANDONED, NOT_FINISHED, RESIGNED,
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
	private TimeControl tcWhite, tcBlack;
	private PlayerColour winner = PlayerColour.NONE;

	public CheckersGame(String gameName, String creatorName, PlayerColour colour, String tcSpec) {
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
		setTimeControl(tcSpec);
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
		this.tcWhite = (TimeControl) conf.get("tcWhite");
		this.tcBlack = (TimeControl) conf.get("tcBlack");

		players[PlayerColour.WHITE.getIndex()] = createPlayer(PlayerColour.WHITE, conf.getString("playerWhite"));
		players[PlayerColour.BLACK.getIndex()] = createPlayer(PlayerColour.BLACK, conf.getString("playerBlack"));

		// replay the saved move history
		List<Integer> encoded = conf.getIntegerList("moves");
		for (int m : encoded) {
			Move move = new Move(m);
			position.makeMove(move);
		}

		// set chess clock activity appropriately
		tcWhite.setActive(getPosition().getToMove() == PlayerColour.WHITE);
		tcBlack.setActive(!tcWhite.isActive());
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
		map.put("tcWhite", tcWhite);
		map.put("tcBlack", tcBlack);

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

	public CheckersPlayer getPlayerToMove() {
		return players[getPosition().getToMove().getIndex()];
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
	 * @param newState the state to set
	 */
	public void setState(GameState newState) {
		if (newState == GameState.RUNNING) {
			CheckersValidate.isTrue(this.state == GameState.SETTING_UP, "invalid state transition " + state + "->" + newState);
			started = lastMoved = System.currentTimeMillis();
		} else if (newState == GameState.FINISHED) {
			CheckersValidate.isTrue(this.state == GameState.RUNNING, "invalid state transition " + state + "->" + newState);
			finished = System.currentTimeMillis();
		}
		this.state = newState;
	}

	/**
	 * Called when a game is permanently deleted.
	 */
	public void deletePermanently() {
		CheckersPlugin.getInstance().getPersistenceHandler().unpersist(this);

		if (getState() == GameState.RUNNING) {
			gameOver(PlayerColour.NONE, GameResult.ABANDONED);
			handlePayout();
		}

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
		save();
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
			getPlayer(PlayerColour.BLACK).validateAffordability("Game.cantAffordToStart");
			getPlayer(PlayerColour.WHITE).withdrawFunds(stake);
			getPlayer(PlayerColour.BLACK).withdrawFunds(stake);
		}

		clearInvitation();
		setState(GameState.RUNNING);

		getPlayer(PlayerColour.WHITE).promptForFirstMove();

		save();

		for (GameListener l : listeners) {
			l.gameStarted(this);
		}
	}

	public void doMove(String playerName, int fromSqi, int toSqi) {
		ensureGameInState(GameState.RUNNING);
		ensurePlayerToMove(playerName);

		Move move = new Move(Checkers.sqiToRow(fromSqi), Checkers.sqiToCol(fromSqi), Checkers.sqiToRow(toSqi), Checkers.sqiToCol(toSqi));
		Move validMove = null;
		for (Move m : getPosition().getLegalMoves()) {
			if (m.equals(move)) {
				// legal move
				validMove = m;
				break;
			}
		}
		if (validMove == null) {
			throw new IllegalMoveException();
		}

		PlayerColour prevToMove = getPosition().getToMove();

		// the move is valid; make the necessary changes
		getPosition().makeMove(validMove);  // this will cause a board redraw
		lastMoved = System.currentTimeMillis();

		getPlayer(prevToMove).cancelOffers();

		if (getPosition().getLegalMoves().length == 0) {
			// no legal moves, therefore the player currently to-play has just lost
			setState(GameState.FINISHED);
			gameOver(getPosition().getToMove().getOtherColour(), GameResult.WIN);
		}

		save();
	}

	public void resign(String playerName) {
		ensureGameInState(GameState.RUNNING);
		ensurePlayerInGame(playerName);

		CheckersPlayer loser = getPlayer(playerName);
		setState(GameState.FINISHED);
		gameOver(loser.getColour().getOtherColour(), GameResult.RESIGNED);
	}

	public void drawn(GameResult result) {
		setState(GameState.FINISHED);
		gameOver(PlayerColour.NONE, result);
	}

	public void tick() {
		checkForAutoDelete();
		checkForAIActivity();
	}

	public TimeControl getTimeControl(PlayerColour colour) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTimeControl(String spec) {
		ensureGameInState(GameState.SETTING_UP);
		tcWhite = new TimeControl(spec);
		tcBlack = new TimeControl(spec);
	}

	public void setStake(String playerName, double newStake) {
		Economy economy = CheckersPlugin.getInstance().getEconomy();
		if (economy == null || !economy.isEnabled()) {
			return;
		}

		ensureGameInState(GameState.SETTING_UP);
		ensurePlayerInGame(playerName);

		if (newStake < 0.0) {
			throw new CheckersException(Messages.getString("Game.noNegativeStakes"));
		}

		if (!economy.has(playerName, newStake)) {
			throw new CheckersException(Messages.getString("Game.cantAffordStake"));
		}

		double max = CheckersPlugin.getInstance().getConfig().getDouble("stake.max");
		if (max >= 0.0 && newStake > max) {
			throw new CheckersException(Messages.getString("Game.stakeTooHigh", max));
		}

		if (isFull()) {
			throw new CheckersException(Messages.getString("Game.stakeCantBeChanged"));
		}

		this.stake = newStake;
	}

	public void adjustStake(String playerName, double adjustment) {
		Economy economy = CheckersPlugin.getInstance().getEconomy();
		if (economy == null || !economy.isEnabled()) {
			return;
		}

		double newStake = getStake() + adjustment;
		double max = CheckersPlugin.getInstance().getConfig().getDouble("stake.max");

		if (max >= 0.0 && newStake > max && adjustment < 0.0) {
			// allow stake to be adjusted down without throwing an exception
			// could happen if global max stake was changed to something lower than
			// a game's current stake setting
			newStake = Math.min(max, economy.getBalance(playerName));
		}
		if (!economy.has(playerName, newStake) && adjustment < 0.0) {
			// similarly for the player's own balance
			newStake = Math.min(max, economy.getBalance(playerName));
		}

		setStake(playerName, newStake);
	}

	public void offerDraw(String playerName) {
		ensurePlayerInGame(playerName);
		ensurePlayerToMove(playerName);
		ensureGameInState(GameState.RUNNING);

		CheckersPlayer offeringPlayer = getPlayer(playerName);
		CheckersPlayer offeredPlayer = getPlayer(offeringPlayer.getColour().getOtherColour());
		offeringPlayer.statusMessage(Messages.getString("Game.youOfferDraw", offeredPlayer.getName()));
		offeredPlayer.drawOffered();
	}

	public void offerUndoMove(String name) {
		// TODO Auto-generated method stub

	}

	private void checkForAIActivity() {
		// TODO add AI support
	}

	private void checkForAutoDelete() {
		long now = System.currentTimeMillis();
		ConfigurationSection cs = CheckersPlugin.getInstance().getConfig().getConfigurationSection("auto_delete");

		String alertStr = null;
		if (getState() == GameState.SETTING_UP) {
			Duration timeout = new Duration(cs.getString("not_started", "90 sec"));
			if (now - created > timeout.getTotalDuration()) {
				alertStr = Messages.getString("Game.autoDeleteNotStarted", timeout);
			}
		} else if (getState() == GameState.RUNNING) {
			Duration timeout = new Duration(cs.getString("running", "28 days"));
			if (now - lastMoved > timeout.getTotalDuration()) {
				alertStr = Messages.getString("Game.autoDeleteRunning", timeout);
			}
		} else if (getState() == GameState.FINISHED) {
			Duration timeout = new Duration(cs.getString("finished", "15 sec"));
			if (now - finished > timeout.getTotalDuration()) {
				alertStr = Messages.getString("Game.autoDeleteFinished");
			}
		}

		if (alertStr != null) {
			alert(alertStr);
			LogUtils.info(alertStr);
			deletePermanently();
		}
	}

	private void gameOver(PlayerColour winner, GameResult result) {
		String msg = "";
		this.result = result;
		this.winner = winner;
		CheckersPlayer p1 = winner == PlayerColour.NONE ? getPlayer(PlayerColour.WHITE) : getPlayer(winner);
		CheckersPlayer p2 = getPlayer(p1.getColour().getOtherColour());
		switch (result) {
		case WIN:
			msg = Messages.getString("Game.resultWin", p1.getName(), p2.getName());
			if (!p1.getName().equals(p2.getName())) {
				p1.playEffect("game_won");
				p2.playEffect("game_lost");
			}
			break;
		case DRAW_AGREED:
			msg = Messages.getString("Game.resultDrawAgreed", p1.getName(), p2.getName());
			break;
		case RESIGNED:
			msg = Messages.getString("Game.resultResigned", p1.getName(), p2.getName());
			break;
		case ABANDONED:
			String name1 = p1 == null ? "???" : p1.getName();
			String name2 = p2 == null ? "???" : p2.getName();
			msg = Messages.getString("Game.resultAbandoned", name1, name2);
			break;
		default:
			break;
		}
		if (!msg.isEmpty()) {
			if (CheckersPlugin.getInstance().getConfig().getBoolean("broadcast_results")) {
				MiscUtil.broadcastMessage(msg);
			} else {
				alert(msg);
			}
		}
		if (p1 == null || p2 == null || p1.getName().equals(p2.getName())) {
			return;
		}

		handlePayout();

		// TODO: result logging to database
	}

	private void inviteSanityCheck(String inviterName) {
		ensurePlayerInGame(inviterName);
		ensureGameInState(GameState.SETTING_UP);
	}

	private void ensurePlayerToMove(String playerName) {
		CheckersValidate.isTrue(playerName.equals(getPlayerToMove().getName()), Messages.getString("Game.notYourTurn"));
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
		if (stake <= 0.0 || getPlayerName(PlayerColour.WHITE).equals(getPlayerName(PlayerColour.BLACK))) {
			return;
		}
		if (getState() == GameState.SETTING_UP) {
			return;
		}

		if (result == GameResult.WIN) {
			// winner takes the stake multiplied by the loser's payout multiplier
			CheckersPlayer winner = getPlayer(getPosition().getToMove().getOtherColour());
			CheckersPlayer loser  = getPlayer(getPosition().getToMove());
			double winnings = stake * loser.getPayoutMultiplier();
			winner.depositFunds(winnings);
			winner.alert(Messages.getString("Game.stakeWon", CheckersUtils.formatStakeStr(winnings)));
			loser.alert(Messages.getString("Game.stakeLost", CheckersUtils.formatStakeStr(stake)));
		} else if (result == GameResult.RESIGNED) {
			
		} else {
			// draw or abandoned; return original stakes
			getPlayer(PlayerColour.WHITE).depositFunds(stake);
			getPlayer(PlayerColour.BLACK).depositFunds(stake);
			getPlayer(PlayerColour.WHITE).alert("Game.stakeReturned");
			getPlayer(PlayerColour.BLACK).alert("Game.stakeReturned");
		}

		stake = 0.0;
	}
}
