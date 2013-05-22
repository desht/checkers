package me.desht.checkers.commands;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.checkers.view.controlpanel.ControlPanel;
import me.desht.checkers.view.controlpanel.TimeControlButton;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class TimeControlCommand extends AbstractCheckersCommand {

	public TimeControlCommand() {
		super("checkers tc", 1, 1);
		addAlias("checkers timecontrol");
		setPermissionNode("checkers.commands.timecontrol");
		setUsage("/checkers tc <time-control-spec>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		String tcSpec = args[0];

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(sender.getName(), true);
		game.setTimeControl(tcSpec);
		BoardView bv = BoardViewManager.getManager().findBoardForGame(game);
		if (bv != null) {
			ControlPanel cp = bv.getControlPanel();
			cp.getTcDefs().addCustomSpec(tcSpec);
			cp.getButton(TimeControlButton.class).repaint();
			cp.updateClock(PlayerColour.WHITE, game.getTimeControl(PlayerColour.WHITE));
			cp.updateClock(PlayerColour.BLACK, game.getTimeControl(PlayerColour.BLACK));
		}
		game.alert(Messages.getString("Game.timeControlSet", tcSpec, game.getTimeControl(PlayerColour.WHITE).toString()));

		return true;
	}

}
