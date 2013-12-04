package me.desht.checkers.responses;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.game.CheckersGame;
import me.desht.dhutils.responsehandler.ExpectBase;
import me.desht.dhutils.responsehandler.ResponseHandler;

import org.bukkit.entity.Player;

public abstract class YesNoResponse extends ExpectBase {

	protected final CheckersGame game;
	protected final String offerer;
	protected boolean accepted;

	public YesNoResponse(CheckersGame game, String offerer) {
		this.game = game;
		this.offerer = offerer;
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
	 * @param player
	 * @param isAccepted
	 * @throws CheckersException
	 */
	public static void handleYesNoResponse(Player player, boolean isAccepted) {
		ResponseHandler respHandler = CheckersPlugin.getInstance().getResponseHandler();

		// TODO: code smell!
		Class<? extends YesNoResponse> c;
		if (respHandler.isExpecting(player.getName(), DrawResponse.class)) {
			c = DrawResponse.class;
		} else if (respHandler.isExpecting(player.getName(), SwapResponse.class)) {
			c = SwapResponse.class;
		} else if (respHandler.isExpecting(player.getName(), UndoResponse.class)) {
			c = UndoResponse.class;
		} else {
			return;
		}

		YesNoResponse response = respHandler.getAction(player.getName(), c);
		response.setResponse(isAccepted);
		response.handleAction();
	}

}
