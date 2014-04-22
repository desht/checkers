package me.desht.checkers.responses;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameResult;
import me.desht.checkers.model.PlayerColour;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DrawResponse extends YesNoResponse {

	public DrawResponse(CheckersGame game, PlayerColour offererColour) {
		super(game, offererColour);
	}

	@Override
	public void doResponse(final UUID offereeId) {
		final UUID offererId = UUID.fromString(game.getPlayer(offererColour).getId());
		deferTask(offererId, new Runnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(offereeId);
				if (player != null) {
					if (accepted) {
						game.alert(offererId, Messages.getString("Offers.drawOfferAccepted", getPlayerId()));
						game.drawn(GameResult.DRAW_AGREED);
					} else {
						game.alert(offererId, Messages.getString("Offers.drawOfferDeclined", getPlayerId()));
						MiscUtil.statusMessage(player, Messages.getString("Offers.youDeclinedDrawOffer"));
					}
				}
			}
		});
	}
}
