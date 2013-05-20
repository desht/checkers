package me.desht.checkers.model;

import me.desht.checkers.CheckersValidate;

public class Move {
	private final int fromRow, fromCol;
	private final int toRow, toCol;

	public Move(int fromRow, int fromCol, int toRow, int toCol) {
		int rowOff = Math.abs(fromRow - toRow);
		int colOff = Math.abs(fromRow - toRow);
		CheckersValidate.isTrue(rowOff > 0 && rowOff < 3, "invalid row offset");
		CheckersValidate.isTrue(colOff > 0 && colOff < 3, "invalid column offset");
		CheckersValidate.isTrue(colOff == rowOff, "invalid row/column offset");

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
		return Checkers.rowColToString(fromRow, fromCol) + "-" + Checkers.rowColToString(toRow, toCol);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fromCol;
		result = prime * result + fromRow;
		result = prime * result + toCol;
		result = prime * result + toRow;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Move other = (Move) obj;
		if (fromCol != other.fromCol)
			return false;
		if (fromRow != other.fromRow)
			return false;
		if (toCol != other.toCol)
			return false;
		if (toRow != other.toRow)
			return false;
		return true;
	}
}
