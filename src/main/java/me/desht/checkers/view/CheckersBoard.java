package me.desht.checkers.view;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.model.PieceType;
import me.desht.checkers.model.Position;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class CheckersBoard {
	// the center of the A1 square (lower-left on the board)
	private final PersistableLocation a1Center;
	// the lower-left-most part (outer corner) of the a1 square (depends on rotation)
	private final PersistableLocation a1Corner;
	// the upper-right-most part (outer corner) of the h8 square (depends on rotation)
	private final PersistableLocation h8Corner;
	// region that defines the board itself - just the squares
	private final Cuboid board;
	// area above the board squares
	private final Cuboid areaBoard;
	// region outset by the frame
	private final Cuboid frameBoard;
	// area <i>above</i> the board
	private final Cuboid aboveFullBoard;
	// the full board region (board, frame, and area above)
	private final Cuboid fullBoard;
	// this is the direction white faces
	private final BoardRotation rotation;

	// the square currently selected, if any
	private int selectedRow = -1, selectedCol = -1;

	// settings related to how the board is drawn
	private BoardStyle boardStyle = null;
	// note a full redraw needed if the board or piece style change
	private boolean redrawNeeded;

	public CheckersBoard(Location origin, BoardRotation rotation, String boardStyleName) {
		setBoardStyle(boardStyleName);
		this.rotation = rotation;
		a1Center = new PersistableLocation(origin);
		a1Corner = initA1Corner(origin);
		h8Corner = initH8Corner(a1Corner.getLocation());
		board = new Cuboid(a1Corner.getLocation(), h8Corner.getLocation());
		areaBoard = board.expand(CuboidDirection.Up, boardStyle.getHeight());
		frameBoard = board.outset(CuboidDirection.Horizontal, boardStyle.getFrameWidth());
		aboveFullBoard = frameBoard.shift(CuboidDirection.Up, 1).expand(CuboidDirection.Up, boardStyle.getHeight() - 1);
		fullBoard = frameBoard.expand(CuboidDirection.Up, boardStyle.getHeight() + 1);
		validateBoardPosition();
	}

	private PersistableLocation initA1Corner(Location origin) {
		Location a1 = new Location(origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
		int offset = -boardStyle.getSquareSize() / 2;
		a1.add(rotation.getXadjustment(offset), 0, rotation.getRight().getXadjustment(offset));
		return new PersistableLocation(a1);
	}

	private PersistableLocation initH8Corner(Location a1) {
		Location h8 = new Location(a1.getWorld(), a1.getBlockX(), a1.getBlockY(), a1.getBlockZ());
		int size = (boardStyle.getSquareSize() * 8) - 1;
		h8.add(rotation.getXadjustment(size), 0, rotation.getRight().getXadjustment(size));
		return new PersistableLocation(h8);
	}

	private void validateBoardPosition() {
		Cuboid bounds = getFullBoard();

		if (bounds.getUpperSW().getBlock().getLocation().getY() > bounds.getUpperSW().getWorld().getMaxHeight()) {
			throw new CheckersException(Messages.getString("BoardView.boardTooHigh"));
		}
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			if (bv.getBoard().getWorld() != bounds.getWorld()) {
				continue;
			}
			for (Block b : bounds.corners()) {
				if (bv.getBoard().getFullBoard().contains(b)) {
					throw new CheckersException(Messages.getString("BoardView.boardWouldIntersect", bv.getName()));
				}
			}
		}
	}

	/**
	 * @return the boardStyle
	 */
	public BoardStyle getBoardStyle() {
		return boardStyle;
	}

	public void setBoardStyle(String boardStyleName) {
		BoardStyle newStyle = BoardStyle.loadStyle(boardStyleName);
		setBoardStyle(newStyle);
	}

	/**
	 * @param boardStyle the boardStyle to set
	 */
	public void setBoardStyle(BoardStyle boardStyle) {
		this.boardStyle = boardStyle;
		redrawNeeded = true;
	}

	/**
	 * @return the a1Center
	 */
	public PersistableLocation getA1Center() {
		return a1Center;
	}

	/**
	 * @return the a1Corner
	 */
	public PersistableLocation getA1Corner() {
		return a1Corner;
	}

	/**
	 * @return the h8Corner
	 */
	public PersistableLocation getH8Corner() {
		return h8Corner;
	}

	/**
	 * @return the board
	 */
	public Cuboid getBoard() {
		return board;
	}

	/**
	 * @return the areaBoard
	 */
	public Cuboid getAreaBoard() {
		return areaBoard;
	}

	/**
	 * @return the frameBoard
	 */
	public Cuboid getFrameBoard() {
		return frameBoard;
	}

	/**
	 * @return the aboveFullBoard
	 */
	public Cuboid getAboveFullBoard() {
		return aboveFullBoard;
	}

	/**
	 * @return the rotation
	 */
	public BoardRotation getRotation() {
		return rotation;
	}

	public Cuboid getFullBoard() {
		return fullBoard;
	}

	/**
	 * @return the redrawNeeded
	 */
	public boolean isRedrawNeeded() {
		return redrawNeeded;
	}

	public boolean isOnBoard(Location loc, int minHeight, int maxHeight) {
		Cuboid bounds = getBoard().shift(CuboidDirection.Up, minHeight).expand(CuboidDirection.Up, maxHeight - minHeight);
		return bounds.contains(loc);
	}

	/**
	 * Check if the location is a part of the board itself.
	 * 
	 * @param loc	location to check
	 * @return true if the location is part of the board itself
	 */
	public boolean isOnBoard(Location loc) {
		return isOnBoard(loc, 0, 0);
	}

	/**
	 * Check if the location is above the board but below the enclosure roof.
	 * 
	 * @param loc	location to check
	 * @return true if the location is above the board AND within the board's height range
	 */
	public boolean isAboveBoard(Location loc) {
		return isOnBoard(loc, 1, getBoardStyle().getHeight());
	}

	/**
	 * Check if this is somewhere within the board bounds.
	 * 
	 * @param loc		location to check
	 * @param fudge		fudge factor - check within a slightly larger area
	 * @return true if the location is *anywhere* within the board <br>
	 *         including frame & enclosure
	 */
	public boolean isPartOfBoard(Location loc, int fudge) {
		Cuboid o = getFullBoard();
		if (fudge != 0) {
			o = o.outset(CuboidDirection.Both, fudge);
		}
		return o.contains(loc);
	}

	public boolean isPartOfBoard(Location loc) {
		return isPartOfBoard(loc, 0);
	}

	/**
	 * Get the Cuboid region for this square of the board itself.
	 *
	 * @param row
	 * @param col
	 * @return a Cuboid representing the square
	 */
	public Cuboid getSquare(int row, int col) {
		if (row < 0 || col < 0 || row > 7 || col > 7) {
			throw new CheckersException("CheckersBoard: getSquare: bad (row, col): (" + row + "," + col + ")");	
		}

		Cuboid sq = new Cuboid(a1Corner.getLocation());

		int s = boardStyle.getSquareSize();
		CuboidDirection dir = rotation.getDirection();
		CuboidDirection dirRight = rotation.getRight().getDirection();

		sq = sq.shift(dir, row * s).shift(dirRight, col * s);
		sq = sq.expand(dir, s - 1).expand(dirRight, s - 1);

		return sq;
	}

	/**
	 * Get the region above the given square, expanded to the board's height.
	 *
	 * @param row
	 * @param col
	 * @return
	 */
	public Cuboid getPieceRegion(int row, int col) {
		Cuboid sq = getSquare(row, col).expand(CuboidDirection.Up, boardStyle.getHeight() - 1).shift(CuboidDirection.Up, 1);
		return sq;
	}

	public World getWorld() {
		return board.getWorld();
	}

	public void setSelected(int row, int col) {
		selectedRow = row;
		selectedCol = col;
		highlightSquare(selectedRow, selectedCol, boardStyle.getSelectedHighlightMaterial());
	}

	public void clearSelected() {
		paintBoardSquare(selectedRow, selectedCol, null);
		selectedRow = selectedCol = -1;
	}

	void repaint(MassBlockUpdate mbu) {
		paintEnclosure(mbu);
		paintFrame(mbu);
		paintBoard(mbu);
		highlightSquare(selectedRow, selectedCol, boardStyle.getSelectedHighlightMaterial());
		fullBoard.forceLightLevel(boardStyle.getLightLevel());
		redrawNeeded = false;
	}

	public void paintPieces(Position position) {
		for (int row = 0; row < 8; ++row) {
			for (int col = 0; col < 8; ++col) {
				paintPiece(row, col, position.getPieceAt(row, col));
			}
		}
	}

	public void paintPiece(int row, int col, PieceType piece) {
		if (row % 2 != col % 2) {
			// pieces are only ever found on half the squares of a checkers board
			return;
		}
		MaterialWithData mat;
		switch (piece.getColour()) {
		case WHITE: mat = boardStyle.getWhitePieceMaterial(); break;
		case BLACK: mat = boardStyle.getBlackPieceMaterial(); break;
		default: mat = MaterialWithData.get(0); break;
		}
		// TODO: simple algorithm just draws cubic pieces; should be disks!
		Cuboid c = getSquare(row, col).inset(CuboidDirection.Horizontal, 1).shift(CuboidDirection.Up, 1);
		int height = boardStyle.getHeight();
		if (piece.isKing()) {
			height *= 2;
		}
		c = c.expand(CuboidDirection.Up, height);
		c.fill(mat);
	}

	private void highlightSquare(int row, int col, MaterialWithData mat) {
		if (row >= 0 && row <= 7 && col >= 0 && col <= 7) {
			Cuboid sq = getSquare(row, col);
			sq.getFace(CuboidDirection.East).fill(mat);
			sq.getFace(CuboidDirection.North).fill(mat);
			sq.getFace(CuboidDirection.West).fill(mat);
			sq.getFace(CuboidDirection.South).fill(mat);
		}
	}

	private void paintBoard(MassBlockUpdate mbu) {
		for (int row = 0; row < 7; row++) {
			for (int col = 0; col < 7; col++) {
				paintBoardSquare(row, col, mbu);
			}
		}
	}

	private void paintBoardSquare(int row, int col, MassBlockUpdate mbu) {
		Cuboid square = getSquare(row, col);
		boolean black = (col + (row % 2)) % 2 == 0;
		if (mbu == null) {
			square.fill(black ? boardStyle.getBlackSquareMaterial() : boardStyle.getWhiteSquareMaterial());
		} else {
			square.fill(black ? boardStyle.getBlackSquareMaterial() : boardStyle.getWhiteSquareMaterial(), mbu);
		}
	}

	private void paintFrame(MassBlockUpdate mbu) {
		int fw = boardStyle.getFrameWidth();
		MaterialWithData fm = boardStyle.getFrameMaterial();
		frameBoard.getFace(CuboidDirection.West).expand(CuboidDirection.East, fw - 1).fill(fm, mbu);
		frameBoard.getFace(CuboidDirection.South).expand(CuboidDirection.North, fw - 1).fill(fm, mbu);
		frameBoard.getFace(CuboidDirection.East).expand(CuboidDirection.West, fw - 1).fill(fm, mbu);
		frameBoard.getFace(CuboidDirection.North).expand(CuboidDirection.South, fw - 1).fill(fm, mbu);
	}

	private void paintEnclosure(MassBlockUpdate mbu) {
		aboveFullBoard.getFace(CuboidDirection.North).fill(boardStyle.getEnclosureMaterial(), mbu);
		aboveFullBoard.getFace(CuboidDirection.East).fill(boardStyle.getEnclosureMaterial(), mbu);
		aboveFullBoard.getFace(CuboidDirection.South).fill(boardStyle.getEnclosureMaterial(), mbu);
		aboveFullBoard.getFace(CuboidDirection.West).fill(boardStyle.getEnclosureMaterial(), mbu);

		fullBoard.getFace(CuboidDirection.Up).fill(boardStyle.getEnclosureMaterial(), mbu);

		if (!boardStyle.getEnclosureMaterial().equals(boardStyle.getStrutsMaterial())) {
			paintStruts(mbu);
		}
	}

	private void paintStruts(MassBlockUpdate mbu) {
		MaterialWithData struts = boardStyle.getStrutsMaterial();

		// vertical struts at the frame corners
		Cuboid c = new Cuboid(frameBoard.getLowerNE()).shift(CuboidDirection.Up, 1).expand(CuboidDirection.Up, boardStyle.getHeight());
		c.fill(struts, mbu);
		c = c.shift(CuboidDirection.South, frameBoard.getSizeX() - 1);
		c.fill(struts, mbu);
		c = c.shift(CuboidDirection.West, frameBoard.getSizeZ() - 1);
		c.fill(struts, mbu);
		c = c.shift(CuboidDirection.North, frameBoard.getSizeZ() - 1);
		c.fill(struts, mbu);

		// horizontal struts along roof edge
		Cuboid roof = frameBoard.shift(CuboidDirection.Up, boardStyle.getHeight() + 1);
		roof.getFace(CuboidDirection.East).fill(struts, mbu);
		roof.getFace(CuboidDirection.North).fill(struts, mbu);
		roof.getFace(CuboidDirection.West).fill(struts, mbu);
		roof.getFace(CuboidDirection.South).fill(struts, mbu);
	}

	public void clearAll() {
		// TODO Auto-generated method stub
		
	}
}
