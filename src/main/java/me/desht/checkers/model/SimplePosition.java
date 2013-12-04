package me.desht.checkers.model;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.desht.checkers.CheckersException;
import me.desht.checkers.IllegalMoveException;
import me.desht.checkers.model.rules.GameRules;
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
	private boolean[][] doomed;
	private GameRules rules;

	private PlayerColour toMove;

	private Move[] legalMoves;
	private boolean jumpInProgress;
	private List<Move> moveHistory;
	private int halfMoveClock; // moves since a capture was made

	public SimplePosition(String ruleset) {
		setRules(ruleset);
		newGame();
	}

	public SimplePosition(SimplePosition other, boolean copyHistory) {
		rules = other.rules;
		int l = other.board.length;
		board = new PieceType[l][l];
		doomed = new boolean[l][l];
		for (int row = 0; row < l; row++) {
			System.arraycopy(other.board[row], 0, board[row], 0, l);
			System.arraycopy(other.doomed[row], 0, doomed[row], 0, l);
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

	@Override
	public int getBoardSize() {
		return rules.getBoardSize();
	}

	@Override
	public void addPositionListener(PositionListener listener) {
		listeners.add(listener);
		for (int row = 0; row < getBoardSize(); row++) {
			for (int col = 0; col < getBoardSize(); col++) {
				RowCol square = RowCol.get(row, col);
				listener.squareChanged(square, getPieceAt(square));
			}
		}
		listener.plyCountChanged(getPlyCount());
		listener.halfMoveClockChanged(getHalfMoveClock());
		listener.toMoveChanged(getToMove());
	}

	@Override
	public void newGame() {
		board = new PieceType[getBoardSize()][getBoardSize()];
		doomed = new boolean[getBoardSize()][getBoardSize()];
		toMove = rules.getWhoMovesFirst();
		for (int row = 0; row < getBoardSize(); row++) {
			for (int col = 0; col < getBoardSize(); col++) {
				if (row % 2 == col % 2) {
					if (row < rules.getPieceRowCount()) {
						board[row][col] = getPieceForColour(toMove);
					} else if (row > getBoardSize() - 1 - rules.getPieceRowCount()) {
						board[row][col] = getPieceForColour(toMove.getOtherColour());
					} else {
						board[row][col] = PieceType.NONE;
					}
				} else {
					board[row][col] = PieceType.NONE;
				}
			}
		}
		legalMoves = rules.calculateLegalMoves(this);
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

	@Override
	public Move[] getLegalMoves() {
		return legalMoves;
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
		setPieceAt(toSquare, movingPiece);

		Move[] moreJumps;
		if (move.isJump()) {
			// move is a jump - remove the intervening piece
			RowCol over = getCapturingSquare(fromSquare, toSquare);
			move.setCapturedPiece(getPieceAt(over));
			markCaptured(over);
			moreJumps = rules.getLegalMoves(this, toSquare, true);
		} else {
			moreJumps = new Move[0];
		}

		int h = halfMoveClock;

		// check for piece promotion
		boolean justPromoted = false;
		if (toSquare.getRow() == getPromotionRow(movingPiece) && (moreJumps.length == 0 || rules.allowChainedJumpPromotion()) && !movingPiece.isKing()) {
			justPromoted = true;
			halfMoveClock = 0;
			setPieceAt(toSquare, movingPiece.toKing());
		}

		if (move.isJump()) {
			// check for a possible chain of jumps
			if (moreJumps.length > 0 && !justPromoted) {
				// the same player must continue jumping
				jumpInProgress = true;
				move.setChainedJump(true);
				legalMoves = moreJumps;
			} else {
				// this is the final (or only) jump move in the chain
				for (int r = 0; r < getBoardSize(); r++) {
					for (int c = 0; c < getBoardSize(); c++) {
						if (doomed[r][c]) {
							setPieceAt(RowCol.get(r, c), PieceType.NONE);
							doomed[r][c] = false;
						}
					}
				}
				toMove = toMove.getOtherColour();
				legalMoves = rules.calculateLegalMoves(this);
				jumpInProgress = false;
			}
			halfMoveClock = 0;
		} else {
			toMove = toMove.getOtherColour();
			legalMoves = rules.calculateLegalMoves(this);
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

	private void markCaptured(RowCol square) {
		doomed[square.getRow()][square.getCol()] = true;
	}

	@Override
	public boolean isMarkedCaptured(RowCol square) {
		return doomed[square.getRow()][square.getCol()];
	}

	@Override
	public Position tryMove(Move move) {
		Position newPos = new SimplePosition(this, false);
		newPos.makeMove(new Move(move.getFrom(), move.getTo()));
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
			Constructor<? extends GameRules> ctor = rulesTmp.getClass().getDeclaredConstructor();
			rules = ctor.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			throw new CheckersException("can't instantiate a position: " + e.getMessage());
		}
	}

	private int getPromotionRow(PieceType movingPiece) {
		return movingPiece.getColour() == rules.getWhoMovesFirst() ? getBoardSize() - 1 : 0;
	}

	private void setPieceAt(RowCol square, PieceType piece) {
		board[square.getRow()][square.getCol()] = piece;
		for (PositionListener l : listeners) {
			l.squareChanged(square, piece);
		}
	}

	private RowCol getCapturingSquare(RowCol fromSquare, RowCol toSquare) {
		// find the first square between fromSquare and toSquare with a piece of the opposing colour
		int fr = fromSquare.getRow(), tr = toSquare.getRow();
		int fc = fromSquare.getCol(), tc = toSquare.getCol();
		int roff = Integer.signum(tr - fr), coff = Integer.signum(tc - fc);
		while (fr != tr) {
			fr += roff; fc += coff;
			if (getPieceAt(fr, fc).getColour() == toMove.getOtherColour()) {
				return RowCol.get(fr, fc);
			}
		}
		throw new IllegalStateException("impossible: no capture candidate found between " + fromSquare + " and " + toSquare);
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
		return moveHistory.isEmpty() ? null : moveHistory.get(moveHistory.size() - 1);
	}

	@Override
	public void undoLastMove(int nMoves) {
		if (moveHistory.size() == 0) {
			return;
		}
		int idx = moveHistory.size() - 1;
		// back up until the start of the last complete move
		// (could be multiple steps if the last move was a chained jump)
		while (nMoves-- > 0 && idx >= 0) {
			do {
				idx--;
			} while (idx >= 0 && moveHistory.get(idx).isChainedJump());
		}

		// now reset the position to the start and replay all moves up to the previous move
		List<PositionListener> saved = new ArrayList<PositionListener>();
		saved.addAll(listeners);
		listeners.clear();
		List<Move> tmp = new ArrayList<Move>(moveHistory);
		newGame();
		for (int i = 0; i <= idx; i++) {
			makeMove(tmp.get(i));
		}
		// restore the position listeners
		for (PositionListener l : saved) {
			addPositionListener(l);
			l.lastMoveUndone(this);
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
