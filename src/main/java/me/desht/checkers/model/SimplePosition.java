package me.desht.checkers.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.desht.checkers.CheckersException;
import me.desht.checkers.IllegalMoveException;
import me.desht.dhutils.LogUtils;

import com.google.common.base.Joiner;

/**
 * Represents a checkers board position.
 *
 * TODO: Position and game (move history etc.) are currently all stored in this object.
 * It mnight be better design to move game details into a separate object which contains
 * a Position object.
 */
public class SimplePosition implements Position {
	private final List<PositionListener> listeners = new ArrayList<PositionListener>();
	private final PieceType[][] board;
	private final GameRules rules;

	private PlayerColour toMove;

	private Move[] legalMoves;
	private boolean jumpInProgress;
	private List<Move> moveHistory;
	private int halfMoveClock; // moves since a capture was made
	private boolean forcedJump = true;

	public SimplePosition(Class <? extends GameRules> ruleClass) {
		try {
			Constructor<? extends GameRules> ctor = ruleClass.getDeclaredConstructor(Position.class);
			rules = ctor.newInstance(this);
			board = new PieceType[getSize()][getSize()];
			newGame();
		} catch (Exception e) {
			throw new CheckersException("can't instantiate a position: " + e.getMessage());
		}
	}

	public SimplePosition(SimplePosition other, boolean copyHistory) {
		rules = other.rules;
		board = new PieceType[getSize()][getSize()];
		for (int row = 0; row < getSize(); row++) {
			for (int col = 0; col < getSize(); col++) {
				setPieceAt(row, col, other.getPieceAt(row, col));
			}
		}
		legalMoves = Arrays.copyOf(other.getLegalMoves(), other.getLegalMoves().length);
		toMove = other.getToMove();
		jumpInProgress = other.isJumpInProgress();
		moveHistory = new ArrayList<Move>();
		halfMoveClock = other.getHalfMoveClock();
		if (copyHistory) {
			for (Move m : other.getMoveHistory()) {
				moveHistory.add(m);
			}
		}
	}

	public int getSize() {
		return rules.getSize();
	}

	@Override
	public void addPositionListener(PositionListener listener) {
		listeners.add(listener);
		for (int row = 0; row < getSize(); row++) {
			for (int col = 0; col < getSize(); col++) {
				listener.squareChanged(row, col, getPieceAt(row, col));
			}
		}
	}

	@Override
	public void newGame() {
		for (int row = 0; row < getSize(); row++) {
			for (int col = 0; col < getSize(); col++) {
				if (row % 2 == col % 2) {
					if (row < rules.getPieceRowCount()) {
						board[row][col] = getPieceForColour(rules.getWhoMovesFirst());
					} else if (row > getSize() - 1 - rules.getPieceRowCount()) {
						board[row][col] = getPieceForColour(rules.getWhoMovesFirst().getOtherColour());
					} else {
						board[row][col] = PieceType.NONE;
					}
				} else {
					board[row][col] = PieceType.NONE;
				}
			}
		}
		toMove = PlayerColour.BLACK;
		legalMoves = rules.calculateLegalMoves(toMove);
		jumpInProgress = false;
		moveHistory = new ArrayList<Move>();
		halfMoveClock = 0;
	}

	private PieceType getPieceForColour(PlayerColour colour) {
		switch (colour) {
			case WHITE: return PieceType.WHITE;
			case BLACK: return PieceType.BLACK;
			default: return PieceType.NONE;
		}
	}

	@Override
	public PieceType getPieceAt(int row, int col) {
		return board[row][col];
	}

	@Override
	public Move[] getLegalMoves() {
		return legalMoves;
	}

	private int getPromotionRow(PieceType movingPiece) {
		switch (rules.getWhoMovesFirst()) {
			case BLACK: return movingPiece == PieceType.BLACK ? getSize() - 1 : 0;
			case WHITE: return movingPiece == PieceType.WHITE ? getSize() - 1 : 0;
			default: return -1;
		}
	}

	@Override
	public void makeMove(Move move) {
		Move legalMove = null;
		for (Move m : getLegalMoves()) {
			if (m.equals(move)) {
				legalMove = m;
				break;
			}
		}
		if (legalMove == null) {
			throw new IllegalMoveException();
		}

		int fromRow = move.getFromRow();
		int fromCol = move.getFromCol();
		int toRow = move.getToRow();
		int toCol = move.getToCol();

		PieceType movingPiece = getPieceAt(fromRow, fromCol);
		move.setMovedPiece(movingPiece);
		setPieceAt(fromRow, fromCol, PieceType.NONE);

		if (move.isJump()) {
			// move is a jump - remove the intervening piece
			int overRow = (fromRow + toRow) / 2;
			int overCol = (fromCol + toCol) / 2;
			move.setCapturedPiece(getPieceAt(overRow, overCol));
			setPieceAt(overRow, overCol, PieceType.NONE);
		}

		int h = halfMoveClock;

		// check for piece promotion
		boolean justPromoted = false;
		if (toRow == getPromotionRow(movingPiece)) {
			justPromoted = true;
			halfMoveClock = 0;
			setPieceAt(toRow, toCol, movingPiece.toKing());
		} else {
			setPieceAt(toRow, toCol, movingPiece);
		}

		if (move.isJump()) {
			// check for a possible chain of jumps
			Move[] jumps = rules.getLegalMoves(toRow, toCol, true);
			if (jumps.length > 0 && !justPromoted) {
				// the same player must continue jumping
				jumpInProgress = true;
				move.setChainedJump(true);
				legalMoves = jumps;
			} else {
				toMove = toMove.getOtherColour();
				legalMoves = rules.calculateLegalMoves(toMove);
				jumpInProgress = false;
			}
			halfMoveClock = 0;
		} else {
			toMove = toMove.getOtherColour();
			legalMoves = rules.calculateLegalMoves(toMove);
			jumpInProgress = false;
			halfMoveClock++;
		}

		LogUtils.fine("move made by " + toMove.getOtherColour() + ", legal moves now: " + Joiner.on(",").join(legalMoves));
		moveHistory.add(move);

		for (PositionListener l : listeners) {
			l.moveMade(this, move);
			if (h != halfMoveClock) {
				l.halfMoveClockChanged(halfMoveClock);
			}
			if (!move.isChainedJump()) {
				l.plyCountChanged(getPlyCount());
				l.toMoveChanged(toMove);
			}
		}
	}

	@Override
	public Position tryMove(Move move) {
		Position newPos = new SimplePosition(this, false);
		newPos.makeMove(move);
		return newPos;
	}

	@Override
	public GameRules getRules() {
		return rules;
	}

	@Override
	public PlayerColour getToMove() {
		return toMove;
	}

	public int getHalfMoveClock() {
		return halfMoveClock;
	}

	@Override
	public boolean isJumpInProgress() {
		return jumpInProgress;
	}

	@Override
	public Move[] getMoveHistory() {
		return moveHistory.toArray(new Move[moveHistory.size()]);
	}

	@Override
	public Move getLastMove() {
		return moveHistory.get(moveHistory.size() - 1);
	}

	@Override
	public void undoLastMove() {
		if (moveHistory.size() == 0) {
			return;
		}
		int idx = moveHistory.size() - 1;
		do {
			Move move = moveHistory.get(idx);
			setPieceAt(move.getToRow(), move.getToCol(), PieceType.NONE);
			setPieceAt(move.getFromRow(), move.getFromCol(), move.getMovedPiece());
			if (move.isJump()) {
				int overRow = (move.getFromRow() + move.getToRow()) / 2;
				int overCol = (move.getFromCol() + move.getToCol()) / 2;
				setPieceAt(overRow, overCol, move.getCapturedPiece());
			}
			idx--;
		} while (idx >= 0 && moveHistory.get(idx).isChainedJump());

		toMove = toMove.getOtherColour();
		legalMoves = rules.calculateLegalMoves(toMove);
		jumpInProgress = false;

		// truncate the move history
		List<Move> tmpHist = new ArrayList<Move>(idx + 1);
		for (int i = 0; i <= idx; i++) {
			tmpHist.add(moveHistory.get(i));
		}
		moveHistory = tmpHist;

		for (PositionListener l : listeners) {
			l.lastMoveUndone(this);
			l.plyCountChanged(getPlyCount());
			l.toMoveChanged(toMove);
		}
	}

	@Override
	public int getPlyCount() {
		int count = 0;
		// need to account for chained jumps
		for (Move m : moveHistory) {
			if (!m.isChainedJump()) {
				count++;
			}
		}
		return count;
	}

	private void setPieceAt(int row, int col, PieceType piece) {
		board[row][col] = piece;
		for (PositionListener l : listeners) {
			l.squareChanged(row, col, piece);
		}
	}
}
