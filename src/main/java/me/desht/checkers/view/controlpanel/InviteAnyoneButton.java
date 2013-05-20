package me.desht.checkers.view.controlpanel;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;

import org.bukkit.event.player.PlayerInteractEvent;

public class InviteAnyoneButton extends AbstractSignButton {

	public InviteAnyoneButton(ControlPanel panel) {
		super(panel, "inviteAnyoneBtn", "invite.anyone", 3, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		CheckersGame game = getGame();
		if (game != null) {
			game.inviteOpen(event.getPlayer().getName());
		}
	}

	@Override
	public boolean isEnabled() {
		CheckersGame game = getGame();
		return game != null && game.getState() == GameState.SETTING_UP && !game.isFull();
	}

}
