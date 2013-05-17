package me.desht.checkers.listeners;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.responses.BoardCreationHandler;
import me.desht.checkers.responses.InvitePlayer;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.responsehandler.ResponseHandler;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerListener extends CheckersBaseListener {

	public PlayerListener(CheckersPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		ResponseHandler resp = plugin.getResponseHandler();
		String playerName = event.getPlayer().getName();

		if (resp.isExpecting(playerName, InvitePlayer.class)) {
			// a left or right-click (even air, where the event is cancelled) cancels any pending player invite response
			resp.cancelAction(playerName, InvitePlayer.class);
			MiscUtil.alertMessage(event.getPlayer(), Messages.getString("Game.playerInviteCancelled"));
		} else if (resp.isExpecting(playerName, BoardCreationHandler.class)) {
			BoardCreationHandler boardCreation = resp.getAction(playerName, BoardCreationHandler.class);
			switch (event.getAction()) {
			case LEFT_CLICK_BLOCK:
				boardCreation.setLocation(event.getClickedBlock().getLocation());
				boardCreation.handleAction();
				break;
			case RIGHT_CLICK_BLOCK: case RIGHT_CLICK_AIR:
				MiscUtil.alertMessage(event.getPlayer(),  Messages.getString("Board.boardCreationCancelled"));
				boardCreation.cancelAction();
				break;
			default:
				break;
			}
			event.setCancelled(true);
		} else {
		
		}
	}
}
