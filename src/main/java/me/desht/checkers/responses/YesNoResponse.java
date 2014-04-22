package me.desht.checkers.responses;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.dhutils.responsehandler.ExpectBase;
import me.desht.dhutils.responsehandler.ResponseHandler;
import org.bukkit.entity.Player;

public abstract class YesNoResponse extends ExpectBase {

	protected final CheckersGame game;
	protected final PlayerColour offererColour;
	protected boolean accepted;

	public YesNoResponse(CheckersGame game, PlayerColour offererColour) {
		this.game = game;
		this.offererColour = offererColour;
	}

	public void setResponse(boolean accepted) {
		this.accepted = accepted;
	}

	public CheckersGame getGame() {
		return game;
	}

	/**
	 * The given player has just typed "yes" or "no" (or used a Yes/No button).  Work out to what offer they're
	 * responding, and carry out the associated action.
	 *
	 * @param player the player
	 * @param isAccepted true if accepted, false if declined
	 * @throws CheckersException
	 */
	public static void handleYesNoResponse(Player player, boolean isAccepted) {
		ResponseHandler respHandler = CheckersPlugin.getInstance().getResponseHandler();

		// TODO: code smell!
		Class<? extends YesNoResponse> c;
		if (respHandler.isExpecting(player, DrawResponse.class)) {
			c = DrawResponse.class;
		} else if (respHandler.isExpecting(player, SwapResponse.class)) {
			c = SwapResponse.class;
		} else if (respHandler.isExpecting(player, UndoResponse.class)) {
			c = UndoResponse.class;
		} else {
			return;
		}

		YesNoResponse response = respHandler.getAction(player, c);
		response.setResponse(isAccepted);
		response.handleAction(player);
	}

}
