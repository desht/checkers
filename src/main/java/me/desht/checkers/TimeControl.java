package me.desht.checkers;


public class TimeControl {
	public enum ControlType { NONE, MOVE_IN, GAME_IN }

	private final String spec;
	private final ControlType controlType;
	private final long totalTime;	// milliseconds
	private long remainingTime;		// milliseconds

	public TimeControl(String specStr) {
		spec = specStr.toUpperCase();
		if (spec.isEmpty() || spec.startsWith("N")) {
			totalTime = 0L;
			controlType = ControlType.NONE;
		} else if (spec.startsWith("G/")) {
			// game in - minutes
			int t = Integer.parseInt(spec.substring(2));
			remainingTime = totalTime = t * 60000;
			controlType = ControlType.GAME_IN;
		} else if (spec.startsWith("M/")) {
			// move in - seconds
			int t = Integer.parseInt(spec.substring(2));
			remainingTime = totalTime = t * 1000;
			controlType = ControlType.MOVE_IN;
		} else {
			throw new CheckersException("Invalid time control specification: " + spec);
		}
	}

	public ControlType getControlType() {
		return controlType;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public String getSpec() {
		return spec;
	}

	/**
	 * Get the remaining time for this time control.
	 *
	 * @return the remaining time, in milliseconds
	 */
	public long getRemainingTime() {
		return controlType == ControlType.NONE ? Long.MAX_VALUE : remainingTime;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO: i18n needed here
		switch (controlType) {
		case MOVE_IN:
			return "Move in " + (totalTime / 1000) + "s";
		case GAME_IN:
			return "Game in " + (totalTime / 60000) + "m";
		case NONE:
			return "None";
		default:
			return "???";
		}
	}

}
