package me.desht.checkers.ai.engines;

import java.util.Random;

import me.desht.checkers.CheckersException;
import me.desht.checkers.TimeControl;
import me.desht.checkers.ai.CheckersAI;
import me.desht.checkers.ai.evaluation.Evaluator;
import me.desht.checkers.ai.evaluation.PositionWeightedEvaluator;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.Move;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.Position;

import org.bukkit.configuration.ConfigurationSection;

public class Standard extends CheckersAI {

	private static final int WORST = Integer.MIN_VALUE / 2;

	private final int maxDepth;
	private final Evaluator evaluator;

	Move selectedMove = null;

	public Standard(String name, CheckersGame checkersGame, PlayerColour aiColour, ConfigurationSection params) {
		super(name, checkersGame, aiColour, params);

		evaluator = new PositionWeightedEvaluator();
		maxDepth = params.getInt("depth");

		setReady();
	}

	@Override
	public void shutdown() {
		// nothing to do here
	}

	@Override
	public void run() {
		selectedMove = null;

		// TODO: can we pre-sort the successor moves to optimise the alphabeta search?

		int best = WORST - 1;
		for (Move m : getCheckersGame().getPosition().getLegalMoves()) {
			Position pos = getCheckersGame().getPosition().tryMove(m);
//			System.out.println("after move " + m + ", to move = " + pos.getToMove() + " pos value = " + evaluator.evaluate(pos, pos.getToMove()));
			int val = -alphaBetaSearch(pos, 1, WORST, -best);
			if (val > best || val == best && new Random().nextBoolean()) {
				best = val;
				selectedMove = m;
			}
		}

		if (selectedMove != null) {
			aiHasMoved(selectedMove.getFromSqi(), selectedMove.getToSqi());
		} else {
			aiHasFailed(new CheckersException("Failed to find a move"));
		}
	}

	@Override
	public void undoLastMove() {
		// nothing to do here
	}

	@Override
	public void notifyTimeControl(TimeControl timeControl) {
		if (timeControl.getRemainingTime() < 10000) {
			if (selectedMove != null) {
				aiHasMoved(selectedMove.getFromSqi(), selectedMove.getToSqi());
			} else {
				aiHasFailed(new CheckersException("Failed to find a move"));
			}
		}
	}

	@Override
	protected void movePiece(int fromSqi, int toSqi, boolean otherPlayer) {
		// nothing to do here
	}

	private int alphaBetaSearch(Position pos, int depth, int alpha, int beta) {
		if (alpha > beta) {
			throw new IllegalStateException("alpha > beta ???");
		}

//		System.out.println("alphabeta " + Str.repeat(' ', depth) + " d=" + depth + " tomove=" + pos.getToMove() + " alpha=" + alpha + " beta=" + beta);
		if (depth == maxDepth) {
			return evaluator.evaluate(pos, pos.getToMove());
		}

		Move[] successors = pos.getLegalMoves();
		if (successors.length == 0) {
			return evaluator.evaluate(pos, pos.getToMove());
		}

		int bestValue = WORST - 1;

		int alpha1 = alpha;
		for (Move move : successors) {
			Position nextPos = pos.tryMove(move);
			int value;
			if (nextPos.getToMove() == pos.getToMove()) {
				// chained jump
				value = alphaBetaSearch(pos.tryMove(move), depth, alpha1, beta);
			} else {
				value = -alphaBetaSearch(pos.tryMove(move), depth + 1, -beta, -alpha1);
			}
			if (value > bestValue) {
				bestValue = value;
				if (bestValue > alpha1) {
					alpha1 = bestValue;
					if (alpha1 > beta) {
						return bestValue;
					}
				}
			}
		}
		return bestValue;
	}
}
