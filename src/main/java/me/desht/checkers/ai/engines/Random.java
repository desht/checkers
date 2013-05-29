package me.desht.checkers.ai.engines;

import me.desht.checkers.TwoPlayerClock;
import me.desht.checkers.ai.CheckersAI;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.Move;
import me.desht.checkers.model.PlayerColour;

import org.bukkit.configuration.ConfigurationSection;

public class Random extends CheckersAI {

	private final java.util.Random rnd;

	public Random(String name, CheckersGame checkersGame, PlayerColour aiColour, ConfigurationSection params) {
		super(name, checkersGame, aiColour, params);
		rnd = new java.util.Random();
		setReady();
	}

	@Override
	public void shutdown() {
		// nothing to do here - no internal model
	}

	@Override
	public void run() {
		// just pick a random move from the list of legal moves
		Move[] moves = getCheckersGame().getPosition().getLegalMoves();
		Move picked = moves[rnd.nextInt(moves.length)];
		aiHasMoved(picked.getFromSqi(), picked.getToSqi());
	}

	@Override
	public void undoLastMove() {
		// nothing to do here - no internal model
	}

	@Override
	public void notifyTimeControl(TwoPlayerClock clock) {
		// don't care about time controls
	}

	@Override
	protected void movePiece(int fromSqi, int toSqi, boolean otherPlayer) {
		// nothing to do here - no internal model
	}

}
