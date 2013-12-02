package me.desht.checkers.model.rules;

import me.desht.checkers.model.*;

import java.util.ArrayList;
import java.util.List;

public class InternationalDraughts extends GameRules {
	public InternationalDraughts() {
	}

	@Override
	public int getBoardSize() {
		return 10;
	}

	@Override
	public int getPieceRowCount() {
		return 4;
	}

	@Override
	public PlayerColour getWhoMovesFirst() {
		return PlayerColour.WHITE;
	}

	@Override
	public boolean isForcedJump() {
		return true;
	}

	@Override
	public boolean allowChainedJumpPromotion() {
		return false;
	}

	@Override
	public List<Move> getMoves(Position position, PlayerColour who, RowCol from, MoveDirection direction) {
		RowCol to = from.add(direction);
		if (!isValidSquare(to) || position.getPieceAt(to) != PieceType.NONE) {
			return null;
		}
		PieceType moving = position.getPieceAt(from);
		if (moving == PieceType.WHITE && to.getRow() < from.getRow()) {
			return null;
		}
		if (moving == PieceType.BLACK && to.getRow() > from.getRow()) {
			return null;
		}
		List<Move> res = new ArrayList<Move>(1);
		res.add(new Move(from, from.add(direction)));
		return res;
	}

	@Override
	public List<Move> getJumps(Position position, PlayerColour who, RowCol from, MoveDirection direction) {
		RowCol over = from.add(direction);
		if (!isValidSquare(over)) {
			return null;
		}
		List<Move> res = new ArrayList<Move>();
		PieceType moving = position.getPieceAt(from);
		if (moving.isKing()) {
			// flying kings - move and capture any distance
			while (true) {
				if (over.getRow() <= 0 || over.getCol() <= 0 || over.getRow() >= getBoardSize() - 1 || over.getCol() >= getBoardSize() - 1) {
					return null;  // reached the edge of the board; no possible jump here
				} else if (position.getPieceAt(over).getColour() == who) {
					return null;  // one of our own pieces detected; no possible jump
				} else if (position.isMarkedCaptured(over)) {
					return null;  // can't capture a piece twice!
				} else if (position.getPieceAt(over).getColour() == who.getOtherColour()) {
					break;  // found a piece of the opposing colour
				}
				over = over.add(direction);
			}
			// at this point, we know there's an candidate for capture - see if there's space behind it to jump into
			over = over.add(direction);
			while (isValidSquare(over) && position.getPieceAt(over).getColour() == PlayerColour.NONE) {
				res.add(new Move(from, over));
				over = over.add(direction);
			}
		} else {
			// an ordinary piece may jump one square, forwards or backwards
			RowCol to = from.add(direction, 2);
			if (!isValidSquare(to) || position.getPieceAt(to) != PieceType.NONE) {
				return null;
			}
			if (position.getPieceAt(over).getColour() != moving.getColour().getOtherColour() || position.isMarkedCaptured(over)) {
				return null;
			}
			res.add(new Move(from, from.add(direction, 2)));
		}

		return res;
	}
}
