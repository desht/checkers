package me.desht.checkers.responses;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class UndoResponse extends YesNoResponse {

	public UndoResponse(CheckersGame game, String offerer) {
		super(game, offerer);
	}

	@Override
	public void doResponse(final String playerName) {
		deferTask(Bukkit.getPlayer(offerer), new Runnable() {

			@Override
			public void run() {
				if (accepted) {
					game.alert(offerer, Messages.getString("Offers.undoOfferAccepted", getPlayerName()));
					game.undoMove(offerer);
				} else {
					game.alert(offerer, Messages.getString("Offers.undoOfferDeclined", getPlayerName()));
					Player player = Bukkit.getPlayer(playerName);
					if (player != null) {
						MiscUtil.statusMessage(player, Messages.getString("Offers.youDeclinedUndoOffer"));
					}
				}
			}
		});
	}

}
