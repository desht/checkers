package me.desht.checkers.model;

import me.desht.checkers.util.CheckersUtils;

import org.apache.commons.lang.Validate;

public class Move {
	private final int fromRow, fromCol;
	private final int toRow, toCol;

	public Move(int fromRow, int fromCol, int toRow, int toCol) {
		int rowOff = Math.abs(fromRow - toRow);
		int colOff = Math.abs(fromRow - toRow);
		Validate.isTrue(rowOff > 0 && rowOff < 3, "invalid row offset");
		Validate.isTrue(colOff > 0 && colOff < 3, "invalid column offset");
		Validate.isTrue(colOff == rowOff, "invalid row/column offset");

		this.fromRow = fromRow;
		this.fromCol = fromCol;
		this.toRow = toRow;
		this.toCol = toCol;
	}

	public Move(int encoded) {
		this(encoded & 0x7, (encoded >> 3) & 0x7, (encoded >> 6) & 0x7, (encoded >> 9) & 0x7);
	}

	public boolean isJump() {
		return Math.abs(fromRow - toRow) == 2 && Math.abs(fromCol - toCol) == 2;
	}

	/**
	 * @return the fromRow
	 */
	public int getFromRow() {
		return fromRow;
	}

	/**
	 * @return the fromCol
	 */
	public int getFromCol() {
		return fromCol;
	}

	/**
	 * @return the toRow
	 */
	public int getToRow() {
		return toRow;
	}

	/**
	 * @return the toCol
	 */
	public int getToCol() {
		return toCol;
	}

	public int encode() {
		return fromRow + (fromCol << 3) + (toRow << 6) + (toCol << 9);
	}

	@Override
	public String toString() {
		return CheckersUtils.rowColToString(fromRow, fromCol) + "-" + CheckersUtils.rowColToString(toRow, toCol);
	}
}
