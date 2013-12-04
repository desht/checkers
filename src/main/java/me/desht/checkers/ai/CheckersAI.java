package me.desht.checkers.ai;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.TwoPlayerClock;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.Move;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.RowCol;
import me.desht.dhutils.LogUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

public abstract class CheckersAI implements Runnable {
	/*
	 * Special character ensures AI name cannot (easily) be faked/hacked, also
	 * adds another level of AI name visibility. Users/admins should NOT be given
	 * control of this prefix - use something else to enable changing AI name
	 * colors, if wanted.
	 */
	public static final String AI_PREFIX = ChatColor.WHITE.toString();

	public enum PendingAction { NONE, MOVED, DRAW_OFFERED, DRAW_ACCEPTED, DRAW_DECLINED, UNDO_ACCEPTED, UNDO_DECLINED }

	private boolean active = false;
	private BukkitTask aiTask;
	private boolean hasFailed = false;
	private PendingAction pendingAction = PendingAction.NONE;
	private RowCol pendingFrom, pendingTo;
	private boolean ready = false;
	private boolean drawOffered = false; // draw offered *to* the AI

	private final String name;
	private final CheckersGame checkersGame;
	private final PlayerColour aiColour;
	protected final ConfigurationSection params;
	protected final String gameDetails;

	public CheckersAI(String name, CheckersGame checkersGame, PlayerColour aiColour, ConfigurationSection params) {
		this.name = name;
		this.checkersGame = checkersGame;
		this.aiColour = aiColour;
		this.params = params;
		this.gameDetails = "game [" + checkersGame.getName() + "] AI [" + getName() + "]: ";
	}

	public static boolean isAIPlayer(String playerName) {
		return playerName.startsWith(AI_PREFIX);
	}

	/**
	 * Perform the implementation-specfic steps needed to cleanly shuto down this AI instance.
	 */
	public abstract void shutdown();

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * Called when the AI has to calculate its next move.
	 */
	public abstract void run();

	/**
	 * Perform the implementation-specfic steps needed to undo the AI's last move.
	 */
	public abstract void undoLastMove();

	public abstract void notifyTimeControl(TwoPlayerClock clock);

	/**
	 * Perform the implementation-specfic steps needed to update the AI's internal game model with
	 * the given move.
	 *
	 * @param fromSquare	Square being moved from
	 * @param toSquare		Square being move to
	 * @param otherPlayer	true if this is the other player moving, false if it's us
	 */
	protected abstract void movePiece(RowCol fromSquare, RowCol toSquare, boolean otherPlayer);

	/**
	 * Offer a draw to the AI.  The default implementation just rejects any offers, but subclasses may
	 * override this if the implementing AI supports being offered a draw.
	 */
	public void offerDraw() {
		drawOfferResponse(false);
	}

	/**
	 * Ask the AI if undoing the last move is acceptable.
	 */
	public void offerUndo() {
		if (getCheckersGame().getStake() == 0.0) {
			undoOfferResponse(CheckersPlugin.getInstance().getConfig().getBoolean("ai.accept_undo_offers.no_stake"));
		} else {
			undoOfferResponse(CheckersPlugin.getInstance().getConfig().getBoolean("ai.accept_undo_offers.stake"));
		}
	}

	/**
	 * Get the AI's canonical name.  This is dependent only on the internal prefix.
	 *
	 * @return the canonical name
	 */
	public String getName() {
		return CheckersAI.AI_PREFIX + name;
	}

	/**
	 * Get the AI's displayed name.  This may vary depending on the "ai.name_format" config setting.
	 *
	 * @return the displayed name
	 */
	public String getDisplayName() {
		String fmt = CheckersPlugin.getInstance().getConfig().getString("ai.name_format", "[AI]<NAME>").replace("<NAME>", name);
		return CheckersAI.AI_PREFIX + fmt + ChatColor.RESET;
	}

	public CheckersGame getCheckersGame() {
		return checkersGame;
	}

	protected boolean isDrawOfferedToAI() {
		return drawOffered;
	}

	protected void setDrawOfferedToAI(boolean drawOffered) {
		this.drawOffered = drawOffered;
	}

	public PlayerColour getColour() {
		return aiColour;
	}

	public PendingAction getPendingAction() {
		return pendingAction;
	}

	public void clearPendingAction() {
		pendingAction = PendingAction.NONE;
	}

	public RowCol getPendingFrom() {
		return pendingFrom;
	}

	public RowCol getPendingTo() {
		return pendingTo;
	}

	public boolean hasFailed() {
		return hasFailed;
	}

	public void setFailed(boolean failed) {
		hasFailed = failed;
	}

	protected void setReady() {
		ready = true;
	}

	public boolean isReady() {
		return ready;
	}

	/**
	 * Check if it's the AI's move.  Note this does not necessarily mean the AI is actively thinking
	 * right now, just that it's the AI's move.
	 *
	 * @return true if this AI is to move, false otherwise
	 */
	public boolean toMove() {
		PlayerColour toMove = getCheckersGame().getPosition().getToMove();
		return getColour() == toMove;
	}

	/**
	 * Delete a running AI instance.  Called when a game is finished, deleted, or the plugin is disabled.
	 */
	public void delete() {
		setActive(false);
		CheckersPlugin.getInstance().getAIFactory().deleteAI(this);
		shutdown();
	}

	/**
	 * Set the AI-active state.  Will cause either the launch or termination of the AI calculation thread.
	 *
	 * @param active true if the AI should become active
	 */
	public void setActive(boolean active) {
		if (active == this.active)
			return;

		this.active = active;

		LogUtils.fine(gameDetails + "active => " + active);

		if (active) {
			startThinking();
		} else {
			stopThinking();
		}
	}

	/**
	 * Inform the AI that the other player has made the given move.  We are assuming the move is legal.
	 * This also sets this AI to active, so it starts calculating the next move.
	 *
	 * @param fromSquare	the square the other player has moved from
	 * @param toSquare		the square the other player has moved to
	 */
	public void userHasMoved(RowCol fromSquare, RowCol toSquare) {
		if (active) {
			LogUtils.warning(gameDetails + "userHasMoved() called while AI is active?");
			return;
		}

		try {
			movePiece(fromSquare, toSquare, true);
			LogUtils.fine(gameDetails + "userHasMoved: " + fromSquare + "->" + toSquare);
		} catch (Exception e) {
			// oops
			aiHasFailed(e);
		}

		setActive(true);
	}

//	/**
//	 * Replay a list of moves into the AI object.  Called when a game is restored
//	 * from persisted data.
//	 *
//	 * @param moves a list of moves
//	 */
//	public void replayMoves(Move[] moves) {
//		active = getColour() == PlayerColour.BLACK;
//		for (Move move : moves) {
//			movePiece(move.getFrom(), move.getTo(), !active);
//			active = !active;
//		}
//		LogUtils.fine(gameDetails + "CheckersAI: replayed " + moves.length + " moves: AI to move = " + active);
//		if (active) {
//			startThinking();
//		}
//	}

	/**
	 * Tell the AI to setActive thinking.  This will call a run() method, implemented in subclasses,
	 * which will analyze the current board position and culminate by calling aiHasMoved() with the
	 * AI's next move.
	 */
	private void startThinking() {
		long delay = CheckersPlugin.getInstance().getConfig().getInt("ai.min_move_wait", 0);
		aiTask = Bukkit.getScheduler().runTaskLaterAsynchronously(CheckersPlugin.getInstance(), this, delay * 20L);
	}

	/**
	 * Tell the AI to stop thinking.
	 */
	private void stopThinking() {
		if (Bukkit.getScheduler().isCurrentlyRunning(aiTask.getTaskId())) {
			LogUtils.fine(gameDetails + "forcing shutdown for AI task #" + aiTask);
			aiTask.cancel();
		}
		aiTask = null;
	}

	/**
	 * Called when the AI has come up with its next move.
	 *
	 * @param fromSquare	the square the AI is moving from
	 * @param toSquare		the square the AI is moving to.
	 */
	protected void aiHasMoved(RowCol fromSquare, RowCol toSquare) {
		if (!active) {
			LogUtils.warning(gameDetails + "aiHasMoved() called when AI not active?");
			return;
		}

		if (isDrawOfferedToAI()) {
			// making a move effectively rejects any pending draw offer
			drawOfferResponse(false);
		}

		setActive(false);
		movePiece(fromSquare, toSquare, false);
		LogUtils.fine(gameDetails + "aiHasMoved: " + fromSquare + "->" + toSquare);

		// Moving directly isn't thread-safe: we'd end up altering the Minecraft world from a separate thread,
		// which is Very Bad.  So we just note the move made now, and let the CheckersGame object check for it on
		// the next clock tick.
		synchronized (checkersGame) {
			pendingFrom = fromSquare;
			pendingTo = toSquare;
			pendingAction = PendingAction.MOVED;
		}
	}

	protected void makeDrawOffer() {
		pendingAction = PendingAction.DRAW_OFFERED;
	}

	protected void drawOfferResponse(boolean accept) {
		pendingAction = accept ? PendingAction.DRAW_ACCEPTED : PendingAction.DRAW_DECLINED;
	}

	protected void undoOfferResponse(boolean accept) {
		pendingAction = accept ? PendingAction.UNDO_ACCEPTED : PendingAction.UNDO_DECLINED;
	}

	/**
	 * Something has gone horribly wrong.  Need to abandon this game.
	 *
	 * @param e
	 */
	protected void aiHasFailed(Exception e) {
		LogUtils.severe(gameDetails + "Unexpected Exception in AI");
		e.printStackTrace();
		checkersGame.alert(Messages.getString("AI.AIunexpectedException", e.getMessage()));
		hasFailed = true;
	}
}
