package me.desht.checkers.model;

public enum PieceType {
	NONE, WHITE, WHITE_KING, BLACK, BLACK_KING;

	public PlayerColour getColour() {
		switch (this) {
		case WHITE: case WHITE_KING: return PlayerColour.WHITE;
		case BLACK: case BLACK_KING: return PlayerColour.BLACK;
		default: case NONE: return PlayerColour.NONE;
		}
	}

	public boolean isKing() {
		switch (this) {
		case WHITE_KING: case BLACK_KING: return true;
		default: return false;
		}
	}

	public PieceType toKing() {
		switch (this) {
		case WHITE: return WHITE_KING;
		case BLACK: return BLACK_KING;
		default: return this;
		}
	}

	public static PieceType decode(int enc) {
		switch (enc) {
		case 0: return PieceType.WHITE;
		case 1: return PieceType.BLACK;
		case 2: return PieceType.WHITE_KING;
		case 3: return PieceType.BLACK_KING;
		default: return PieceType.NONE;
		}
	}

	public int encode() {
		int ret = 0;
		if (getColour() == PlayerColour.BLACK) ret |= 0x1;
		if (isKing()) ret |= 0x2;
		return ret;
	}
}
