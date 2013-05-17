package me.desht.checkers.responses;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.dhutils.responsehandler.ExpectBase;

import org.bukkit.Bukkit;

public class InvitePlayer extends ExpectBase {
	private String inviteeName;

	public String getInviteeName() {
		return inviteeName;
	}

	public void setInviteeName(String playerName) {
		this.inviteeName = playerName;
	}

	@Override
	public void doResponse(final String playerName) {
		// Run this as a sync delayed task because we're not in the main thread at this point
		// (coming from the AsyncPlayerChatEvent handler)
		deferTask(Bukkit.getPlayerExact(playerName), new Runnable() {
			@Override
			public void run() {
				CheckersGame game = CheckersGameManager.getManager().getCurrentGame(playerName, true);
				game.invitePlayer(playerName, inviteeName);
			}
		});
	}

}
