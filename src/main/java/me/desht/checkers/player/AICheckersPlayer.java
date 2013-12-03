package me.desht.checkers.player;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.IllegalMoveException;
import me.desht.checkers.Messages;
import me.desht.checkers.ai.AIFactory.AIDefinition;
import me.desht.checkers.ai.CheckersAI;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameResult;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.model.Move;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.RowCol;
import me.desht.checkers.view.BoardView;
import me.desht.dhutils.LogUtils;

import org.bukkit.Location;

public class AICheckersPlayer extends CheckersPlayer {

	private final CheckersAI ai;

	public AICheckersPlayer(String name, CheckersGame game, PlayerColour colour) {
		super(name, game, colour);

		ai = CheckersPlugin.getInstance().getAIFactory().getNewAI(name, game, colour);
	}

	@Override
	public void promptForFirstMove() {
		ai.setActive(true);
	}

	@Override
	public void promptForNextMove() {
		Move m = getGame().getPosition().getLastMove();
		ai.userHasMoved(m.getFrom(), m.getTo());
	}

	@Override
	public void promptForContinuedMove() {
		Move lastMove = getGame().getPosition().getLastMove();
		getGame().getPlayer(getColour().getOtherColour()).alert(Messages.getString("Game.playerPlayedChainedMove", getColour().getDisplayColour(), lastMove));
		ai.setActive(true);
	}

	@Override
	public void alert(String message) {
		// do nothing here
	}

	@Override
	public void statusMessage(String message) {
		// do nothing here
	}

//	@Override
//	public void replayMoves() {
//		ai.replayMoves(getGame().getPosition().getMoveHistory());
//	}

	@Override
	public String getDisplayName() {
		return ai.getDisplayName();
	}

	@Override
	public void cleanup() {
		ai.delete();
	}

	@Override
	public void validateAffordability(String error) {
		// nothing to do here - AI's have infinite resources, for now
		// (limited AI resources a possible future addition)
	}

	@Override
	public void validateInvited(String error) {
		// nothing to do here - AI's do not need invites
	}

	@Override
	public boolean isHuman() {
		return false;
	}

	@Override
	public void withdrawFunds(double amount) {
		// nothing to do here - AI's have infinite resources, for now
	}

	@Override
	public void depositFunds(double amount) {
		// nothing to do here - AI's have infinite resources, for now
	}

	@Override
	public void cancelOffers() {
		// AI doesn't respond to offers right now - possible future addition
	}

	@Override
	public double getPayoutMultiplier() {
		AIDefinition aiDef = CheckersPlugin.getInstance().getAIFactory().getAIDefinition(getName());
		if (aiDef == null) {
			LogUtils.warning("can't find AI definition for " + getName());
			return 2.0;
		} else {
			return 1.0 + aiDef.getPayoutMultiplier();
		}
	}

	@Override
	public void drawOffered() {
		ai.offerDraw();
	}

	@Override
	public void swapOffered() {
		// do nothing here
	}

	@Override
	public void undoOffered() {
		ai.offerUndo();
	}

	@Override
	public void undoLastMove() {
		ai.setActive(false);
		ai.undoLastMove();
	}

	@Override
	public void checkPendingAction() {
		CheckersGame game = getGame();
		CheckersPlayer otherPlayer = game.getPlayer(getColour().getOtherColour());

		if (ai.hasFailed()) {
			// this will happen if the AI caught an exception and its state can't be guaranteed anymore
			try {
				if (CheckersPlugin.getInstance().getConfig().getBoolean("ai.lose_on_fail", false)) {
					game.forfeit(getName());
				} else {
					game.drawn(GameResult.ABANDONED);
				}
			} catch (CheckersException e) {
				// should never get here
				LogUtils.severe("Unexpected exception caught while trying to draw game - deleted", e);
				game.deletePermanently();
			}
		} else {
			// see if the AI has any pending actions from the other thread that we need to pick up
			switch (ai.getPendingAction()) {
				case MOVED:
					RowCol from = ai.getPendingFrom();
					RowCol to = ai.getPendingTo();
					try {
						PlayerColour toMove = game.getPosition().getToMove();
						game.doMove(getName(), from, to);
						if (game.getState() != GameState.FINISHED) {
							if (toMove == game.getPosition().getToMove()) {
								// still this player to move - must be a chained jump
								this.promptForContinuedMove();
							} else {
								game.getPlayerToMove().promptForNextMove();
							}
						}
					} catch (IllegalMoveException e) {
						getGame().alert(Messages.getString("AI.AIunexpectedException", e.getMessage()));
						ai.setFailed(true);
					} catch (CheckersException e) {
						getGame().alert(Messages.getString("AI.AIunexpectedException", e.getMessage()));
						ai.setFailed(true);
					}
					break;
				case DRAW_OFFERED:
					game.offerDraw(getName());
					break;
				case DRAW_ACCEPTED:
					if (otherPlayer != null) {
						otherPlayer.alert(Messages.getString("Offers.drawOfferAccepted", getName()));
					}
					game.drawn(GameResult.DRAW_AGREED);
					break;
				case DRAW_DECLINED:
					if (otherPlayer != null) {
						otherPlayer.alert(Messages.getString("Offers.drawOfferDeclined", getName()));
					}
					break;
				case UNDO_ACCEPTED:
					if (otherPlayer != null) {
						otherPlayer.alert(Messages.getString("Offers.undoOfferAccepted", getName()));
					}
					game.undoMove(otherPlayer.getName());
					break;
				case UNDO_DECLINED:
					if (otherPlayer != null) {
						otherPlayer.alert(Messages.getString("Offers.undoOfferDeclined", getName()));
					}
					break;
				default:
					break;
			}
			ai.clearPendingAction();
		}
	}

	@Override
	public void playEffect(String effect) {
		// do nothing
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public void teleport(Location loc) {
		// do nothing
	}

	@Override
	public void teleport(BoardView bv) {
		// do nothing
	}

	@Override
	public void timeControlCheck() {
		ai.notifyTimeControl(getGame().getClock());
	}
}
