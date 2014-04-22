package me.desht.checkers.view.controlpanel;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.checkers.responses.DrawResponse;
import me.desht.checkers.responses.SwapResponse;
import me.desht.checkers.responses.UndoResponse;
import me.desht.checkers.responses.YesNoResponse;
import me.desht.dhutils.responsehandler.ResponseHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public abstract class YesNoButton extends AbstractSignButton {

	private final PlayerColour colour;
	private final boolean yesOrNo;

	public YesNoButton(ControlPanel panel, int x, int y, PlayerColour colour, boolean yesOrNo) {
		super(panel, yesOrNo ? "yesBtn" : "noBtn", null, x, y);
		this.colour = colour;
		this.yesOrNo = yesOrNo;
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		YesNoResponse.handleYesNoResponse(event.getPlayer(), yesOrNo);
	}

	@Override
	public boolean isEnabled() {
		return !getOfferText().isEmpty();
	}

	@Override
	public String[] getCustomSignText() {
		String[] text = getSignText();

		text[0] = getOfferText();

		return text;
	}

	private String getOfferText() {
		CheckersGame game = getGame();
		if (game == null) return "";

		CheckersPlayer player = game.getPlayer(colour);
		if (player == null || !player.isHuman())
			return "";

		Player p = Bukkit.getPlayer(player.getId());

		ResponseHandler rh = CheckersPlugin.getInstance().getResponseHandler();
		if (p == null) {
			// gone offline, perhaps?
			return "";
		} else if (rh.isExpecting(p, DrawResponse.class)) {
			return Messages.getString("ControlPanel.acceptDrawBtn");
		} else if (rh.isExpecting(p, SwapResponse.class)) {
			return Messages.getString("ControlPanel.acceptSwapBtn");
		} else if (rh.isExpecting(p, UndoResponse.class)) {
			return Messages.getString("ControlPanel.acceptUndoBtn");
		} else {
			return "";
		}
	}
}
