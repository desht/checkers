package me.desht.checkers.view.controlpanel;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;

import org.bukkit.event.player.PlayerInteractEvent;

public class ResignButton extends AbstractSignButton {

	public ResignButton(ControlPanel panel) {
		super(panel, "resignBtn", "resign", 6, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		CheckersGame game = getGame();

		if (game != null) {
			game.resign(event.getPlayer().getName());
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() != null && getGame().getState() == GameState.RUNNING;
	}

}
