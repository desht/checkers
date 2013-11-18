package me.desht.checkers.model;

public interface PositionListener {
	public void moveMade(Position position, Move move);
	public void squareChanged(RowCol square, PieceType piece);
	public void plyCountChanged(int plyCount);
	public void toMoveChanged(PlayerColour toMove);
	public void lastMoveUndone(Position position);
	public void halfMoveClockChanged(int halfMoveClock);
}
