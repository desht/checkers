package me.desht.checkers.view;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.checkers.*;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.game.GameListener;
import me.desht.checkers.model.*;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.checkers.util.CheckersUtils;
import me.desht.checkers.util.TerrainBackup;
import me.desht.checkers.view.controlpanel.ControlPanel;
import me.desht.checkers.view.controlpanel.StakeButton;
import me.desht.checkers.view.controlpanel.TimeControlButton;
import me.desht.dhutils.AttributeCollection;
import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.block.CraftMassBlockUpdate;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class BoardView implements PositionListener, ConfigurationListener, CheckersPersistable, ConfigurationSerializable, GameListener {
	private static final String BOARD_STYLE = "boardstyle";
	private static final String DEFAULT_STAKE = "defaultstake";
	private static final String LOCK_STAKE = "lockstake";
	private static final String DEFAULT_TC = "defaulttc";
	private static final String LOCK_TC = "locktc";

	private final String name;
	private final CheckersBoard checkersBoard;
	private final ControlPanel controlPanel;
	private final String savedGameName;
	private final String worldName;
	private final AttributeCollection attributes;

	private CheckersGame game;
	private PersistableLocation teleportOutDest;

	public BoardView(String name, Location loc, BoardRotation rot, String boardStyle, int size) {
		this.name = name;
		this.game = null;
		this.savedGameName = "";
		this.attributes = new AttributeCollection(this);
		registerAttributes();
		attributes.set(BOARD_STYLE, boardStyle);
		this.checkersBoard = new CheckersBoard(loc, rot, boardStyle, size);
		this.worldName = checkersBoard.getWorld().getName();
		this.controlPanel = new ControlPanel(this);
	}

	public BoardView(ConfigurationSection conf) {
		this.name = conf.getString("name");
		if (BoardViewManager.getManager().boardViewExists(name)) {
			throw new CheckersException(Messages.getString("Board.boardExists"));
		}
		this.savedGameName = conf.getString("game", "");
		this.attributes = new AttributeCollection(this);
		registerAttributes();
		for (String attr : attributes.listAttributeKeys(false)) {
			if (conf.contains(attr)) {
				attributes.set(attr, conf.getString(attr));
			}
		}

		PersistableLocation where = (PersistableLocation) conf.get("origin");
		this.worldName = where.getWorldName();
		if (!where.isWorldAvailable()) {
			this.checkersBoard = null;
			this.controlPanel = null;
			return;
		}

		teleportOutDest = conf.contains("teleportOutDest") ? (PersistableLocation) conf.get("teleportOutDest") : null;

		BoardRotation dir = BoardRotation.getRotation(conf.getString("rotation"));
		int size = conf.getInt("size", 8);
		this.checkersBoard = new CheckersBoard(where.getLocation(), dir, (String)attributes.get(BOARD_STYLE), size);
		this.controlPanel = new ControlPanel(this);
	}

	private void registerAttributes() {
		attributes.registerAttribute(BOARD_STYLE, "", "Board style for this board");
		attributes.registerAttribute(DEFAULT_STAKE, 0.0, "Default stake for games on this board");
		attributes.registerAttribute(LOCK_STAKE, false, "Disallow changing of stake by players");
		attributes.registerAttribute(DEFAULT_TC, "NONE", "Default time control for games on this board");
		attributes.registerAttribute(LOCK_TC, false, "Disallow changing of time control by players");
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("name", name);
		result.put("size", checkersBoard.getSize());
		result.put("game", game == null ? "" : game.getName());
		result.put("origin", checkersBoard.getA1Center());
		result.put("rotation", checkersBoard.getRotation().name());
		if (teleportOutDest != null) {
			result.put("teleportOutDest", teleportOutDest);
		}
		for (String k : attributes.listAttributeKeys(false)) {
			result.put(k, attributes.get(k));
		}
		return result;
	}

	public static BoardView deserialize(Map<String, Object> map) {
		Configuration conf = new MemoryConfiguration();

		for (Entry<String, Object> e : map.entrySet()) {
			if (!conf.contains(e.getKey())) {
				conf.set(e.getKey(), e.getValue());
			}
		}

		return new BoardView(conf);
	}

	@Override
	public File getSaveDirectory() {
		return DirectoryStructure.getBoardPersistDirectory();
	}

	public void save() {
		CheckersPlugin.getInstance().getPersistenceHandler().savePersistable("board", this);
	}

	public String getName() {
		return name;
	}

	public String getWorldName() {
		return worldName;
	}

	public CheckersGame getGame() {
		return game;
	}

	public void setGame(CheckersGame game) {
		this.game = game;

		if (game != null) {
			int ruleSize = game.getPosition().getRules().getBoardSize();
			CheckersValidate.isTrue(ruleSize == getBoard().getSize(),
					Messages.getString("Game.invalidBoardSize", game.getPosition().getRules().getId(), ruleSize));
			game.getPosition().addPositionListener(this);
			game.addGameListener(this);
			if (game.getPosition().getMoveHistory().length > 0) {
				getBoard().setLastMovedSquare(game.getPosition().getLastMove().getTo());
			}
			playerAdded(game, game.getPlayer(PlayerColour.BLACK));
			playerAdded(game, game.getPlayer(PlayerColour.WHITE));
		} else {
			getBoard().reset();
			getControlPanel().repaintControls();
		}

		save();
	}

	public String getSavedGameName() {
		return savedGameName;
	}

	public World getWorld() {
		return getBoard().getWorld();
	}

	public CheckersBoard getBoard() {
		return checkersBoard;
	}

	public ControlPanel getControlPanel() {
		return controlPanel;
	}

	public double getDefaultStake() {
		return (Double) attributes.get(DEFAULT_STAKE);
	}

	public boolean getLockStake() {
		return (Boolean) attributes.get(LOCK_STAKE);
	}

	public String getDefaultTcSpec() {
		return (String) attributes.get(DEFAULT_TC);
	}

	public boolean getLockTcSpec() {
		return (Boolean) attributes.get(LOCK_TC);
	}

	public AttributeCollection getAttributes() {
		return attributes;
	}

	public boolean hasTeleportOutDestination() {
		return teleportOutDest != null;
	}

	public Location getTeleportOutDestination() {
		return teleportOutDest == null ? null : teleportOutDest.getLocation();
	}

	public void setTeleportOutDestination(Location location) {
		teleportOutDest = new PersistableLocation(location);
	}

	public Location getTeleportInDestination() {
		return getControlPanel().getTeleportInDestination();
	}

	public boolean isControlPanel(Location loc) {
		// outsetting the cuboid allows the signs on the panel to be targeted too
		return controlPanel.getPanelBlocks().outset(CuboidDirection.Horizontal, 1).contains(loc);
	}

	public boolean isWorldAvailable() {
		return getBoard() != null;
	}

	public RowCol getSquareAt(Location loc) {
		return getBoard().getSquareAt(loc);
	}

	public Location findSafeLocationOutside() {
		Location dest = getBoard().getFullBoard().outset(CuboidDirection.Horizontal, 1).getLowerNE();
		return dest.getWorld().getHighestBlockAt(dest).getLocation();
	}

	public void repaint() {
		MassBlockUpdate mbu = CraftMassBlockUpdate.createMassBlockUpdater(CheckersPlugin.getInstance(), getWorld());
		checkersBoard.repaint(mbu);
		controlPanel.repaint(mbu);
		if (game != null) {
			checkersBoard.paintPieces(game.getPosition());
		}
		mbu.notifyClients();
	}

	/**
	 * Permanently delete a board, purging its data from disk and restoring the terrain behind it.
	 */
	public void deletePermanently() {
		if (getGame() != null) {
			throw new CheckersException(Messages.getString("Board.boardCantBeDeleted", getName(), getGame().getName()));
		}
		deleteCommon();
		restoreTerrain();
		CheckersPlugin.getInstance().getPersistenceHandler().unpersist(this);
	}

	/**
	 * Temporarily delete a board; called when reloading saved data.
	 */
	public void deleteTemporary() {
		if (getGame() != null) {
			getGame().deleteTemporary();
		}
		deleteCommon();
	}

	private void deleteCommon() {
		BoardViewManager.getManager().unregisterBoardView(getName());
	}

	private void restoreTerrain() {
		boolean restored = false;

		// signs can get dropped otherwise
		getControlPanel().removeSigns();

		if (CheckersPlugin.getInstance().getWorldEdit() != null) {
			// WorldEdit will take care of changes being pushed to client
			restored = TerrainBackup.reload(this);
		}

		if (!restored) {
			// we couldn't restore the original terrain - just set the board to air
			checkersBoard.clearAll();
		}
	}

	public List<String> getBoardDetail() {
		List<String> res = new ArrayList<String>();

		String bullet = MessagePager.BULLET + ChatColor.YELLOW;
		Cuboid bounds = checkersBoard.getFullBoard();
		BoardStyle style = checkersBoard.getBoardStyle();
		String gameName = getGame() != null ? getGame().getName() : "-";

		res.add(Messages.getString("Board.boardDetail.board", getName()));
		res.add(bullet + Messages.getString("Board.boardDetail.boardExtents", MiscUtil.formatLocation(bounds.getLowerNE()), MiscUtil.formatLocation(bounds.getUpperSW())));
		res.add(bullet + Messages.getString("Board.boardDetail.game", gameName));
		res.add(bullet + Messages.getString("Board.boardDetail.boardOrientation", checkersBoard.getRotation().toString()));
		res.add(bullet + Messages.getString("Board.boardDetail.boardStyle", style.getName()));
		res.add(bullet + Messages.getString("Board.boardDetail.pieces", style.getWhitePieceMaterial(), style.getBlackPieceMaterial()));
		res.add(bullet + Messages.getString("Board.boardDetail.squareSize", style.getSquareSize(), style.getWhiteSquareMaterial(), style.getBlackSquareMaterial()));
		res.add(bullet + Messages.getString("Board.boardDetail.frameWidth", style.getFrameWidth(), style.getFrameMaterial()));
		res.add(bullet + Messages.getString("Board.boardDetail.enclosure", style.getEnclosureMaterial()));
		res.add(bullet + Messages.getString("Board.boardDetail.struts", style.getStrutsMaterial()));
		res.add(bullet + Messages.getString("Board.boardDetail.height", style.getHeight()));
		res.add(bullet + Messages.getString("Board.boardDetail.lightLevel", style.getLightLevel()));
		String lockStakeStr = getLockStake() ? Messages.getString("Board.boardDetail.locked") : "";
		res.add(bullet + Messages.getString("Board.boardDetail.defaultStake", CheckersUtils.formatStakeStr(getDefaultStake()), lockStakeStr));
		String lockTcStr = getLockTcSpec() ? Messages.getString("Board.boardDetail.locked") : "";
		res.add(bullet + Messages.getString("Board.boardDetail.defaultTimeControl", getDefaultTcSpec(), lockTcStr));
		String dest = hasTeleportOutDestination() ? MiscUtil.formatLocation(getTeleportOutDestination()) : "-";
		res.add(bullet + Messages.getString("Board.boardDetail.teleportDest", dest));

		return res;
	}

	public void tick() {
		if (game != null) {
			updateClocks(false);
			game.tick();
		}
	}

	private void updateClocks(boolean force) {
		updateClock(PlayerColour.WHITE, force);
		updateClock(PlayerColour.BLACK, force);
	}

	private void updateClock(PlayerColour colour, boolean force) {
		TwoPlayerClock clock = game.getClock();
		clock.tick();
		if (!force && colour != game.getPosition().getToMove()) {
			return;
		}
		getControlPanel().updateClock(colour, clock.getClockString(colour));

		if (game.getState() == GameState.RUNNING) {
			CheckersPlayer cp = game.getPlayer(colour);
			if (clock.getRemainingTime(colour) <= 0) {
				try {
					game.forfeit(cp.getName());
				} catch (CheckersException e) {
					LogUtils.severe("unexpected exception: " + e.getMessage(), e);
				}
			} else {
				cp.timeControlCheck();
			}
		}
	}

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal,
			Object newVal) {
		if (key.equals(DEFAULT_TC) && !newVal.toString().isEmpty()) {
			new TimeControl(newVal.toString());		// force validation of the spec
		}
	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equals(DEFAULT_TC) && getControlPanel() != null) {
			String spec = newVal.toString();
			if (spec.isEmpty()) {
				spec = CheckersPlugin.getInstance().getConfig().getString("time_control.default");
			}
			getControlPanel().getTcDefs().addCustomSpec(spec);
			getControlPanel().getButton(TimeControlButton.class).repaint();
		} else if (key.equals(BOARD_STYLE) && checkersBoard != null) {
			checkersBoard.setBoardStyle(newVal.toString());
		}
	}

	@Override
	public void moveMade(Position position, Move move) {
		if (move.isJump()) {
			int cr = (move.getFromRow() + move.getToRow()) / 2;
			int cc = (move.getFromCol() + move.getToCol()) / 2;
			Location loc = getBoard().getSquare(RowCol.get(cr, cc)).getCenter();
			CheckersPlugin.getInstance().getFX().playEffect(loc.add(0, 2, 0), "piece_captured");
		} else {
			CheckersPlayer cp = getGame().getPlayer(position.getToMove().getOtherColour());
			cp.playEffect("piece_moved");
		}

		getBoard().setLastMovedSquare(move.getTo());
		getBoard().clearSelected();
	}

	@Override
	public void lastMoveUndone(Position position) {
		Move m = position.getLastMove();
		if (m == null) {
			getBoard().clearLastMovedSquare();
		} else {
			getBoard().setLastMovedSquare(position.getLastMove().getTo());
		}
		getBoard().clearSelected();
	}

	@Override
	public void squareChanged(RowCol square, PieceType piece) {
		checkersBoard.paintPiece(square, piece);
	}

	@Override
	public void plyCountChanged(int plyCount) {
		getControlPanel().updatePlyCount();
	}

	@Override
	public void toMoveChanged(PlayerColour toMove) {
		getControlPanel().updateToMoveIndicator(toMove);
		game.getClock().toggle();
		updateClocks(true);
	}

	@Override
	public void halfMoveClockChanged(int halfMoveClock) {
		getControlPanel().updateHalfMoveClock();
	}

	@Override
	public void gameDeleted(CheckersGame game) {
		setGame(null);
	}

	@Override
	public void playerAdded(CheckersGame checkersGame, CheckersPlayer checkersPlayer) {
		if (checkersPlayer != null) {
			getControlPanel().repaintControls();
			if (CheckersPlugin.getInstance().getConfig().getBoolean("auto_teleport_on_join")) {
				checkersPlayer.teleport(this);
			}
		}
	}

	@Override
	public void gameStarted(CheckersGame checkersGame) {
		if (CheckersPlugin.getInstance().getConfig().getBoolean("auto_teleport_on_start")) {
			checkersGame.getPlayer(PlayerColour.WHITE).teleport(this);
			checkersGame.getPlayer(PlayerColour.BLACK).teleport(this);
		}
		getControlPanel().repaintControls();
	}

	@Override
	public boolean tryStakeChange(double newStake) {
		return !getLockStake();
	}

	@Override
	public void stakeChanged(double newStake) {
		getControlPanel().getButton(StakeButton.class).repaint();
	}

	@Override
	public boolean tryTimeControlChange(String tcSpec) {
		return !getLockTcSpec();
	}

	@Override
	public void timeControlChanged(String tcSpec) {
		ControlPanel cp = getControlPanel();
		cp.getTcDefs().addCustomSpec(tcSpec);
		cp.getButton(TimeControlButton.class).repaint();
		updateClocks(true);
	}

	@Override
	public void selectSquare(RowCol square) {
		if (square == null) {
			getBoard().clearSelected();
		} else {
			getBoard().setSelected(square);
		}
	}
}
