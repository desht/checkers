package me.desht.checkers.model;

public enum CheckersPlayer {
	NONE, BLACK, WHITE;

	public CheckersPlayer getOtherColour() {
		switch (this) {
		case WHITE: return BLACK;
		case BLACK: return WHITE;
		default: throw new IllegalArgumentException("unexpected colour");
		}
	}
}
