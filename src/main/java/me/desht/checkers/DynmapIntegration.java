package me.desht.checkers;

import java.io.InputStream;

import me.desht.checkers.ai.CheckersAI;
import me.desht.checkers.event.CheckersBoardCreatedEvent;
import me.desht.checkers.event.CheckersBoardDeletedEvent;
import me.desht.checkers.event.CheckersGameCreatedEvent;
import me.desht.checkers.event.CheckersGameDeletedEvent;
import me.desht.checkers.event.CheckersGameStateChangedEvent;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.cuboid.Cuboid;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

public class DynmapIntegration implements Listener {
	private static final String MARKER_SET = "checkers.boards";
	private static final String ICON_ID = "checkers.board";
	private static final String ICON = "dynmap-checkers.png";

	private final CheckersPlugin plugin;
	private final DynmapAPI dynmap;
	private final MarkerAPI mapi;

	private MarkerSet markerSet;
	private MarkerIcon icon;
	private boolean active = false;

	private boolean enabled = true;
	private int priority = 1;
	private boolean hideByDefault = false;
	private int minZoom = 0;

	public DynmapIntegration(CheckersPlugin checkersPlugin, DynmapAPI dynmap) {
		this.plugin = checkersPlugin;
		this.dynmap = dynmap;
		this.mapi = this.dynmap.getMarkerAPI();

		processConfig();
	}

	public void processConfig() {
		enabled = plugin.getConfig().getBoolean("dynmap.enabled");
		priority = plugin.getConfig().getInt("dynmap.layer_priority");
		hideByDefault = plugin.getConfig().getBoolean("dynmap.hide_by_default");
		minZoom = plugin.getConfig().getInt("dynmap.min_zoom");

		if (active) {
			initIcon();
			initMarkerSet();
			initMarkers();
		}
	}

	public void triggerUpdate(Cuboid c) {
		LogUtils.finer("dynmap: triggering render of " + c);
		dynmap.triggerRenderOfVolume(c.getLowerNE(), c.getUpperSW());
	}

	/**
	 * Check if dynmap integration is active.  Integration is activated once all boards & games have
	 * been restored from disk, at the end of onEnable(), iff "dynmap.enabled" is true.  Integration
	 * will also be activated if the "dynmap.enabled" is changed to true later on, and will be deactivated
	 * if "dynmap.enabled" is set to false at any time.
	 *
	 * @return the active status
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Activate or deactivate dynmap integration.
	 *
	 * @param newActive
	 */
	public void setActive(boolean newActive) {
		if (active == newActive) {
			return;
		}
		LogUtils.fine("dynmap integration activation: " + active + " -> " + newActive);
		active = newActive;
		if (active)	{
			// activate
			initIcon();
			initMarkerSet();
			initMarkers();
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
		} else {
			// deactivate
			icon = null;
			if (markerSet != null) {
				markerSet.deleteMarkerSet();
				markerSet = null;
			}
			HandlerList.unregisterAll(this);
		}
	}

	/**
	 * Check if dynmap integration is enabled, which directly mirrors the "dynmap.enabled" config setting.
	 *
	 * @return the enabled status
	 */
	public boolean isEnabled() {
		return enabled;
	}

	@EventHandler
	public void onBoardCreated(CheckersBoardCreatedEvent event) {
		addMarker(event.getBoardView());
	}

	@EventHandler
	public void onBoardDeleted(CheckersBoardDeletedEvent event) {
		BoardView bv = event.getBoardView();
		Marker m = markerSet.findMarker(bv.getName());
		if (m != null) {
			m.deleteMarker();
		}
	}

	@EventHandler
	public void onGameCreated(CheckersGameCreatedEvent event) {
		BoardView bv = BoardViewManager.getManager().findBoardForGame(event.getGame());
		addMarker(bv);
	}

	@EventHandler
	public void onGameDeleted(CheckersGameDeletedEvent event) {
		BoardView bv = BoardViewManager.getManager().findBoardForGame(event.getGame());
		addMarker(bv);
	}

	@EventHandler
	public void onGameStateChanged(CheckersGameStateChangedEvent event) {
		BoardView bv = BoardViewManager.getManager().findBoardForGame(event.getGame());
		addMarker(bv);
	}

	private void initMarkerSet() {
		markerSet = mapi.getMarkerSet(MARKER_SET);
		if (markerSet == null) {
			markerSet = mapi.createMarkerSet(MARKER_SET, "Checkers Boards", null, false);
		} else {
			markerSet.setMarkerSetLabel("Checkers Boards");
		}
		if (markerSet == null) {
			LogUtils.warning("Error creating dynmap " + MARKER_SET + " markers");
			return;
		}

		markerSet.setLayerPriority(priority);
		markerSet.setHideByDefault(hideByDefault);
		markerSet.setMinZoom(minZoom);
	}

	private void initMarkers() {
		// remove any stale markers
		for (Marker m : markerSet.getMarkers()) {
			if (!BoardViewManager.getManager().boardViewExists(m.getMarkerID())) {
				m.deleteMarker();
			}
		}
		// add (or maybe update) a marker for each board
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			addMarker(bv);
		}
	}

	private void initIcon() {
		icon = mapi.getMarkerIcon(ICON_ID);
		if (icon == null) {
			InputStream stream = getClass().getResourceAsStream("/images/" + ICON);
			icon = mapi.createMarkerIcon(ICON_ID, ICON_ID, stream);
			if (icon == null) {
				LogUtils.warning("Error creating dynmap icon " + ICON_ID);
			}
		}
	}

	private Marker addMarker(BoardView bv) {
		Location loc = bv.getBoard().getBoardSquares().getCenter();
		String id = bv.getName();

		String label = colour("Checkers Board: ", "ffa") + colour(bv.getName(), "aa0");
		if (bv.getGame() != null) {
			CheckersGame game = bv.getGame();
			String plb = getPlayerString(game, PlayerColour.BLACK);
			String plw = getPlayerString(game, PlayerColour.WHITE);
			label += "<br>" + colour("Game: ", "ffa") + colour(bv.getGame().getName() + " (" + bv.getGame().getState() + ")", "aa0");
			label += "<br>" + colour("Players: ", "ffa") + colour(plb, "888") + colour(" vs. ", "ffa") + colour(plw, "fff");
		} else {
			label += "<br><em>" + colour("No game", "ffa") + "</em>";
		}

		Marker m = markerSet.findMarker(bv.getName());
		if (m == null) {
			m = markerSet.createMarker(id, label, true, bv.getWorldName(), loc.getX(), loc.getY(), loc.getZ(), icon, false);
		} else {
			m.setLocation(bv.getWorldName(), loc.getX(), loc.getY(), loc.getZ());
			m.setLabel(label, true);
			m.setMarkerIcon(icon);
		}
		return m;
	}

	private String getPlayerString(CheckersGame game, PlayerColour colour) {
		return game.getPlayer(colour) == null ? "?" : game.getPlayer(colour).getName().replace(CheckersAI.AI_PREFIX, "[AI]");
	}

	private String colour(String text, String col) {
		return "<span style='color: #" + col + ";'>" + text + "</span>";
	}
}
