package me.desht.checkers.view.controlpanel;

import me.desht.checkers.model.PlayerColour;


public class WhiteYesButton extends YesNoButton {

	public WhiteYesButton(ControlPanel panel) {
		super(panel, 0, 0, PlayerColour.WHITE, true);
	}

}
