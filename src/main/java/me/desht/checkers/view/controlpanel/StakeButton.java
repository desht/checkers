package me.desht.checkers.view.controlpanel;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.util.CheckersUtils;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class StakeButton extends AbstractSignButton {

	public StakeButton(ControlPanel panel) {
		super(panel, "stakeBtn", "stake", 7, 1);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		double stakeIncr;
		if (event.getPlayer().isSneaking()) {
			stakeIncr = CheckersPlugin.getInstance().getConfig().getDouble("stake.smallIncrement");
		} else {
			stakeIncr = CheckersPlugin.getInstance().getConfig().getDouble("stake.largeIncrement");
		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			stakeIncr = -stakeIncr;
		}

		getGame().adjustStake(event.getPlayer().getName(), stakeIncr);
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null;
	}

	@Override
	public boolean isReactive() {
		CheckersGame game = getGame();
		return game != null && !getView().getLockStake() && game.getState() == GameState.SETTING_UP && !game.isFull();

	}

	@Override
	protected String[] getCustomSignText() {
		String[] res = getSignText();

		CheckersGame game = getGame();
		double stake = game == null ? getView().getDefaultStake() : game.getStake();
		String[] s =  CheckersUtils.formatStakeStr(stake).split(" ", 2);
		res[2] = getIndicatorColour() + s[0];
		res[3] = s.length > 1 ? getIndicatorColour() + s[1] : "";

		return res;
	}
}
