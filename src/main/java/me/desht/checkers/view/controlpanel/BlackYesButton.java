package me.desht.checkers.view.controlpanel;

import me.desht.checkers.model.PlayerColour;

public class BlackYesButton extends YesNoButton {

	public BlackYesButton(ControlPanel panel) {
		super(panel, 6, 0, PlayerColour.BLACK, true);
	}

}
