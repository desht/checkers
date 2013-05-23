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

	public Standard(String name, CheckersGame checkersGame, PlayerColour aiColour, ConfigurationSection params) {
		super(name, checkersGame, aiColour, params);

		evaluator = new PositionWeightedEvaluator();
		maxDepth = params.getInt("depth");

		setReady();
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		Move selectedMove = null;

		// TODO: we should sort the moves to optimise the alphabeta search

		int best = WORST - 1;
		for (Move m : getCheckersGame().getPosition().getLegalMoves()) {
			Position pos = getCheckersGame().getPosition().tryMove(m);
			System.out.println("after move " + m + ", to move = " + pos.getToMove() + " pos value = " + evaluator.evaluate(pos, pos.getToMove()));
			int val = -alphaBetaSearch(1, pos, WORST, -best);
			System.out.println("move " + m + ", value " + val);
			if (val > best || val == best && new Random().nextBoolean()) {
				best = val;
				selectedMove = m;
			}
			System.out.println("---");
		}

		if (selectedMove != null) {
			aiHasMoved(selectedMove.getFromSqi(), selectedMove.getToSqi());
		} else {
			aiHasFailed(new CheckersException("Failed to find a move"));
		}
	}

	@Override
	public void undoLastMove() {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyTimeControl(TimeControl timeControl) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void movePiece(int fromSqi, int toSqi, boolean otherPlayer) {
		// TODO Auto-generated method stub

	}

	private int alphaBetaSearch(int depth, Position pos, int alpha, int beta) {
		assert alpha <= beta;

		System.out.println("alphabeta d=" + depth + " tomove=" + pos.getToMove() + " alpha=" + alpha + " beta=" + beta);
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
			int value = -alphaBetaSearch(depth + 1, pos.tryMove(move), -beta, -alpha1);
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
