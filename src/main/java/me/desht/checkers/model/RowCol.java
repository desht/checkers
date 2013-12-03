package me.desht.checkers.model;

public class RowCol {
	private static final int MAX_BOARD_DIMENSION = 12;
	private static final RowCol[][] rc = new RowCol[MAX_BOARD_DIMENSION][MAX_BOARD_DIMENSION];

	private final int row, col;

	static {
		for (int row = 0; row < MAX_BOARD_DIMENSION; row++) {
			for (int col = 0; col < MAX_BOARD_DIMENSION; col++) {
				rc[row][col] = new RowCol(row, col);
			}
		}
	}

	public static RowCol get(int row, int col) {
		if (row < 0 || row >= MAX_BOARD_DIMENSION || col < 0 || col >= MAX_BOARD_DIMENSION) {
			return new RowCol(row, col);
		} else {
			return rc[row][col];
		}
	}

	private RowCol(int row, int col) {
		this.row = row;
		this.col = col;
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	public RowCol add(MoveDirection dir) {
		return RowCol.get(row + dir.getRowOffset(), col + dir.getColOffset());
	}

	public RowCol add(MoveDirection dir, int distance) {
		return RowCol.get(row + dir.getRowOffset() * distance, col + dir.getColOffset() * distance);
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
