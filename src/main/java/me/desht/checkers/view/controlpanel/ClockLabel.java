package me.desht.checkers.view.controlpanel;

import me.desht.checkers.Messages;
import me.desht.checkers.TimeControl;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.util.CheckersUtils;

public class ClockLabel extends AbstractSignLabel {

	private static final int[] xPos = new int[2];
	static {
		xPos[PlayerColour.WHITE.getIndex()] = 5;
		xPos[PlayerColour.BLACK.getIndex()] = 2;
	}

	private final PlayerColour colour;
	private String timeStr = CheckersUtils.milliSecondsToHMS(0);

	public ClockLabel(ControlPanel panel, PlayerColour colour) {
		super(panel, colour.getColour(), xPos[colour.getIndex()], 1);

		this.colour = colour;
	}


	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	public void setLabel(String timeStr) {
		if (timeStr == null) {
			timeStr = CheckersUtils.milliSecondsToHMS(0);
		}
		this.timeStr = timeStr;
	}

	@Override
	public String[] getCustomSignText() {
		String[] res = new String[] { "", "", "", "" };

		res[0] = colour.getColour();
		res[2] = getIndicatorColour() + timeStr;

		if (getGame() == null) {
			res[3] = "";
		} else {
			TimeControl timeControl = getView().getGame().getClock().getTimeControl();
			switch (timeControl.getControlType()) {
			case NONE:
				res[3] = Messages.getString("ControlPanel.timeElapsed");
				break;
			default:
				res[3] = Messages.getString("ControlPanel.timeRemaining");
				break;
			}
		}

		return res;
	}

}
