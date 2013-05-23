package me.desht.checkers.ai.evaluation;

import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.Position;

public class PositionWeightedEvaluator implements Evaluator {

	private static final int EDGE_PENALTY = 10;
	private static final int KING_SCORE = 200;
	private static final int PIECE_SCORE = 100;

	@Override
	public int evaluate(Position position, PlayerColour colour) {
		int score = 0;

		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				switch (position.getPieceAt(row, col)) {
				case WHITE:
					score += PIECE_SCORE + Math.pow(7 - row, 2);
					break;
				case WHITE_KING:
					score += KING_SCORE;
					if (row == 0 || row == 7 || col == 0 || col == 7) {
						score -= EDGE_PENALTY;
					}
					break;
				case BLACK:
					score -= PIECE_SCORE + Math.pow(row, 2);
					break;
				case BLACK_KING:
					score -= KING_SCORE;
					if (row == 0 || row == 7 || col == 0 || col == 7) {
						score += EDGE_PENALTY;
					}
					break;
				default:
					break;
				}
			}
		}
		return colour == PlayerColour.WHITE ? score : -score;
	}

}
