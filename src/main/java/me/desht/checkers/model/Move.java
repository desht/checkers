package me.desht.checkers.model;

public class Move {
	private final RowCol from;
	private final RowCol to;
	private boolean chainedJump;
	private PieceType capturedPiece;
	private PieceType movedPiece;

	public Move(RowCol from, RowCol to) {
		this.from = from;
		this.to = to;
		this.chainedJump = false;
	}

	/**
	 * Backwards-compatibility, for saves where row/columns were saved with only 3 bits.
	 *
	 * @param encoded the encoded move
	 * @return a new Move object
	 */
	public static Move getOldFormatMove(int encoded) {
		Move m = new Move(new RowCol(encoded & 0x7, (encoded >> 3) & 0x7), new RowCol((encoded >> 6) & 0x7, (encoded >> 9) & 0x7));
		m.movedPiece = PieceType.decode((encoded >> 15) & 0x3);
		if (m.isJump()) {
			m.chainedJump = (encoded & 0x1000) != 0;
			m.capturedPiece = PieceType.decode((encoded >> 13) & 0x3);
		}
		return m;
	}

	public Move(int encoded) {
		this(new RowCol(encoded & 0xf, (encoded >> 4) & 0xf), new RowCol((encoded >> 8) & 0xf, (encoded >> 12) & 0xf));
		this.movedPiece = PieceType.decode((encoded >> 18) & 0x3);
		if (isJump()) {
			this.chainedJump = (encoded & 0x100000) != 0;
			this.capturedPiece = PieceType.decode((encoded >> 16) & 0x3);
		}
	}

	public int encode() {
		int enc = from.getRow() + (from.getCol() << 4) + (to.getRow() << 8) + (to.getCol() << 12);
		enc |= movedPiece.encode() << 18;
		if (isJump()) {
			if (chainedJump) enc |= 0x100000;
			enc |= capturedPiece.encode() << 16;
		}
		return enc;
	}

	public RowCol getFrom() {
		return from;
	}

	public RowCol getTo() {
		return to;
	}

	public boolean isJump() {
		return Math.abs(from.getRow() - to.getRow()) == 2 && Math.abs(from.getCol() - to.getCol()) == 2;
	}

	public boolean isChainedJump() {
		return chainedJump;
	}

	public void setChainedJump(boolean chained) {
		this.chainedJump = chained;
	}

	public void setCapturedPiece(PieceType piece) {
		this.capturedPiece = piece;
	}

	public PieceType getCapturedPiece() {
		return capturedPiece;
	}

	public PieceType getMovedPiece() {
		return movedPiece;
	}

	public void setMovedPiece(PieceType movedPiece) {
		this.movedPiece = movedPiece;
	}

	/**
	 * @return the fromRow
	 */
	public int getFromRow() {
		return from.getRow();
	}

	/**
	 * @return the fromCol
	 */
	public int getFromCol() {
		return from.getCol();
	}

	/**
	 * @return the toRow
	 */
	public int getToRow() {
		return to.getRow();
	}

	/**
	 * @return the toCol
	 */
	public int getToCol() {
		return to.getCol();
	}

	public String toCheckersNotation(int boardSize) {
		return from.toCheckersNotation(boardSize) + "-" + to.toCheckersNotation(boardSize);
	}

	@Override
	public String toString() {
		return from.toString() + "-" + to.toString(); // + "[" + movedPiece + "|" + capturedPiece + "|" + chainedJump + "]";
	}

	public String toChainedString(int boardSize) {
		return "-" + to.toCheckersNotation(boardSize);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Move move = (Move) o;

//		if (chainedJump != move.chainedJump) return false;
//		if (capturedPiece != move.capturedPiece) return false;
		if (from != null ? !from.equals(move.from) : move.from != null) return false;
//		if (movedPiece != move.movedPiece) return false;
		if (to != null ? !to.equals(move.to) : move.to != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = from != null ? from.hashCode() : 0;
		result = 31 * result + (to != null ? to.hashCode() : 0);
		result = 31 * result + (chainedJump ? 1 : 0);
		result = 31 * result + (capturedPiece != null ? capturedPiece.hashCode() : 0);
		result = 31 * result + (movedPiece != null ? movedPiece.hashCode() : 0);
		return result;
	}
}
