package me.desht.checkers.view.controlpanel;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.GameRules;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class SelectRulesButton extends AbstractSignButton {
	private final List<GameRules> matchingRules;
	private int ruleIdx;

	public SelectRulesButton(ControlPanel panel) {
		super(panel, "selectRulesBtn", "rules", 1, 1);

		matchingRules = GameRules.getMatchingRules(getView().getBoard().getSize());
		ruleIdx = 0;
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		if (matchingRules.isEmpty()) {
			return;
		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ruleIdx = (ruleIdx + 1) % matchingRules.size();
			if (getGame() != null && getGame().getState() == CheckersGame.GameState.SETTING_UP) {
				getGame().getPosition().setRules(getSelectedRuleset());
			} else if (getGame() == null) {
				getPanel().getButton(CreateGameButton.class).setColour(matchingRules.get(ruleIdx).getWhoMovesFirst());
			}
			repaint();
		} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (!matchingRules.isEmpty()) {
				String id = matchingRules.get(ruleIdx).getId();
				List<String> summary = Messages.getStringList("Rules." + id + ".summary");
				String label = Messages.getString("Rules." + id + ".label").replace(';', ' ');
				MiscUtil.statusMessage(event.getPlayer(), Messages.getString("Rules.headerMessage", label));
				for (String s : summary) {
					MiscUtil.statusMessage(event.getPlayer(), MessagePager.BULLET + s);
				}
			}
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isReactive() {
		return (getGame() == null || getGame().getState() == CheckersGame.GameState.SETTING_UP) && !matchingRules.isEmpty();
	}

	@Override
	public boolean isReactive(PlayerInteractEvent event) {
		// allow button to be left-clicked to get rules summary at any time
		return event.getAction() == Action.LEFT_CLICK_BLOCK;
	}

	@Override
	protected String[] getCustomSignText() {
		String[] res = getSignText();
		if (matchingRules.isEmpty()) {
			res[2] = "-";
		} else {
			String id = matchingRules.get(ruleIdx).getId();
			String[] label = Messages.getString("Rules." + id + ".label").split(";");
			int j = 4 - label.length;
			for (int i = 0; i < label.length && i < 3; i++) {
				res[i + j] = getIndicatorColour() + MiscUtil.parseColourSpec(label[i]);
			}
		}
		return res;
	}

	public String getSelectedRuleset() {
		return matchingRules.isEmpty() ? "" : matchingRules.get(ruleIdx).getId();
	}

	public void setSelectedRuleset(String ruleId) {
		boolean found = false;
		for (int i = 0; i < matchingRules.size(); i++) {
			if (ruleId.equals(matchingRules.get(i).getId())) {
				found = true;
				ruleIdx = i;
				break;
			}
		}
		if (found) {
			if (getGame() != null && getGame().getState() == CheckersGame.GameState.SETTING_UP) {
				getGame().getPosition().setRules(getSelectedRuleset());
			}
			repaint();
		} else {
			throw new CheckersException(Messages.getString("Rules.unknownRules", ruleId));
		}
	}
}
