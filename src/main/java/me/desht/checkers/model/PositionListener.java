package me.desht.checkers.model;

public interface PositionListener {
	public void moveMade(Position position, Move move);
	public void squareChanged(int row, int col, PieceType piece);
}
