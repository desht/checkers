package me.desht.checkers.view.controlpanel;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;

import org.bukkit.event.player.PlayerInteractEvent;

public class StartButton extends AbstractSignButton {

	public StartButton(ControlPanel panel) {
		super(panel, "startGameBtn", "setActive", 4, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		CheckersGame game = getGame();

		if (game != null) {
			game.start(event.getPlayer().getName());
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null && getGame().getState() == GameState.SETTING_UP;
	}
}
