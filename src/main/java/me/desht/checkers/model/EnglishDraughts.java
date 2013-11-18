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
	public boolean canMove(PlayerColour who, RowCol from, MoveDirection direction) {
		int toRow = from.getRow() + direction.getRowOffset();
		int toCol = from.getCol() + direction.getColOffset();
		if (toRow < 0 || toRow >= getSize() || toCol < 0 || toCol >= getSize()) {
			return false;
		}
		PieceType moving = getPosition().getPieceAt(from);
		PieceType target = getPosition().getPieceAt(toRow, toCol);
		if (target != PieceType.NONE) {
			return false;
		}
		if (moving == PieceType.WHITE && toRow > from.getRow()) {
			return false;
		}
		if (moving == PieceType.BLACK && toRow < from.getRow()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canJump(PlayerColour who, RowCol from, MoveDirection direction) {
		int overRow = from.getRow() + direction.getRowOffset();
		int overCol = from.getCol() + direction.getColOffset();
		int toRow = from.getRow() + direction.getRowOffset(2);
		int toCol = from.getCol() + direction.getColOffset(2);
		if (toRow < 0 || toRow >= getSize() || toCol < 0 || toCol >= getSize()) {
			return false;
		}
		PieceType moving = getPosition().getPieceAt(from);
		PieceType victim = getPosition().getPieceAt(overRow, overCol);
		PieceType target = getPosition().getPieceAt(toRow, toCol);
		if (target != PieceType.NONE) {
			return false;
		}
		if (moving == PieceType.WHITE && toRow > from.getRow()) {
			return false;
		}
		if (moving == PieceType.BLACK && toRow < from.getRow()) {
			return false;
		}
		if (victim.getColour() != moving.getColour().getOtherColour()) {
			return false;
		}
		return true;
	}
}
