package me.desht.checkers.model;

public class Checkers {
	public static final RowCol NO_SQUARE = null;

	public static RowCol checkersNotationToSquare(int cn, int size) {
		int n = cn * 2 - 1;
		int row = size - 1 - (n / size);
		int col = ((cn - 1) % (size / 2)) * 2;
		if (row % 2 != 0) {
			col++;
		}
		return RowCol.get(row, col);
	}
}
