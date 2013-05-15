package me.desht.checkers.view.controlpanel;

import me.desht.checkers.view.BoardView;
import me.desht.dhutils.block.MassBlockUpdate;

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
}
