package me.desht.checkers.model;

public interface Position {
	public PieceType getPieceAt(int row, int col);
	public Move[] getLegalMoves();
	public Move[] getLegalMoves(int row, int col);
	public void makeMove(Move moves);
	public Move[] getMoveHistory();
	public Move getLastMove();
	public PlayerColour getToMove();
	public void newGame();
	public boolean isJumpInProgress();
	public void addPositionListener(PositionListener listener);
	public void undoLastMove();
	public int getPlyCount();
}
