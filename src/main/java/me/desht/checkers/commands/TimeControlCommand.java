package me.desht.checkers.commands;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class TimeControlCommand extends AbstractCheckersCommand {

	public TimeControlCommand() {
		super("checkers tc", 1, 1);
		addAlias("checkers timecontrol");
		setPermissionNode("checkers.commands.timecontrol");
		setUsage("/<command> tc <time-control-spec>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		String tcSpec = args[0];

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(sender.getName(), true);
		game.setTimeControl(tcSpec);
		game.alert(Messages.getString("Game.timeControlSet", tcSpec, game.getTimeControl().toString()));

		return true;
	}

}
