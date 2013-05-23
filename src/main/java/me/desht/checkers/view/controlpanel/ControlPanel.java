package me.desht.checkers.view.controlpanel;

import java.util.HashMap;
import java.util.Map;

import me.desht.checkers.TimeControl;
import me.desht.checkers.TimeControlDefs;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.view.BoardRotation;
import me.desht.checkers.view.BoardStyle;
import me.desht.checkers.view.BoardView;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;

public class ControlPanel {
	private static final int PANEL_WIDTH = 8;

	private final BoardView view;
	private final Cuboid panelBlocks;
	private BoardRotation boardDir;
	private BoardRotation signDir;
	private final Cuboid toMoveIndicator;
	private final PlyCountLabel plyCountLabel;
	private final HalfmoveClockLabel halfMoveClockLabel;
	private final ClockLabel[] clockLabels;
	private final Map<PersistableLocation, AbstractSignButton> buttonLocs;
	private final Map<String,AbstractSignButton> buttonNames;

	public ControlPanel(BoardView view) {
		this.view = view;
		this.boardDir = view.getBoard().getRotation();
		this.signDir = boardDir.getRight();

		buttonLocs = new HashMap<PersistableLocation, AbstractSignButton>();
		buttonNames = new HashMap<String, AbstractSignButton>();
		panelBlocks = getPanelPosition();
		plyCountLabel = new PlyCountLabel(this);
		halfMoveClockLabel = new HalfmoveClockLabel(this);
		clockLabels = new ClockLabel[2];
		clockLabels[PlayerColour.WHITE.getIndex()] = new ClockLabel(this, PlayerColour.WHITE);
		clockLabels[PlayerColour.BLACK.getIndex()] = new ClockLabel(this, PlayerColour.BLACK);
		toMoveIndicator = panelBlocks.inset(CuboidDirection.Vertical, 1).
				expand(boardDir.getDirection(), -((PANEL_WIDTH - 2) / 2)).
				expand(boardDir.getDirection().opposite(), -((PANEL_WIDTH - 2) / 2));

		createSignButtons();
	}

	public boolean isButton(Location location) {
		return buttonLocs.containsKey(new PersistableLocation(location));
	}

	public <T extends AbstractSignButton> T getButton(Class<T> type) {
		return type.cast(buttonNames.get(type.getSimpleName()));
	}

	public void handleButtonClick(PlayerInteractEvent event) {
		AbstractSignButton btn = buttonLocs.get(new PersistableLocation(event.getClickedBlock().getLocation()));
		if (btn != null) {
			btn.onClicked(event);
		}
	}

	/**
	 * @return the view
	 */
	public BoardView getView() {
		return view;
	}

	public void repaint(MassBlockUpdate mbu) {
		repaintPanel(mbu);
		repaintControls();
	}

	public void repaintPanel(MassBlockUpdate mbu) {
		if (mbu != null) {
			panelBlocks.fill(view.getBoard().getBoardStyle().getControlPanelMaterial(), mbu);
		} else {
			panelBlocks.fill(view.getBoard().getBoardStyle().getControlPanelMaterial());
		}
		panelBlocks.forceLightLevel(view.getBoard().getBoardStyle().getLightLevel());

		repaintControls();
	}

	public void repaintControls() {
		repaintSignButtons();
		repaintClocks();
		halfMoveClockLabel.repaint();
		plyCountLabel.repaint();
		updateToMoveIndicator();
	}

	public void updateToMoveIndicator() {
		PlayerColour toPlay = PlayerColour.NONE;
		if (view.getGame() != null) {
			toPlay = view.getGame().getPosition().getToMove();
		}
		updateToMoveIndicator(toPlay);
}

	public void updateToMoveIndicator(PlayerColour toPlay) {
		MaterialWithData mat = getView().getBoard().getBoardStyle().getControlPanelMaterial();
		if (toPlay == PlayerColour.WHITE) {
			mat = getView().getBoard().getBoardStyle().getWhitePieceMaterial();
		} else if (toPlay == PlayerColour.BLACK) {
			mat = getView().getBoard().getBoardStyle().getBlackPieceMaterial();
		}
		toMoveIndicator.fill(mat);
	}

	public void updatePlyCount(int plyCount) {
		plyCountLabel.setCount(plyCount);
		plyCountLabel.repaint();
	}

	public void updateClock(PlayerColour colour, TimeControl timeControl) {
		clockLabels[colour.getIndex()].setTimeControl(timeControl);
		clockLabels[colour.getIndex()].repaint();
	}

	public void repaintClocks() {
		CheckersGame game = view.getGame();
		updateClock(PlayerColour.WHITE, game == null ? null : game.getTimeControl(PlayerColour.WHITE));
		updateClock(PlayerColour.BLACK, game == null ? null : game.getTimeControl(PlayerColour.BLACK));
	}

	public void removeSigns() {
		panelBlocks.shift(signDir.getDirection(), 1).fill(0, (byte)0);
	}

	public Location getTeleportInDestination() {
		double xOff = (panelBlocks.getUpperX() - panelBlocks.getLowerX()) / 2.0 + 0.5 + signDir.getXadjustment() * 3.5;
		double zOff = (panelBlocks.getUpperZ() - panelBlocks.getLowerZ()) / 2.0 + 0.5 + signDir.getZadjustment() * 3.5;

		return new Location(panelBlocks.getWorld(),
		                    panelBlocks.getLowerX() + xOff,
		                    panelBlocks.getLowerY(),
		                    panelBlocks.getLowerZ() + zOff,
		                    (signDir.getYaw() + 180.0f) % 360,
		                    0.0f);
	}

	public Cuboid getPanelBlocks() {
		return panelBlocks;
	}

	public TimeControlDefs getTcDefs() {
		return getButton(TimeControlButton.class).getTcDefs();
	}

	private Cuboid getPanelPosition() {
		BoardStyle style = view.getBoard().getBoardStyle();
		BoardRotation dir = view.getBoard().getRotation();
		BoardRotation dirLeft = dir.getLeft();
		Location a1 = view.getBoard().getA1Corner().getLocation();

		int panelOffset = 4 * style.getSquareSize() - PANEL_WIDTH / 2;
		int frameOffset = (int) Math.ceil((style.getFrameWidth() + .5) / 2);

		// for the control panel edge, move <panelOffset> blocks in the board's direction, then
		// <frameOffset> blocks to the left of that.
		int x = a1.getBlockX() + dir.getXadjustment(panelOffset) + dirLeft.getXadjustment(frameOffset);
		int y = a1.getBlockY() + 1;
		int z = a1.getBlockZ() + dir.getZadjustment(panelOffset) + dirLeft.getZadjustment(frameOffset);
		// then expand the cuboid in the board's direction by the panel's desired width
		Cuboid panel = new Cuboid(new Location(a1.getWorld(), x, y, z));
		return panel.expand(dir.getDirection(), PANEL_WIDTH - 1).expand(CuboidDirection.Up, 2);
	}

	private void createSignButtons() {
		createSignButton(new BlackNoButton(this));
		createSignButton(new BlackYesButton(this));
		createSignButton(new BoardInfoButton(this));
		createSignButton(new CreateGameButton(this));
		createSignButton(new GameInfoButton(this));
		createSignButton(new InviteAnyoneButton(this));
		createSignButton(new InvitePlayerButton(this));
		createSignButton(new OfferDrawButton(this));
		createSignButton(new ResignButton(this));
		createSignButton(new StakeButton(this));
		createSignButton(new StartButton(this));
		createSignButton(new TeleportButton(this));
		createSignButton(new TimeControlButton(this));
		createSignButton(new UndoButton(this));
		createSignButton(new WhiteNoButton(this));
		createSignButton(new WhiteYesButton(this));
	}

	private void createSignButton(AbstractSignButton button) {
		buttonLocs.put(button.getLocation(), button);
		buttonNames.put(button.getClass().getSimpleName(), button);
	}

	private void repaintSignButtons() {
		for (AbstractSignButton btn : buttonLocs.values()) {
			btn.repaint();
		}
	}
}
