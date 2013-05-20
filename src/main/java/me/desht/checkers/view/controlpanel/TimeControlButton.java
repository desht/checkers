package me.desht.checkers.view.controlpanel;

import me.desht.checkers.TimeControlDefs;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.model.PlayerColour;

import org.bukkit.event.player.PlayerInteractEvent;

public class TimeControlButton extends AbstractSignButton {

	private final TimeControlDefs tcDefs;

	public TimeControlButton(ControlPanel panel) {
		super(panel, "timeControl", "tc", 3, 0);

		tcDefs = new TimeControlDefs();
	}

	public TimeControlDefs getTcDefs() {
		return tcDefs;
	}

	public void reloadDefs() {
		tcDefs.reload();
		repaint();
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		CheckersGame game = getGame();
		if (game == null) return;

		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			tcDefs.nextDef(); break;
		case RIGHT_CLICK_BLOCK:
			tcDefs.prevDef(); break;
		default:
			break;
		}
		game.setTimeControl(tcDefs.currentDef().getSpec());
		getPanel().updateClock(PlayerColour.WHITE, game.getTimeControl(PlayerColour.WHITE));
		getPanel().updateClock(PlayerColour.BLACK, game.getTimeControl(PlayerColour.BLACK));

		repaint();
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	public boolean isReactive() {
		return gameInState(GameState.SETTING_UP) && !getView().getLockTcSpec();
	}

	@Override
	protected String[] getCustomSignText() {
		String[] text = getSignText();

		String[] tcText = tcDefs.currentDef().getLabel();
		int start = tcText.length < 3 ? 2 : 1;

		for (int l = start, i = 0; l < 4; l++, i++) {
			text[l] = getIndicatorColour() + (i < tcText.length ? tcText[i] : "");
		}

		return text;
	}
}
