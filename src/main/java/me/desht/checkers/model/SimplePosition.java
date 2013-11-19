package me.desht.checkers.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.desht.checkers.CheckersException;
import me.desht.checkers.IllegalMoveException;
import me.desht.dhutils.LogUtils;

import com.google.common.base.Joiner;
import org.apache.commons.lang.Validate;

/**
 * Represents a checkers board position.
 *
 * TODO: Position and game (move history etc.) are currently all stored in this object.
 * It mnight be better design to move game details into a separate object which contains
 * a Position object.
 */
public class SimplePosition implements Position {
	private final List<PositionListener> listeners = new ArrayList<PositionListener>();
	private PieceType[][] board;
	private GameRules rules;

	private PlayerColour toMove;

	private Move[] legalMoves;
	private boolean jumpInProgress;
	private List<Move> moveHistory;
	private int halfMoveClock; // moves since a capture was made
//	private boolean forcedJump = true;

	public SimplePosition(String ruleset) {
		setRules(ruleset);
	}

	public SimplePosition(SimplePosition other, boolean copyHistory) {
		rules = other.rules;
		board = new PieceType[getSize()][getSize()];
		for (int row = 0; row < getSize(); row++) {
			for (int col = 0; col < getSize(); col++) {
				RowCol square = new RowCol(row, col);
				setPieceAt(square, other.getPieceAt(square));
			}
		}
		legalMoves = Arrays.copyOf(other.getLegalMoves(), other.getLegalMoves().length);
		toMove = other.getToMove();
		jumpInProgress = other.isJumpInProgress();
		moveHistory = new ArrayList<Move>();
		halfMoveClock = other.getHalfMoveClock();
		if (copyHistory) {
			Collections.addAll(moveHistory, other.getMoveHistory());
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
				RowCol square = new RowCol(row, col);
				listener.squareChanged(square, getPieceAt(square));
			}
		}
	}

	@Override
	public void newGame() {
		board = new PieceType[getSize()][getSize()];
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
	public PieceType getPieceAt(RowCol square) {
		return board[square.getRow()][square.getCol()];
	}

	@Override
	public PieceType getPieceAt(int row, int col) {
		return board[row][col];
	}

	private void setPieceAt(RowCol square, PieceType piece) {
		board[square.getRow()][square.getCol()] = piece;
		for (PositionListener l : listeners) {
			l.squareChanged(square, piece);
		}
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

		RowCol fromSquare = move.getFrom();
		RowCol toSquare = move.getTo();
		PieceType movingPiece = getPieceAt(fromSquare);
		move.setMovedPiece(movingPiece);
		setPieceAt(fromSquare, PieceType.NONE);

		if (move.isJump()) {
			// move is a jump - remove the intervening piece
			RowCol over = new RowCol((fromSquare.getRow() + toSquare.getRow()) / 2, (fromSquare.getCol() + toSquare.getCol()) / 2);
			move.setCapturedPiece(getPieceAt(over));
			setPieceAt(over, PieceType.NONE);
		}

		int h = halfMoveClock;

		// check for piece promotion
		boolean justPromoted = false;
		if (toSquare.getRow() == getPromotionRow(movingPiece)) {
			justPromoted = true;
			halfMoveClock = 0;
			setPieceAt(toSquare, movingPiece.toKing());
		} else {
			setPieceAt(toSquare, movingPiece);
		}

		if (move.isJump()) {
			// check for a possible chain of jumps
			Move[] jumps = rules.getLegalMoves(toSquare, true);
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

		LogUtils.finer("move made by " + toMove.getOtherColour() + ", legal moves now: " + Joiner.on(",").join(legalMoves));
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
	public void setRules(String ruleId) {
		try {
			GameRules rulesTmp = GameRules.getRules(ruleId);
			Validate.notNull(rulesTmp, "Unknown ruleset " + ruleId);
			Constructor<? extends GameRules> ctor = rulesTmp.getClass().getDeclaredConstructor(Position.class);
			rules = ctor.newInstance(this);
			newGame();
		} catch (Exception e) {
			e.printStackTrace();
			throw new CheckersException("can't instantiate a position: " + e.getMessage());
		}
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
			setPieceAt(move.getTo(), PieceType.NONE);
			setPieceAt(move.getFrom(), move.getMovedPiece());
			if (move.isJump()) {
				int overRow = (move.getFromRow() + move.getToRow()) / 2;
				int overCol = (move.getFromCol() + move.getToCol()) / 2;
				setPieceAt(new RowCol(overRow, overCol), move.getCapturedPiece());
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
}
