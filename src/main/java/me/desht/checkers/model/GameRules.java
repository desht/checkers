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

	public String getName() {
		return name;
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
	 * @param square the board square
	 * @param direction the direction to move
	 * @return true if the move is legal, false otherwise
	 */
	public abstract boolean canMove(PlayerColour who, RowCol square, MoveDirection direction);

	/**
	 * Check if the given player colour can jump from the given board square in the given direction
	 *
	 * @param who the player colour to check
	 * @param square the board square
	 * @param direction the direction to move
	 * @return true if the jump is legal, false otherwise
	 */
	public abstract boolean canJump(PlayerColour who, RowCol square, MoveDirection direction);

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
				RowCol square = new RowCol(row, col);
				if (getPosition().getPieceAt(square).getColour() == who) {
					for (MoveDirection dir : MoveDirection.values()) {
						if (canJump(who, square, dir)) {
							RowCol square2 = new RowCol(row + dir.getRowOffset() * 2, col + dir.getColOffset() * 2);
							moves.add(new Move(square, square2));
						}
					}
				}
			}
		}

		// if there are any jumps, the player *must* jump, so don't calculate any non-jump moves
		if (moves.isEmpty() || !isForcedJump()) {
			for (int row = 0; row < getSize(); row++) {
				for (int col = 0; col < getSize(); col++) {
					RowCol square = new RowCol(row, col);
					if (getPosition().getPieceAt(square).getColour() == who) {
						for (MoveDirection dir : MoveDirection.values()) {
							if (canMove(who, square, dir)) {
								RowCol square2 = new RowCol(row + dir.getRowOffset(), col + dir.getColOffset());
								moves.add(new Move(square, square2));
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
	 * @param square the board square
	 * @param onlyJumps true if only jump moves should be returned
	 * @return a list of the legal moves
	 */
	public Move[] getLegalMoves(RowCol square, boolean onlyJumps) {
		if (getPosition().getPieceAt(square).getColour() != getPosition().getToMove()) {
			return new Move[0];
		}
		List<Move> moves = new ArrayList<Move>();
		for (MoveDirection dir : MoveDirection.values()) {
			if (canJump(getPosition().getToMove(), square, dir)) {
				RowCol square2 = new RowCol(square.getRow() + dir.getRowOffset() * 2, square.getCol() + dir.getColOffset() * 2);
				moves.add(new Move(square, square2));
			}
		}
		if (!onlyJumps && (moves.isEmpty() || !isForcedJump())) {
			for (MoveDirection dir : MoveDirection.values()) {
				if (canMove(getPosition().getToMove(), square, dir)) {
					RowCol square2 = new RowCol(square.getRow() + dir.getRowOffset(), square.getCol() + dir.getColOffset());
					moves.add(new Move(square, square2));
				}
			}
		}
		return moves.toArray(new Move[moves.size()]);
	}
}
