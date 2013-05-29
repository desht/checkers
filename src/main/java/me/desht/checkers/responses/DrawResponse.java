package me.desht.checkers.responses;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameResult;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DrawResponse extends YesNoResponse {

	public DrawResponse(CheckersGame game, String offerer) {
		super(game, offerer);
	}

	@Override
	public void doResponse(final String offeree) {

		deferTask(Bukkit.getPlayerExact(offerer), new Runnable() {
			@Override
			public void run() {
				if (accepted) {
					game.alert(offerer, Messages.getString("Offers.drawOfferAccepted", getPlayerName()));
					game.drawn(GameResult.DRAW_AGREED);
				} else {
					game.alert(offerer, Messages.getString("Offers.drawOfferDeclined", getPlayerName()));
					Player player = Bukkit.getPlayer(offeree);
					if (player != null) {
						MiscUtil.statusMessage(player, Messages.getString("Offers.youDeclinedDrawOffer"));
					}
				}
			}
		});
	}
}
