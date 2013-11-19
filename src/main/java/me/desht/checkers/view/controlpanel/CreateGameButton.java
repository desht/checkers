package me.desht.checkers.view.controlpanel;

import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.PlayerColour;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CreateGameButton extends AbstractSignButton {

	private PlayerColour colour = PlayerColour.BLACK;

	public CreateGameButton(ControlPanel panel) {
		super(panel, "createGameBtn", "create.game", 1, 2);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			String ruleId = getPanel().getButton(SelectRulesButton.class).getSelectedRuleset();
			CheckersGameManager.getManager().createGame(event.getPlayer(), null, getView(), colour, ruleId);
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			// cycle between "White" and "Black"
			colour = colour.getOtherColour();
			repaint();
		}
	}

	@Override
	public boolean isEnabled() {
		return getGame() == null && !getPanel().getButton(SelectRulesButton.class).getSelectedRuleset().isEmpty();
	}

	@Override
	protected String[] getCustomSignText() {
		String[] res = getSignText();

		res[3] = getIndicatorColour() + colour.getColour();

		return res;
	}
}
