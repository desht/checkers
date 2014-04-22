package me.desht.checkers.view.controlpanel;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;

import org.bukkit.event.player.PlayerInteractEvent;

public class UndoButton extends AbstractSignButton {

	public UndoButton(ControlPanel panel) {
		super(panel, "undoBtn", "undo", 7, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		CheckersGame game = getGame();
		if (game != null) {
			game.offerUndoMove(event.getPlayer().getUniqueId().toString());
			getPanel().repaintControls();
		}
	}

	@Override
	public boolean isEnabled() {
		return gameInState(GameState.RUNNING);
	}

}
