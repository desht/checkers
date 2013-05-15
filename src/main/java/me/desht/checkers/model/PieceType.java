package me.desht.checkers.model;

public enum PieceType {
	NONE, WHITE, WHITE_KING, BLACK, BLACK_KING;

	public CheckersPlayer getColour() {
		switch (this) {
		case WHITE: case WHITE_KING: return CheckersPlayer.WHITE;
		case BLACK: case BLACK_KING: return CheckersPlayer.BLACK;
		default: case NONE: return CheckersPlayer.NONE;
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
}
