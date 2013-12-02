package me.desht.checkers.model;

import java.util.ArrayList;
import java.util.List;

public class EnglishDraughts extends GameRules {
	public EnglishDraughts() {
	}

	@Override
	public int getBoardSize() {
		return 8;
	}

	@Override
	public int getPieceRowCount() {
		return 3;
	}

	@Override
	public PlayerColour getWhoMovesFirst() {
		return PlayerColour.BLACK;
	}

	@Override
	public boolean isForcedJump() {
		return true;
	}

	@Override
	public boolean allowChainedJumpPromotion() {
		return true;
	}

	@Override
	public List<Move> getMoves(Position position, PlayerColour who, RowCol from, MoveDirection direction) {
		RowCol to = from.add(direction);
		if (!isValidSquare(to) || position.getPieceAt(to) != PieceType.NONE) {
			return null;
		}
		PieceType moving = position.getPieceAt(from);
		if (moving == PieceType.WHITE && to.getRow() > from.getRow() || moving == PieceType.BLACK && to.getRow() < from.getRow()) {
			return null;
		}
		List<Move> res = new ArrayList<Move>(1);
		res.add(new Move(from, to));
		return res;
	}

	@Override
	public List<Move> getJumps(Position position, PlayerColour who, RowCol from, MoveDirection direction) {
		RowCol to = from.add(direction, 2);
		if (!isValidSquare(to) || position.getPieceAt(to) != PieceType.NONE) {
			return null;
		}
		PieceType moving = position.getPieceAt(from);
		if (moving == PieceType.WHITE && to.getRow() > from.getRow() || moving == PieceType.BLACK && to.getRow() < from.getRow()) {
			return null;
		}
		RowCol over = from.add(direction);
		if (position.getPieceAt(over).getColour() != moving.getColour().getOtherColour() || position.isMarkedCaptured(over)) {
			return null;
		}
		List<Move> res = new ArrayList<Move>(1);
		res.add(new Move(from, to));
		return res;
	}
}
