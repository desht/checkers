package me.desht.checkers.responses;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.dhutils.responsehandler.ExpectBase;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class InvitePlayer extends ExpectBase {
	private String inviteeName;

	public String getInviteeName() {
		return inviteeName;
	}

	public void setInviteeName(String playerName) {
		this.inviteeName = playerName;
	}

	@Override
	public void doResponse(final UUID playerId) {
		// Run this as a sync delayed task because we're not in the main thread at this point
		// (coming from the AsyncPlayerChatEvent handler)
		deferTask(playerId, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(playerId);
				if (player != null) {
					CheckersGame game = CheckersGameManager.getManager().getCurrentGame(player, true);
					game.invitePlayer(player, inviteeName);
				}
			}
		});
	}

}
