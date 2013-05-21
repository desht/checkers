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
					game.alert(offerer, Messages.getString("Game.drawOfferAccepted", getPlayerName()));
					game.drawn(GameResult.DRAW);
				} else {
					game.alert(offerer, Messages.getString("Game.drawOfferDeclined", getPlayerName()));
					Player player = Bukkit.getPlayer(offeree);
					if (player != null) {
						MiscUtil.statusMessage(player, Messages.getString("Game.youDeclinedDrawOffer"));
					}
				}
			}
		});
	}
}
