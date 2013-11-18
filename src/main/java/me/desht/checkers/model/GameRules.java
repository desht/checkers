package me.desht.checkers.model;

import java.util.ArrayList;
import java.util.List;

public abstract class GameRules {
	private final String name;
	private final Position position;

	public GameRules(String name, Position position) {
		this.name = name;
		this.position = position;
	}

	protected Position getPosition() {
		return position;
	}

	/**
	 * Get the number of squares on a board edge.
	 *
	 * @return the number of squares on an edge
	 */
	public abstract int getSize();

	/**
	 * Get the number of rows of pieces each player has at the start of the game.
	 *
	 * @return the number of rows of pieces
	 */
	public abstract int getPieceRowCount();

	/**
	 * Get who moves first.
	 *
	 * @return the colour of the player to move first
	 */
	public abstract PlayerColour getWhoMovesFirst();

	/**
	 * Check if this ruleset enforces jumps when they are possible.
	 *
	 * @return true if jump moves are enforced, false otherwise
	 */
	public abstract boolean isForcedJump();

	/**
	 * Check if the given player colour can move from the given board square in the given direction
	 *
	 * @param who the player colour to check
	 * @param row the board row index
	 * @param col the board column index
	 * @param direction the direction to move
	 * @return true if the move is legal, false otherwise
	 */
	public abstract boolean canMove(PlayerColour who, int row, int col, MoveDirection direction);

	/**
	 * Check if the given player colour can jump from the given board square in the given direction
	 *
	 * @param who the player colour to check
	 * @param row the board row index
	 * @param col the board column index
	 * @param direction the direction to move
	 * @return true if the jump is legal, false otherwise
	 */
	public abstract boolean canJump(PlayerColour who, int row, int col, MoveDirection direction);

	/**
	 * Calculate all possible legal moves for the given player colour
	 *
	 * @param who the colour to calculate for
	 * @return an array of legal moves
	 */
	public Move[] calculateLegalMoves(PlayerColour who) {
		List<Move> moves = new ArrayList<Move>();

		// get all the possible jumps that can be made
		for (int row = 0; row < getSize(); row++) {
			for (int col = 0; col < getSize(); col++) {
				if (getPosition().getPieceAt(row, col).getColour() == who) {
					for (MoveDirection dir : MoveDirection.values()) {
						if (canJump(who, row, col, dir)) {
							moves.add(new Move(row, col, row + dir.getRowOffset() * 2, col + dir.getColOffset() * 2));
						}
					}
				}
			}
		}

		// if there are any jumps, the player *must* jump, so don't calculate any non-jump moves
		if (moves.isEmpty() || !isForcedJump()) {
			for (int row = 0; row < getSize(); row++) {
				for (int col = 0; col < getSize(); col++) {
					if (getPosition().getPieceAt(row, col).getColour() == who) {
						for (MoveDirection dir : MoveDirection.values()) {
							if (canMove(who, row, col, dir)) {
								moves.add(new Move(row, col, row + dir.getRowOffset(), col + dir.getColOffset()));
							}
						}
					}
				}
			}
		}

		return moves.toArray(new Move[moves.size()]);
	}

	/**
	 * Get a list of the legal moves that can be made from the given square.
	 *
	 * @param row the board row index
	 * @param col the board column index
	 * @param onlyJumps true if only jump moves should be returned
	 * @return a list of the legal moves
	 */
	public Move[] getLegalMoves(int row, int col, boolean onlyJumps) {
		if (getPosition().getPieceAt(row, col).getColour() != getPosition().getToMove()) {
			return new Move[0];
		}
		List<Move> moves = new ArrayList<Move>();
		for (MoveDirection dir : MoveDirection.values()) {
			if (canJump(getPosition().getToMove(), row, col, dir)) {
				moves.add(new Move(row, col, row + dir.getRowOffset(), col + dir.getColOffset()));
			}
		}
		if (!onlyJumps && (moves.isEmpty() || !isForcedJump())) {
			for (MoveDirection dir : MoveDirection.values()) {
				if (canMove(getPosition().getToMove(), row, col, dir)) {
					moves.add(new Move(row, col, row + dir.getRowOffset(), col + dir.getColOffset()));
				}
			}
		}
		return moves.toArray(new Move[moves.size()]);
	}
}
