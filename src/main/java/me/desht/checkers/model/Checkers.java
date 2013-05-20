package me.desht.checkers.model;

public class Checkers {
	public static final int NO_SQUARE = -1;
	private static final String rows = "ABCDEFGH";
	private static final String cols = "12345678";

	public static int rowColToSqi(int row, int col) {
		return (row << 3) | col;
	}

	public static int sqiToRow(int sqi) {
		return (sqi >> 3) & 0x07;
	}

	public static int sqiToCol(int sqi) {
		return sqi & 0x07;
	}

	public static String sqiToString(int sqi) {
		return rowColToString(sqiToRow(sqi), sqiToCol(sqi));
	}

	public static String rowColToString(int row, int col) {
		return rows.substring(row, row + 1) + cols.substring(col, col + 1);
	}
}
