package me.desht.checkers.view.controlpanel;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;

import org.bukkit.event.player.PlayerInteractEvent;

public class OfferDrawButton extends AbstractSignButton {

	public OfferDrawButton(ControlPanel panel) {
		super(panel, "offerDrawBtn", "offer.draw", 5, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		CheckersGame game = getGame();
		if (game != null) {
			game.offerDraw(event.getPlayer().getName());
			getPanel().repaintControls();
		}
	}

	@Override
	public boolean isEnabled() {
		CheckersGame game = getGame();
		return game != null && game.getState() == GameState.RUNNING;
	}

}
