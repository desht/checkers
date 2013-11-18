package me.desht.checkers.model;

import java.util.ArrayList;
import java.util.List;

public class EnglishDraughts extends GameRules {
	public EnglishDraughts(Position position) {
		super("englishdraughts", position);
	}

	@Override
	public int getSize() {
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
	public boolean canMove(PlayerColour who, int fromRow, int fromCol, MoveDirection direction) {
		int toRow = fromRow + direction.getRowOffset();
		int toCol = fromCol + direction.getColOffset();
		if (toRow < 0 || toRow >= getSize() || toCol < 0 || toCol >= getSize()) {
			return false;
		}
		PieceType moving = getPosition().getPieceAt(fromRow, fromCol);
		PieceType target = getPosition().getPieceAt(toRow, toCol);
		if (target != PieceType.NONE) {
			return false;
		}
		if (moving == PieceType.WHITE && toRow > fromRow) {
			return false;
		}
		if (moving == PieceType.BLACK && toRow < fromRow) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canJump(PlayerColour who, int fromRow, int fromCol, MoveDirection direction) {
		int overRow = fromRow + direction.getRowOffset();
		int overCol = fromCol + direction.getColOffset();
		int toRow = fromRow + direction.getRowOffset(2);
		int toCol = fromCol + direction.getColOffset(2);
		if (toRow < 0 || toRow >= getSize() || toCol < 0 || toCol >= getSize()) {
			return false;
		}
		PieceType moving = getPosition().getPieceAt(fromRow, fromCol);
		PieceType victim = getPosition().getPieceAt(overRow, overCol);
		PieceType target = getPosition().getPieceAt(toRow, toCol);
		if (target != PieceType.NONE) {
			return false;
		}
		if (moving == PieceType.WHITE && toRow > fromRow) {
			return false;
		}
		if (moving == PieceType.BLACK && toRow < fromRow) {
			return false;
		}
		if (victim.getColour() != moving.getColour().getOtherColour()) {
			return false;
		}
		return true;
	}
}
