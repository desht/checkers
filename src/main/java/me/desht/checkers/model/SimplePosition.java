package me.desht.checkers.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.desht.checkers.IllegalMoveException;
import me.desht.dhutils.LogUtils;

import com.google.common.base.Joiner;

/**
 * Represents a checkers board position.
 *
 * This is a fairly simple-minded implementation, using an 8x8 array of piece types.
 *
 * TODO: Position and game (move history etc.) are currently all stored in this object.
 * It mnight be better design to move game details into a separate object which contains
 * a Position object.
 */
public class SimplePosition implements Position {
	private final List<PositionListener> listeners = new ArrayList<PositionListener>();
	private final PieceType[][] board;

	private PlayerColour toMove;
	private Move[] legalMoves;
	private boolean jumpInProgress;
	private List<Move> moveHistory;
	private int halfMoveClock; // moves since a capture was made
	private boolean forcedJump = true;
	public SimplePosition() {
		board = new PieceType[8][8];
		newGame();
	}

	public SimplePosition(SimplePosition other, boolean copyHistory) {
		board = new PieceType[8][8];
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
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

	@Override
	public boolean isForcedJump() {
		return forcedJump;
	}

	@Override
	public void setForcedJump(boolean forcedJump) {
		this.forcedJump = forcedJump;
	}

	@Override
	public void addPositionListener(PositionListener listener) {
		listeners.add(listener);
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				listener.squareChanged(row, col, getPieceAt(row, col));
			}
		}
	}

	@Override
	public void newGame() {
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				if (row % 2 == col % 2) {
					if (row < 3) {
						board[row][col] = PieceType.BLACK;
					} else if (row > 4) {
						board[row][col] = PieceType.WHITE;
					} else {
						board[row][col] = PieceType.NONE;
					}
				} else {
					board[row][col] = PieceType.NONE;
				}
			}
		}
		toMove = PlayerColour.BLACK;
		legalMoves = calculateLegalMoves(toMove);
		jumpInProgress = false;
		moveHistory = new ArrayList<Move>();
		halfMoveClock = 0;
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
	public Move[] getLegalMoves(int row, int col) {
		if (getPieceAt(row, col).getColour() != getToMove()) {
			return new Move[0];
		}
		List<Move> moves = new ArrayList<Move>();
		for (MoveDirection dir : MoveDirection.values()) {
			if (canJump(getToMove(), row, col, dir)) {
				moves.add(new Move(row, col, row + dir.getRowOffset(), col + dir.getColOffset()));
			}
		}
		if (moves.isEmpty()) {
			for (MoveDirection dir : MoveDirection.values()) {
				if (canMove(getToMove(), row, col, dir)) {
					moves.add(new Move(row, col, row + dir.getRowOffset(), col + dir.getColOffset()));
				}
			}
		}
		return moves.toArray(new Move[moves.size()]);
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
		if (toRow == 7 && movingPiece == PieceType.BLACK) {
			justPromoted = true;
			halfMoveClock = 0;
			setPieceAt(toRow, toCol, PieceType.BLACK_KING);
		} else if (toRow == 0 && movingPiece == PieceType.WHITE) {
			justPromoted = true;
			halfMoveClock = 0;
			setPieceAt(toRow, toCol, PieceType.WHITE_KING);
		} else {
			setPieceAt(toRow, toCol, movingPiece);
		}

		if (move.isJump()) {
			// check for a possible chain of jumps
			Move[] jumps = getLegalJumps(toRow, toCol);
			if (jumps.length > 0 && !justPromoted) {
				// the same player must continue jumping
				jumpInProgress = true;
				move.setChainedJump(true);
				legalMoves = jumps;
			} else {
				toMove = toMove.getOtherColour();
				legalMoves = calculateLegalMoves(toMove);
				jumpInProgress = false;
			}
			halfMoveClock = 0;
		} else {
			toMove = toMove.getOtherColour();
			legalMoves = calculateLegalMoves(toMove);
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
	public PlayerColour getToMove() {
		return toMove;
	}

	/**
	 * @return the halfMoveClock
	 */
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
		legalMoves = calculateLegalMoves(toMove);
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
		// need to account for chained jumps
		int count = 0;
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

	private Move[] calculateLegalMoves(PlayerColour who) {
		List<Move> moves = new ArrayList<Move>();

		// get all the possible jumps that can be made
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				if (board[row][col].getColour() == who) {
					for (MoveDirection dir : MoveDirection.values()) {
						if (canJump(who, row, col, dir)) {
							moves.add(new Move(row, col, row + dir.getRowOffset() * 2, col + dir.getColOffset() * 2));
						}
					}
				}
			}
		}

		// if there are any jumps, the player *must* jump, so don't calculate any non-jump moves
		if (moves.isEmpty() || !forcedJump) {
			for (int row = 0; row < 8; row++) {
				for (int col = 0; col < 8; col++) {
					if (board[row][col].getColour() == who) {
						for (MoveDirection dir : MoveDirection.values()) {
							if (canMove(who, row, col, dir)) {
								moves.add(new Move(row, col, row + dir.getRowOffset(), col + dir.getColOffset()));
							}
						}
					}
				}
			}
		}

		return moves.toArray(new Move[moves.size()]);
	}

	private Move[] getLegalJumps(int row, int col) {
		if (getPieceAt(row, col).getColour() != getToMove()) {
			return new Move[0];
		}
		List<Move> moves = new ArrayList<Move>();
		for (MoveDirection dir : MoveDirection.values()) {
			if (canJump(getToMove(), row, col, dir)) {
				moves.add(new Move(row, col, row + dir.getRowOffset() * 2, col + dir.getColOffset() * 2));
			}
		}
		return moves.toArray(new Move[moves.size()]);
	}

	private boolean canMove(PlayerColour who, int fromRow, int fromCol, MoveDirection direction) {
		int toRow = fromRow + direction.getRowOffset();
		int toCol = fromCol + direction.getColOffset();
		if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
			return false;
		}
		PieceType moving = getPieceAt(fromRow, fromCol);
		PieceType target = getPieceAt(toRow, toCol);
		if (target != PieceType.NONE) {
			return false;
		}
		if (moving == PieceType.WHITE && toRow > fromRow) {
			return false;
		}
		if (moving == PieceType.BLACK && toRow < fromRow) {
			return false;
		}
		return true;
	}

	private boolean canJump(PlayerColour who, int fromRow, int fromCol, MoveDirection direction) {
		int overRow = fromRow + direction.getRowOffset();
		int overCol = fromCol + direction.getColOffset();
		int toRow = fromRow + direction.getRowOffset(2);
		int toCol = fromCol + direction.getColOffset(2);
		if (toRow < 0 || toRow > 7 || toCol < 0 || toCol > 7) {
			return false;
		}
		PieceType moving = getPieceAt(fromRow, fromCol);
		PieceType victim = getPieceAt(overRow, overCol);
		PieceType target = getPieceAt(toRow, toCol);
		if (target != PieceType.NONE) {
			return false;
		}
		if (moving == PieceType.WHITE && toRow > fromRow) {
			return false;
		}
		if (moving == PieceType.BLACK && toRow < fromRow) {
			return false;
		}
		if (victim.getColour() != moving.getColour().getOtherColour()) {
			return false;
		}
		return true;
	}
}
