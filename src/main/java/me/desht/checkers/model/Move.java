package me.desht.checkers.model;

public class Move {
	private final int fromRow, fromCol;
	private final int toRow, toCol;
	private boolean chainedJump;
	private PieceType capturedPiece;
	private PieceType movedPiece;

	public Move(int fromRow, int fromCol, int toRow, int toCol) {
		//		int rowOff = Math.abs(fromRow - toRow);
		//		int colOff = Math.abs(fromRow - toRow);
		//		CheckersValidate.isTrue(rowOff > 0 && rowOff < 3, Messages.getString("Game.illegalMove"));
		//		CheckersValidate.isTrue(colOff > 0 && colOff < 3, Messages.getString("Game.illegalMove"));
		//		CheckersValidate.isTrue(colOff == rowOff, Messages.getString("Game.illegalMove"));

		this.fromRow = fromRow;
		this.fromCol = fromCol;
		this.toRow = toRow;
		this.toCol = toCol;
		this.chainedJump = false;
	}

	public Move(int encoded) {
		this(encoded & 0x7, (encoded >> 3) & 0x7, (encoded >> 6) & 0x7, (encoded >> 9) & 0x7);
		this.movedPiece = PieceType.decode((encoded >> 15) & 0x3);
		if (isJump()) {
			this.chainedJump = (encoded & 0x1000) != 0;
			this.capturedPiece = PieceType.decode((encoded >> 13) & 0x3);
		}
	}

	public int encode() {
		int enc = fromRow + (fromCol << 3) + (toRow << 6) + (toCol << 9);
		enc |= movedPiece.encode() << 15;
		if (isJump()) {
			if (chainedJump) enc |= 0x1000;
			enc |= capturedPiece.encode() << 13;
		}
		return enc;
	}

	public boolean isJump() {
		return Math.abs(fromRow - toRow) == 2 && Math.abs(fromCol - toCol) == 2;
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
		return fromRow;
	}

	/**
	 * @return the fromCol
	 */
	public int getFromCol() {
		return fromCol;
	}

	/**
	 * @return the toRow
	 */
	public int getToRow() {
		return toRow;
	}

	/**
	 * @return the toCol
	 */
	public int getToCol() {
		return toCol;
	}

	@Override
	public String toString() {
		return Checkers.rowColToCheckersNotation(fromRow, fromCol) + "-" + Checkers.rowColToCheckersNotation(toRow, toCol);
	}

	public String toChainedString() {
		return "-" + Checkers.rowColToCheckersNotation(toRow, toCol);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fromCol;
		result = prime * result + fromRow;
		result = prime * result + toCol;
		result = prime * result + toRow;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Move other = (Move) obj;
		if (fromCol != other.fromCol)
			return false;
		if (fromRow != other.fromRow)
			return false;
		if (toCol != other.toCol)
			return false;
		if (toRow != other.toRow)
			return false;
		return true;
	}
}
