package me.desht.checkers;

import me.desht.checkers.TimeControl.ControlType;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.util.CheckersUtils;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class TwoPlayerClock implements ConfigurationSerializable {
	private TimeControl timeControl;
	private final long[] elapsed = new long[2];
	private final long[] remaining = new long[2];
	private PlayerColour active;
	private long lastTick;

	public TwoPlayerClock(String tcSpec) {
		this.active = PlayerColour.NONE;
		elapsed[0] = elapsed[1] = 0L;
		setTimeControl(tcSpec);
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("tc", timeControl.getSpec());
		map.put("elapsed0", elapsed[0]);
		map.put("elapsed1", elapsed[1]);
		map.put("remaining0", remaining[0]);
		map.put("remaining1", remaining[1]);
		return map;
	}

	public static TwoPlayerClock deserialize(Map<String,Object> map) {
		TwoPlayerClock clock = new TwoPlayerClock((String) map.get("tc"));
		clock.elapsed[0] = getLong(map.get("elapsed0"));
		clock.elapsed[1] = getLong(map.get("elapsed1"));
		clock.remaining[0] = getLong(map.get("remaining0"));
		clock.remaining[1] = getLong(map.get("remaining1"));

		return clock;
	}

	private static long getLong(Object o) {
		if (o instanceof Long) {
			return (Long) o;
		} else if (o instanceof Integer) {
			return Long.valueOf((Integer) o);
		} else {
			throw new IllegalArgumentException("invalid quantity: " + o);
		}
	}

	public TimeControl getTimeControl() {
		return timeControl;
	}

	public void setTimeControl(String tcSpec) {
		this.timeControl = new TimeControl(tcSpec);
		remaining[0] = remaining[1] = timeControl.getRemainingTime();
	}

	public void setActive(PlayerColour active) {
		if (isRunning() && timeControl.getControlType() == ControlType.MOVE_IN) {
			remaining[active.getIndex()] = timeControl.getRemainingTime();
		}
		this.active = active;
		lastTick = System.currentTimeMillis();
	}

	public void stop() {
		active = PlayerColour.NONE;
	}

	public boolean isRunning() {
		return active != PlayerColour.NONE;
	}

	public void tick() {
		if (isRunning()) {
			long now = System.currentTimeMillis();
			long delta = now - lastTick;
			lastTick = now;

			int idx = active.getIndex();
			elapsed[idx] += delta;

			if (timeControl.getControlType() != ControlType.NONE) {
				remaining[idx] = Math.max(0L, remaining[idx] - delta);
			}
		}
	}

	public PlayerColour getActive() {
		return active;
	}

	public long getElapsedTime(PlayerColour colour) {
		return elapsed[colour.getIndex()];
	}

	public long getRemainingTime(PlayerColour colour) {
		return remaining[colour.getIndex()];
	}

	public String getClockString(PlayerColour colour) {
		int idx = colour.getIndex();
		switch (timeControl.getControlType()) {
		case NONE:
			return CheckersUtils.milliSecondsToHMS(elapsed[idx]);
		default:
			return CheckersUtils.milliSecondsToHMS(remaining[idx]);
		}
	}
}
