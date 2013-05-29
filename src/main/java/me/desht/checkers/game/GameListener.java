package me.desht.checkers.game;

import me.desht.checkers.player.CheckersPlayer;

public interface GameListener {
	public void gameDeleted(CheckersGame game);
	public void playerAdded(CheckersGame checkersGame, CheckersPlayer checkersPlayer);
	public void gameStarted(CheckersGame checkersGame);
	public boolean tryStakeChange(double newStake);
	public void stakeChanged(double newStake);
	public boolean tryTimeControlChange(String tcSpec);
	public void timeControlChanged(String tcSpec);
}
