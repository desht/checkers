package me.desht.checkers.model;

public class RowCol {
	private final int row, col;

	public RowCol(int row, int col) {
		this.row = row;
		this.col = col;

	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	@Override
	public String toString() {
		return "(" + row + "," + col + ")";
	}

	public int toCheckersNotation(int boardSize) {
		boardSize /= 2;
		return row * boardSize + (boardSize - (col / 2));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RowCol rowCol = (RowCol) o;

		if (col != rowCol.col) return false;
		if (row != rowCol.row) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = row;
		result = 31 * result + col;
		return result;
	}
}
