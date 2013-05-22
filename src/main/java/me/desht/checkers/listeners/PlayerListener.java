package me.desht.checkers.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.IllegalMoveException;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.Checkers;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.checkers.responses.BoardCreationHandler;
import me.desht.checkers.responses.InvitePlayer;
import me.desht.checkers.util.CheckersUtils;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.block.BlockType;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import me.desht.dhutils.responsehandler.ResponseHandler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerListener extends CheckersBaseListener {

	private static final long MIN_ANIMATION_WAIT = 200; // milliseconds
	private Map<String,Long> lastAnimation = new HashMap<String, Long>();

	// block ids to be considered transparent when calling player.getTargetBlock()
	private static final HashSet<Byte> transparent = new HashSet<Byte>();
	static {
		transparent.add((byte) 0); // air
		transparent.add((byte) 20); // glass
	}

	public PlayerListener(CheckersPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ResponseHandler resp = plugin.getResponseHandler();
		String playerName = event.getPlayer().getName();
		Block b = event.getClickedBlock();

		try {
			if (resp.isExpecting(playerName, InvitePlayer.class)) {
				// a left or right-click (even air, where the event is cancelled) cancels any pending player invite response
				resp.cancelAction(playerName, InvitePlayer.class);
				MiscUtil.alertMessage(event.getPlayer(), Messages.getString("Game.playerInviteCancelled"));
			} else if (resp.isExpecting(playerName, BoardCreationHandler.class)) {
				BoardCreationHandler boardCreation = resp.getAction(playerName, BoardCreationHandler.class);
				switch (event.getAction()) {
				case LEFT_CLICK_BLOCK:
					boardCreation.setLocation(event.getClickedBlock().getLocation());
					boardCreation.handleAction();
					break;
				case RIGHT_CLICK_BLOCK: case RIGHT_CLICK_AIR:
					MiscUtil.alertMessage(event.getPlayer(),  Messages.getString("Board.boardCreationCancelled"));
					boardCreation.cancelAction();
					break;
				default:
					break;
				}
				event.setCancelled(true);
			} else if (b != null) {
				BoardView bv = BoardViewManager.getManager().partOfBoard(b.getLocation(), 0);
				if (bv != null && bv.getControlPanel().isButton(b.getLocation())) {
					bv.getControlPanel().handleButtonClick(event);
					event.setCancelled(true);
				}
			}
		} catch (CheckersException e) {
			MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
			if (resp.isExpecting(playerName, BoardCreationHandler.class)) {
				BoardCreationHandler boardCreation = resp.getAction(playerName, BoardCreationHandler.class);
				MiscUtil.alertMessage(event.getPlayer(),  Messages.getString("Board.boardCreationCancelled"));
				boardCreation.cancelAction();
			}
		} catch (DHUtilsException e) {
			MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
		}
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		Player player = event.getPlayer();

		// We seem to get multiple events very close together, leading to unwanted double actions sometimes.
		// So ignore events that happen too soon after the last one for a player.
		if (System.currentTimeMillis() - lastAnimationEvent(player) < MIN_ANIMATION_WAIT) {
			return;
		}
		lastAnimation.put(player.getName(), System.currentTimeMillis());

		Block targetBlock = null;
		BoardView bv = null;

		try {
			if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
				int wandId = CheckersUtils.getWandId();
				if (wandId < 0 || player.getItemInHand().getTypeId() == wandId) {
					targetBlock = player.getTargetBlock(transparent, 120);
					LogUtils.finer("Player " + player.getName() + " waved at block " + targetBlock);
					Location loc = targetBlock.getLocation();
					bv = BoardViewManager.getManager().partOfBoard(loc);
					if (bv != null) {
						if (bv.getBoard().getBoardSquares().contains(loc)) {
							boardClicked(player, loc, bv);
						} else if (bv.getBoard().getAboveSquares().contains(loc)) {
							pieceClicked(player, loc, bv);
						} else if (bv.isControlPanel(loc)) {
							Location tpLoc = bv.getTeleportInDestination();
							Cuboid zone = bv.getControlPanel().getPanelBlocks().outset(CuboidDirection.Horizontal, 4);
							if (!zone.contains(player.getLocation()) && bv.getBoard().isPartOfBoard(player.getLocation())) {
								teleportPlayer(player, tpLoc);
							}
						}
					}
				}
			}
		} catch (IllegalMoveException e) {
			// targetBlock must be non-null at this point
			cancelMove(bv);
			MiscUtil.errorMessage(player, e.getMessage());
			plugin.getFX().playEffect(player.getLocation(), "piece_unselected");
		} catch (CheckersException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		} catch (IllegalStateException e) {
			// player.getTargetBlock() throws this exception occasionally - it appears
			// to be harmless, so we'll ignore it
		}
	}

	@EventHandler(ignoreCancelled = true, priority=EventPriority.HIGH)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		ResponseHandler resp = plugin.getResponseHandler();
		InvitePlayer ip = resp.getAction(player.getName(), InvitePlayer.class);

		if (ip != null) {
			try {
				ip.setInviteeName(event.getMessage());
				event.setCancelled(true);
				ip.handleAction();
			} catch (CheckersException e) {
				MiscUtil.errorMessage(player, e.getMessage());
				ip.cancelAction();
			}
		}
	}

	private void cancelMove(BoardView view) {
		if (view != null) {
			view.getBoard().clearSelected();
		}
	}

	private void pieceClicked(Player player, Location loc, BoardView bv) {
		if (player.isSneaking()) {
			// shift-clicked a piece - try to teleport the player onto the piece
			teleportToPiece(player, bv, loc);
			return;
		}

		CheckersGame game = bv.getGame();
		if (game == null || game.getState() != GameState.RUNNING) {
			return;
		}
		CheckersPlayer cp = game.getPlayerToMove();
		if (!cp.getName().equals(player.getName())) {
			if (game.hasPlayer(player.getName())) {
				MiscUtil.errorMessage(player, Messages.getString("Game.notYourTurn"));
			} else {
				MiscUtil.errorMessage(player, Messages.getString("Game.notInGame"));
			}
			return;
		}

		CheckersGameManager.getManager().setCurrentGame(player.getName(), game);
		int sqi = bv.getSquareAt(loc);
		if (bv.getBoard().getSelectedSqi() != Checkers.NO_SQUARE) {
			if (sqi == bv.getBoard().getSelectedSqi()) {
				// cancel current selection
				bv.getBoard().clearSelected();
				plugin.getFX().playEffect(player.getLocation(), "piece_unselected");
			} else {
				// try to move the current piece
				tryMove(player, bv, sqi);
			}
		} else {
			// select this square if the piece is the right colour
			if (game.getPosition().getPieceAt(Checkers.sqiToRow(sqi), Checkers.sqiToCol(sqi)).getColour() == cp.getColour()) {
				bv.getBoard().setSelected(sqi);
				plugin.getFX().playEffect(player.getLocation(), "piece_selected");
			}
		}
	}

	private void boardClicked(Player player, Location loc, BoardView bv) {
		int sqi = bv.getSquareAt(loc);
		CheckersGame game = bv.getGame();
		if (game != null && bv.getBoard().getSelectedSqi() != Checkers.NO_SQUARE) {
			tryMove(player, bv, sqi);
		} else {
			if (player.isSneaking()) {
				int row = Checkers.sqiToRow(sqi);
				int col = Checkers.sqiToCol(sqi);
				if (row % 2 == col % 2) {
					MiscUtil.statusMessage(player, Messages.getString("Board.squareMessage", Checkers.rowColToCheckersNotation(row, col), bv.getName()));
				}
				if (bv.getBoard().isPartOfBoard(player.getLocation())) {
					// allow teleporting around the board, but only if the player is already on the board
					Location newLoc = loc.clone().add(0, 1.0, 0);
					newLoc.setPitch(player.getLocation().getPitch());
					newLoc.setYaw(player.getLocation().getYaw());
					teleportPlayer(player, newLoc);
				}
			}
		}
	}

	private void tryMove(Player player, BoardView bv, int toSqi) {
		int fromSqi = bv.getBoard().getSelectedSqi();
		CheckersGame game = bv.getGame();
		CheckersPlayer cp = game.getPlayerToMove();
		game.doMove(player.getName(), fromSqi, toSqi);
		MiscUtil.statusMessage(player, Messages.getString("Game.youPlayed", Checkers.sqiToCheckersNotation(fromSqi), Checkers.sqiToCheckersNotation(toSqi)));
		if (game.getState() != GameState.FINISHED) {
			if (game.getPosition().getToMove() == cp.getColour()) {
				// still the same player to move - must be a chained jump
				MiscUtil.statusMessage(player, Messages.getString("Game.mustContinueJumping"));
				bv.getBoard().setSelected(toSqi);
			} else {
				game.getPlayer(game.getPosition().getToMove()).promptForNextMove();
				bv.getBoard().clearSelected();
			}
		} else {
			bv.getBoard().clearSelected();
		}
	}

	private void teleportToPiece(Player player, BoardView bv, Location loc) {
		Block b = loc.getBlock();
		Block b1 = b.getRelative(BlockFace.UP);
		boolean isSolid = !BlockType.canPassThrough(bv.getBoard().getBoardStyle().getEnclosureMaterial().getId());
		int max = isSolid ? bv.getBoard().getFullBoard().getUpperY() - 2 : loc.getWorld().getMaxHeight();
		while (b.getType() != Material.AIR && b1.getType() != Material.AIR && b1.getLocation().getY() < max) {
			b = b.getRelative(BlockFace.UP);
			b1 = b1.getRelative(BlockFace.UP);
		}
		if (b1.getY() < max) {
			Location dest = b1.getLocation();
			dest.setYaw(player.getLocation().getYaw());
			dest.setPitch(player.getLocation().getPitch());
			teleportPlayer(player, dest);
		}
	}

	private void teleportPlayer(Player player, Location dest) {
		plugin.getFX().playEffect(player.getLocation(), "teleport_out");
		player.teleport(dest);
		plugin.getFX().playEffect(dest, "teleport_in");
	}

	private long lastAnimationEvent(Player player) {
		if (!lastAnimation.containsKey(player.getName())) {
			lastAnimation.put(player.getName(), 0L);
		}
		return lastAnimation.get(player.getName());
	}
}
