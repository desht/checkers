package me.desht.checkers.view;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.CheckersValidate;
import me.desht.checkers.Messages;
import me.desht.checkers.model.PieceType;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.Position;
import me.desht.checkers.model.RowCol;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.block.CraftMassBlockUpdate;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;

public class CheckersBoard {
	// the center of the A1 square (lower-left on the board)
	private final PersistableLocation a1Center;
	// the lower-left-most part (outer corner) of the bottom left square (depends on rotation)
	private final PersistableLocation bottomLeftCorner;
	// region that defines the board itself - just the squares
	private final Cuboid boardSquares;
	// area above the board squares
	private final Cuboid aboveSquares;
	// region outset by the frame
	private final Cuboid frameBoard;
	// area <i>above</i> the board
	private final Cuboid aboveFullBoard;
	// the full board region (board, frame, and area above)
	private final Cuboid fullBoard;
	// this is the direction white faces
	private final BoardRotation rotation;
	// number of squares on the edge
	private final int size;

	// the square currently selected, if any
	private RowCol selectedSquare = null;
	// the last moved square, if any
	private RowCol lastMovedSquare = null;

	// settings related to how the board is drawn
	private BoardStyle boardStyle = null;
	// note a full redraw needed if the board or piece style change
	private boolean redrawNeeded;

	/**
	 * Constructor.
	 *
	 * @param origin board origin (where the player clicked; the centre of the bottom left square)
	 * @param rotation board rotation
	 * @param boardStyleName name of the board style to use for this board
	 * @param size board size; the number of squares on an edge of the board
	 */
	public CheckersBoard(Location origin, BoardRotation rotation, String boardStyleName, int size) {
		setBoardStyle(boardStyleName);
		this.rotation = rotation;
		this.size = size;

		a1Center = new PersistableLocation(origin);
		a1Center.setSavePitchAndYaw(false);
		bottomLeftCorner = initBottomLeftCorner(origin);
		PersistableLocation topRightCorner = initTopRightCorner();
		boardSquares = new Cuboid(bottomLeftCorner.getLocation(), topRightCorner.getLocation());
		aboveSquares = boardSquares.expand(CuboidDirection.Up, boardStyle.getHeight());
		frameBoard = boardSquares.outset(CuboidDirection.Horizontal, boardStyle.getFrameWidth());
		aboveFullBoard = frameBoard.shift(CuboidDirection.Up, 1).expand(CuboidDirection.Up, boardStyle.getHeight() - 1);
		fullBoard = frameBoard.expand(CuboidDirection.Up, boardStyle.getHeight() + 1);
		validateBoardPosition();
	}

	private PersistableLocation initBottomLeftCorner(Location origin) {
		Location a1 = new Location(origin.getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
		int offset = -boardStyle.getSquareSize() / 2;
		BoardRotation toRight = rotation.getRight();
		a1.add(rotation.getXadjustment(offset), 0, rotation.getZadjustment(offset));
		a1.add(toRight.getXadjustment(offset), 0, toRight.getZadjustment(offset));
		return new PersistableLocation(a1);
	}

	private PersistableLocation initTopRightCorner() {
		Location bottomLeft = bottomLeftCorner.getLocation();
		Location topRight = new Location(bottomLeft.getWorld(), bottomLeft.getBlockX(), bottomLeft.getBlockY(), bottomLeft.getBlockZ());
		int size = (boardStyle.getSquareSize() * getSize()) - 1;
		BoardRotation toRight = rotation.getRight();
		topRight.add(rotation.getXadjustment(size), 0, rotation.getZadjustment(size));
		topRight.add(toRight.getXadjustment(size), 0, toRight.getZadjustment(size));
		return new PersistableLocation(topRight);
	}

	private void validateBoardPosition() {
		Cuboid bounds = getFullBoard();

		if (bounds.getUpperSW().getBlock().getLocation().getY() > bounds.getUpperSW().getWorld().getMaxHeight()) {
			throw new CheckersException(Messages.getString("Board.boardTooHigh"));
		}
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			if (bv.getBoard().getWorld() != bounds.getWorld()) {
				continue;
			}
			for (Block b : bounds.corners()) {
				if (bv.getBoard().getFullBoard().contains(b)) {
					throw new CheckersException(Messages.getString("Board.boardWouldIntersect", bv.getName()));
				}
			}
		}
	}

	/**
	 * Get the board style
	 *
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
	 * Get the board size (the number of squares on an edge)
	 *
	 * @return the board size
	 */
	public int getSize() {
		return size;
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
	 * @return the bottomLeftCorner
	 */
	public PersistableLocation getBottomLeftCorner() {
		return bottomLeftCorner;
	}

//	/**
//	 * @return the topRightCorner
//	 */
//	public PersistableLocation getTopRightCorner() {
//		return topRightCorner;
//	}

	/**
	 * @return the board
	 */
	public Cuboid getBoardSquares() {
		return boardSquares;
	}

	/**
	 * @return the areaBoard
	 */
	public Cuboid getAboveSquares() {
		return aboveSquares;
	}

//	/**
//	 * @return the frameBoard
//	 */
//	public Cuboid getFrameBoard() {
//		return frameBoard;
//	}
//
//	/**
//	 * @return the aboveFullBoard
//	 */
//	public Cuboid getAboveFullBoard() {
//		return aboveFullBoard;
//	}

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
		Cuboid bounds = getBoardSquares().shift(CuboidDirection.Up, minHeight).expand(CuboidDirection.Up, maxHeight - minHeight);
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
	 * @param square the square position
	 * @return a Cuboid representing the square
	 */
	public Cuboid getSquare(RowCol square) {
		if (square == null) new Exception().printStackTrace();
		CheckersValidate.isTrue(square != null && square.getRow() >= 0 && square.getRow() < getSize()
				&& square.getCol() >= 0 && square.getCol() < getSize(),
			"CheckersBoard: getSquare: bad (row, col): " + square);

		Cuboid sq = new Cuboid(bottomLeftCorner.getLocation());

		int s = boardStyle.getSquareSize();
		CuboidDirection dir = rotation.getDirection();
		CuboidDirection dirRight = rotation.getRight().getDirection();

		sq = sq.shift(dir, square.getRow() * s).shift(dirRight, square.getCol() * s);
		sq = sq.expand(dir, s - 1).expand(dirRight, s - 1);

		return sq;
	}

//	/**
//	 * Get the region above the given square, expanded to the board's height.
//	 *
//	 * @param square the square position
//	 * @return the region
//	 */
//	public Cuboid getPieceRegion(RowCol square) {
//		return getSquare(square).expand(CuboidDirection.Up, boardStyle.getHeight() - 1).shift(CuboidDirection.Up, 1);
//	}

	public World getWorld() {
		return boardSquares.getWorld();
	}

	public RowCol getSelectedSquare() {
		return selectedSquare;
	}

	public void setSelected(RowCol square) {
		clearSelected();
		this.selectedSquare = square;
		highlightSquare(selectedSquare, boardStyle.getSelectedHighlightMaterial());
	}

	public void clearSelected() {
		if (selectedSquare == lastMovedSquare) {
			highlightSquare(lastMovedSquare, boardStyle.getLastMoveHighlightMaterial());
		} else if (selectedSquare != null) {
			paintBoardSquare(selectedSquare, null);
			selectedSquare = null;
		}
	}

	public void setLastMovedSquare(RowCol square) {
		clearLastMovedSquare();
		this.lastMovedSquare = square;
		highlightSquare(lastMovedSquare, boardStyle.getLastMoveHighlightMaterial());
	}

	public void clearLastMovedSquare() {
		if (selectedSquare == lastMovedSquare) {
			highlightSquare(selectedSquare, boardStyle.getSelectedHighlightMaterial());
		} else if (lastMovedSquare != null) {
			paintBoardSquare(lastMovedSquare, null);
		}
		lastMovedSquare = null;
	}

	void repaint(MassBlockUpdate mbu) {
		fullBoard.fill(0, (byte)0, mbu);
		paintEnclosure(mbu);
		paintFrame(mbu);
		paintBoard(mbu);
		highlightSquare(selectedSquare, boardStyle.getSelectedHighlightMaterial());
		highlightSquare(lastMovedSquare, boardStyle.getLastMoveHighlightMaterial());
		fullBoard.forceLightLevel(boardStyle.getLightLevel());
		redrawNeeded = false;
		if (CheckersPlugin.getInstance().getDynmapIntegration() != null) {
			CheckersPlugin.getInstance().getDynmapIntegration().triggerUpdate(fullBoard);
		}
	}

	void reset() {
		clearLastMovedSquare();
		clearSelected();
		MassBlockUpdate mbu = CraftMassBlockUpdate.createMassBlockUpdater(CheckersPlugin.getInstance(), getWorld());
		paintBoard(mbu);
		getBoardSquares().shift(CuboidDirection.Up, 1).expand(CuboidDirection.Up, getBoardStyle().getHeight() - 1).fill(0, (byte)0, mbu);
		mbu.notifyClients();
	}

	public void paintPieces(Position position) {
		for (int row = 0; row < getSize(); ++row) {
			for (int col = 0; col < getSize(); ++col) {
				RowCol square = RowCol.get(row, col);
				paintPiece(square, position.getPieceAt(square), position.getRules().getWhoMovesFirst());
			}
		}
	}

	public void paintPiece(RowCol square, PieceType piece, PlayerColour starts) {
		if (square.getRow() % 2 != square.getCol() % 2) {
			// pieces are only ever found on half the squares of a checkers board
			return;
		}
		MaterialWithData mat;
		switch (piece.getColour()) {
			case WHITE: mat = boardStyle.getWhitePieceMaterial(); break;
			case BLACK: mat = boardStyle.getBlackPieceMaterial(); break;
			default: mat = MaterialWithData.get(0); break;
		}
		Cuboid c = getSquare(square).shift(CuboidDirection.Up, 1);
		if (mat.getBukkitMaterial() == Material.SKULL && boardStyle.getSquareSize() <= 3) {
			// special case: skull "microblocks" with a facing direction
			Block b = c.getCenter().getBlock();

			if (piece.isKing()) {
				b.setType(piece.getColour() == PlayerColour.WHITE ? Material.FENCE : Material.NETHER_FENCE);
				drawSkull(mat, b.getRelative(BlockFace.UP), starts == piece.getColour());
			} else {
				drawSkull(mat, b, starts == piece.getColour());
			}
		} else {
			// TODO: simple algorithm just draws square pieces; should be disks!
			c = c.inset(CuboidDirection.Horizontal, 1);
			int height = boardStyle.getSquareSize() / 5 + 1;
			if (piece.isKing() || piece == PieceType.NONE) {
				height *= 2;
			}
			c = c.expand(CuboidDirection.Up, height - 1);
			c.fill(mat);
			if (CheckersPlugin.getInstance().getDynmapIntegration() != null) {
				CheckersPlugin.getInstance().getDynmapIntegration().triggerUpdate(c);
			}
		}
	}

	private void drawSkull(MaterialWithData mat, Block b, boolean rotate) {
		b.setType(Material.SKULL);
		Skull skull = (Skull) b.getState();
		org.bukkit.material.Skull skullData = (org.bukkit.material.Skull) skull.getData();
		skullData.setFacingDirection(BlockFace.SELF);
		skull.setData(skullData);
		BlockFace face = rotate ? rotation.getBlockFace() : rotation.getBlockFace().getOppositeFace();
		skull.setRotation(face);
		if (mat.getText().length == 0) {
			skull.setSkullType(rotate ? SkullType.PLAYER : SkullType.CREEPER);
		} else if (mat.getText()[0].startsWith("*")) {
			skull.setSkullType(SkullType.valueOf(mat.getText()[0].substring(1).toUpperCase()));
		} else {
			skull.setSkullType(SkullType.PLAYER);
			skull.setOwner(mat.getText()[0]);
		}
		skull.update();
	}

	public void reloadBoardStyle() {
		if (boardStyle != null) {
			setBoardStyle(boardStyle.getName());
		}
	}

	private void highlightSquare(RowCol square, MaterialWithData mat) {
		if (square != null && square.getRow() >= 0 && square.getRow() < getSize() && square.getCol() >= 0 && square.getCol() < getSize()) {
			Cuboid c = getSquare(square);
			c.getFace(CuboidDirection.East).fill(mat);
			c.getFace(CuboidDirection.North).fill(mat);
			c.getFace(CuboidDirection.West).fill(mat);
			c.getFace(CuboidDirection.South).fill(mat);
			if (CheckersPlugin.getInstance().getDynmapIntegration() != null) {
				CheckersPlugin.getInstance().getDynmapIntegration().triggerUpdate(c);
			}
		}
	}

	private void paintBoard(MassBlockUpdate mbu) {
		for (int row = 0; row < getSize(); row++) {
			for (int col = 0; col < getSize(); col++) {
				paintBoardSquare(RowCol.get(row, col), mbu);
			}
		}
	}

	private void paintBoardSquare(RowCol square, MassBlockUpdate mbu) {
		Cuboid c = getSquare(square);
		boolean black = (square.getCol() + (square.getRow() % 2)) % 2 == 0;
		if (mbu == null) {
			c.fill(black ? boardStyle.getBlackSquareMaterial() : boardStyle.getWhiteSquareMaterial());
		} else {
			c.fill(black ? boardStyle.getBlackSquareMaterial() : boardStyle.getWhiteSquareMaterial(), mbu);
		}
		if (CheckersPlugin.getInstance().getDynmapIntegration() != null) {
			CheckersPlugin.getInstance().getDynmapIntegration().triggerUpdate(c);
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
		MassBlockUpdate mbu = CraftMassBlockUpdate.createMassBlockUpdater(CheckersPlugin.getInstance(), getFullBoard().getWorld());
		getFullBoard().fill(0, (byte) 0, mbu);
		mbu.notifyClients();
	}

	public RowCol getSquareAt(Location loc) {
		if (!aboveSquares.contains(loc)) {
			return null;
		}

		int xOff = (loc.getBlockX() - boardSquares.getLowerX()) / boardStyle.getSquareSize();
		int zOff = (loc.getBlockZ() - boardSquares.getLowerZ()) / boardStyle.getSquareSize();

		LogUtils.fine("getSquareAt: " + loc + ": xOff = " + xOff + ", zOff = " + zOff);
		int sz = getSize() - 1;
		switch (getRotation()) {
		case NORTH:
			return RowCol.get(sz - zOff, xOff);
		case SOUTH:
			return RowCol.get(zOff, sz - xOff);
		case EAST:
			return RowCol.get(xOff, zOff);
		case WEST:
			return RowCol.get(sz - xOff, sz - zOff);
		default:
			return null;
		}
	}
}
