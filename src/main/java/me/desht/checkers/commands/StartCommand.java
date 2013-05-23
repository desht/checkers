package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class StartCommand extends AbstractCheckersCommand {

	public StartCommand() {
		super("checkers start", 0, 1);
		setPermissionNode("checkers.commands.start");
		setUsage("/checkers start [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) {
		notFromConsole(player);
		if (args.length >= 1) {
			CheckersGameManager.getManager().getGame(args[0]).start(player.getName());
		} else {
			CheckersGameManager.getManager().getCurrentGame(player.getName(), true).start(player.getName());
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getPlayerInGameCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

}
