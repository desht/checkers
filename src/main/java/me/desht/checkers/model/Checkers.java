package me.desht.checkers.model;

public class Checkers {
	public static final int NO_SQUARE = -1;

	private static int[] cnToSqi = new int[32];

	static {
		for (int row = 0; row < 7; row++) {
			for (int col = 0; col < 7; col++) {
				if (row % 2 == col % 2) {
					int cn = rowColToCheckersNotation(row, col);
					cnToSqi[cn - 1] = rowColToSqi(row, col);
				}
			}
		}
	}

	public static int rowColToSqi(int row, int col) {
		return (row << 3) | col;
	}

	public static int sqiToRow(int sqi) {
		return (sqi >> 3) & 0x07;
	}

	public static int sqiToCol(int sqi) {
		return sqi & 0x07;
	}

	public static int sqiToCheckersNotation(int sqi) {
		return rowColToCheckersNotation(sqiToRow(sqi), sqiToCol(sqi));
	}

	public static int rowColToCheckersNotation(int row, int col) {
		return row * 4 + (4 - (col / 2));
	}

	public static int checkersNotationToSqi(int cn) {
		System.out.println(cn + " -> " + cnToSqi[cn - 1]);
		return cnToSqi[cn - 1];
	}
}
