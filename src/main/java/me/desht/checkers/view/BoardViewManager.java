package me.desht.checkers.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.event.CheckersBoardCreatedEvent;
import me.desht.checkers.event.CheckersBoardDeletedEvent;
import me.desht.checkers.util.TerrainBackup;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BoardViewManager {

	private static BoardViewManager instance = null;

	private final Map<String, BoardView> chessBoards = new HashMap<String, BoardView>();
	private final Map<String, Set<File>> deferred = new HashMap<String, Set<File>>();
	private PersistableLocation globalTeleportOutDest = null;

	private BoardViewManager() {
	}

	public static synchronized BoardViewManager getManager() {
		if (instance == null) {
			instance = new BoardViewManager();
		}
		return instance;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * @return the globalTeleportOutDest
	 */
	public Location getGlobalTeleportOutDest() {
		return globalTeleportOutDest == null ? null : globalTeleportOutDest.getLocation();
	}

	/**
	 * @param globalTeleportOutDest the globalTeleportOutDest to set
	 */
	public void setGlobalTeleportOutDest(Location globalTeleportOutDest) {
		this.globalTeleportOutDest = globalTeleportOutDest == null ? null : new PersistableLocation(globalTeleportOutDest);
	}

	public void registerView(BoardView view) {
		chessBoards.put(view.getName(), view);

		Bukkit.getPluginManager().callEvent(new CheckersBoardCreatedEvent(view));
	}

	public void unregisterBoardView(String name) {
		BoardView bv;
		try {
			bv = getBoardView(name);
			chessBoards.remove(name);
			Bukkit.getPluginManager().callEvent(new CheckersBoardDeletedEvent(bv));
		} catch (CheckersException  e) {
			LogUtils.warning("removeBoardView: unknown board name " + name);
		}
	}

	public void removeAllBoardViews() {
		for (BoardView bv : listBoardViews()) {
			Bukkit.getPluginManager().callEvent(new CheckersBoardDeletedEvent(bv));
		}
		chessBoards.clear();
	}

	public boolean boardViewExists(String name) {
		return chessBoards.containsKey(name);
	}

	public BoardView getBoardView(String name) throws CheckersException {
		if (!chessBoards.containsKey(name)) {
			throw new CheckersException(Messages.getString("Board.noSuchBoard", name));
		}
		return chessBoards.get(name);
	}

	public Collection<BoardView> listBoardViews() {
		return chessBoards.values();
	}

	public Collection<BoardView> listBoardViewsSorted() {
		SortedSet<String> sorted = new TreeSet<String>(chessBoards.keySet());
		List<BoardView> res = new ArrayList<BoardView>();
		for (String name : sorted) {
			res.add(chessBoards.get(name));
		}
		return res;
	}

	/**
	 * Get a board that does not have a game running.
	 * 
	 * @return the first free board found
	 * @throws CheckersException if no free board was found
	 */
	public BoardView getFreeBoard() throws CheckersException {
		for (BoardView bv : listBoardViews()) {
			if (bv.getGame() == null) {
				return bv;
			}
		}
		throw new CheckersException(Messages.getString("Board.noFreeBoards")); //$NON-NLS-1$
	}

	/**
	 * Check if a location is any part of any board including the frame & enclosure.
	 * 
	 * @param loc	location to check
	 * @param fudge	fudge factor - check a larger area around the board
	 * @return the boardview that matches, or null if none
	 */
	public BoardView partOfChessBoard(Location loc) {
		return partOfChessBoard(loc, 0);
	}

	public BoardView partOfChessBoard(Location loc, int fudge) {
		for (BoardView bv : listBoardViews()) {
			if (bv.getBoard().isPartOfBoard(loc, fudge)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Check if location is above a board square but below the roof
	 * 
	 * @param loc  location to check
	 * @return the boardview that matches, or null if none
	 */
	public BoardView aboveChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.getBoard().isAboveBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Check if location is part of a board square
	 * 
	 * @param loc	location to check
	 * @return the boardview that matches, or null if none
	 */
	public BoardView onChessBoard(Location loc) {
		for (BoardView bv : listBoardViews()) {
			if (bv.getBoard().isOnBoard(loc)) {
				return bv;
			}
		}
		return null;
	}

	/**
	 * Teleport the player in a sensible manner, depending on where they are now.
	 * 
	 * @param player
	 * @throws CheckersException
	 */
	public void teleportOut(Player player) throws CheckersException {
		//		PermissionUtils.requirePerms(player, "chesscraft.commands.teleport");
		//
		//		BoardView bv = partOfChessBoard(player.getLocation(), 0);
		//		Location prev = CheckersPlugin.getInstance().getPlayerTracker().getLastPos(player);
		//		if (bv != null && bv.hasTeleportDestination()) {
		//			// board has a specific location defined
		//			Location loc = bv.getTeleportDestination();
		//			CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, loc);
		//		} else if (bv != null && globalTeleportOutDest != null) {
		//			CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, getGlobalTeleportOutDest());
		//		} else if (bv != null && (prev == null || partOfChessBoard(prev, 0) == bv)) {
		//			// try to get the player out of this board safely
		//			Location loc = bv.findSafeLocationOutside();
		//			if (loc != null) {
		//				CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, loc);
		//			} else {
		//				CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, player.getWorld().getSpawnLocation());
		//				MiscUtil.errorMessage(player, Messages.getString("Board.goingToSpawn")); //$NON-NLS-1$
		//			}
		//		} else if (prev != null) {
		//			// go back to previous location
		//			CheckersPlugin.getInstance().getPlayerTracker().teleportPlayer(player, prev);
		//		} else {
		//			throw new CheckersException(Messages.getString("Board.notOnChessboard")); //$NON-NLS-1$
		//		}
	}

	/**
	 * Convenience method to create a new board and do all the associated setup tasks.
	 * 
	 * @param boardName
	 * @param loc
	 * @param style
	 * @param pieceStyle
	 * @return a fully initialised and painted board
	 */
	public BoardView createBoard(String boardName, Location loc, BoardRotation rotation, String style) {
		BoardView view = new BoardView(boardName, loc, rotation, style);
		registerView(view);
		if (CheckersPlugin.getInstance().getWorldEdit() != null) {
			TerrainBackup.save(view);
		}
		view.save();
		view.repaint();

		return view;
	}

	/**
	 * Mark a board as deferred loading - its world wasn't available so we'll record the board
	 * file name for later.
	 * 
	 * @param worldName
	 * @param f
	 */
	public void deferLoading(String worldName, File f) {
		if (!deferred.containsKey(worldName)) {
			deferred.put(worldName, new HashSet<File>());
		}
		deferred.get(worldName).add(f);
	}

	/**
	 * Load any deferred boards for the given world.
	 * 
	 * @param worldName
	 */
	public void loadDeferred(String worldName) {
		if (!deferred.containsKey(worldName)) {
			return;
		}
		for (File f : deferred.get(worldName)) {
			CheckersPlugin.getInstance().getPersistenceHandler().loadBoard(f);
		}
		deferred.get(worldName).clear();
	}
}

