package me.desht.checkers.view.controlpanel;

import me.desht.checkers.Messages;
import me.desht.checkers.TimeControl;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.util.CheckersUtils;

public class ClockLabel extends AbstractSignLabel {

	private static int[] xPos = new int[2];
	static {
		xPos[PlayerColour.WHITE.getIndex()] = 2;
		xPos[PlayerColour.BLACK.getIndex()] = 5;
	};

	private final PlayerColour colour;
	private TimeControl timeControl;

	public ClockLabel(ControlPanel panel, PlayerColour colour) {
		super(panel, colour.getColour(), xPos[colour.getIndex()], 1);

		this.colour = colour;
		timeControl = null;
	}

	public TimeControl getTimeControl() {
		return timeControl;
	}

	public void setTimeControl(TimeControl timeControl) {
		this.timeControl = timeControl;
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	public String[] getCustomSignText() {
		String[] res = new String[] { "", "", "", "" };

		res[0] = colour.getColour();

		if (timeControl == null) {
			res[2] = getIndicatorColour() + CheckersUtils.milliSecondsToHMS(0);
		} else {
			res[2] = getIndicatorColour() + timeControl.getClockString();
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
