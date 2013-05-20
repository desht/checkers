package me.desht.checkers.view.controlpanel;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;

import me.desht.checkers.view.BoardView;
import me.desht.dhutils.block.MassBlockUpdate;
import me.desht.dhutils.cuboid.Cuboid;

public class ControlPanel {
	private final BoardView view;

	public ControlPanel(BoardView view) {
		this.view = view;
	}

	/**
	 * @return the view
	 */
	public BoardView getView() {
		return view;
	}

	public void repaint(MassBlockUpdate mbu) {
		// TODO Auto-generated method stub
	}

	public void repaintControls() {
		// TODO Auto-generated method stub
	}

	public void removeSigns() {
		// TODO Auto-generated method stub

	}

	public Location getTeleportInLocation() {
		// TODO Auto-generated method stub
		return view.getBoard().getA1Center().getLocation();
	}

	public boolean isButton(Location location) {
		// TODO Auto-generated method stub
		return false;
	}

	public void handleButtonClick(PlayerInteractEvent event) {
		// TODO Auto-generated method stub
		
	}

	public Cuboid getPanelBlocks() {
		// TODO Auto-generated method stub
		return null;
	}

	public void repaintClocks() {
		// TODO Auto-generated method stub
		
	}
}
