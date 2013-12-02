package me.desht.checkers.model;

import me.desht.checkers.model.rules.GameRules;

public interface Position {
	public PieceType getPieceAt(int row, int col);
	public PieceType getPieceAt(RowCol square);
	public Move[] getLegalMoves();
	public void makeMove(Move moves);
	public Move[] getMoveHistory();
	public Move getLastMove();
	public PlayerColour getToMove();
	public void newGame();
	public boolean isJumpInProgress();
	public void addPositionListener(PositionListener listener);
	public void undoLastMove();
	public int getPlyCount();
	public int getHalfMoveClock();
	public Position tryMove(Move move);
	public GameRules getRules();
	public void setRules(String ruleId);

	boolean isMarkedCaptured(RowCol square);
}
