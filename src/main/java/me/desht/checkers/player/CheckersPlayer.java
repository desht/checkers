package me.desht.checkers.player;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.view.BoardView;

import org.bukkit.Location;

public abstract class CheckersPlayer {
	private final String name;
	private final CheckersGame game;

	private PlayerColour colour;

	protected CheckersPlayer(String name, CheckersGame game, PlayerColour colour) {
		this.name = name;
		this.game = game;
		this.colour = colour;
	}

	/**
	 * @return the colour
	 */
	public PlayerColour getColour() {
		return colour;
	}

	/**
	 * @param colour the colour to set
	 */
	public void setColour(PlayerColour colour) {
		this.colour = colour;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return name;
	}

	/**
	 * @return the game
	 */
	public CheckersGame getGame() {
		return game;
	}

	public abstract void validateAffordability(String error);
	public abstract void validateInvited(String error);

	public abstract void promptForFirstMove();
	public abstract void promptForNextMove();
	public abstract void promptForContinuedMove();

	public abstract void alert(String message);
	public abstract void statusMessage(String message);
	public abstract void playEffect(String effect);

//	public abstract void replayMoves();

	public abstract void cleanup();

	public abstract boolean isHuman();
	public abstract boolean isAvailable();

	public abstract void withdrawFunds(double amount);
	public abstract void depositFunds(double amount);

	public abstract void cancelOffers();

	public abstract double getPayoutMultiplier();

	public abstract void drawOffered();
	public abstract void swapOffered();
	public abstract void undoOffered();

	public abstract void undoLastMove();

	public abstract void checkPendingAction();

	public abstract void teleport(Location loc);
	public abstract void teleport(BoardView bv);

	public abstract void timeControlCheck();

}
