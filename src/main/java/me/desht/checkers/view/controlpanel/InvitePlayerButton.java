package me.desht.checkers.view.controlpanel;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.responses.InvitePlayer;
import me.desht.dhutils.MiscUtil;

import org.bukkit.event.player.PlayerInteractEvent;

public class InvitePlayerButton extends AbstractSignButton {

	public InvitePlayerButton(ControlPanel panel) {
		super(panel, "invitePlayerBtn", "invite", 2, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		CheckersGame game = getGame();

		if (game != null && !game.isFull()) {
			CheckersPlugin.getInstance().getResponseHandler().expect(event.getPlayer(), new InvitePlayer());
			MiscUtil.statusMessage(event.getPlayer(), Messages.getString("ControlPanel.invitePrompt"));
		}
	}

	@Override
	public boolean isEnabled() {
		CheckersGame game = getGame();

		return game != null && game.getState() == GameState.SETTING_UP && !game.isFull();
	}

}
