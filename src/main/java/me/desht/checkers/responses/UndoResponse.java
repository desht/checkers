package me.desht.checkers.responses;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UndoResponse extends YesNoResponse {

	public UndoResponse(CheckersGame game, PlayerColour offererColour) {
		super(game, offererColour);
	}

	@Override
	public void doResponse(final UUID playerId) {
		final UUID offererId = UUID.fromString(game.getPlayer(offererColour).getId());
		deferTask(offererId, new Runnable() {

			@Override
			public void run() {
				if (accepted) {
					game.alert(offererId, Messages.getString("Offers.undoOfferAccepted", getPlayerId()));
					game.undoMove(offererId.toString());
				} else {
					game.alert(offererId, Messages.getString("Offers.undoOfferDeclined", getPlayerId()));
					Player player = Bukkit.getPlayer(playerId);
					if (player != null) {
						MiscUtil.statusMessage(player, Messages.getString("Offers.youDeclinedUndoOffer"));
					}
				}
			}
		});
	}

}
