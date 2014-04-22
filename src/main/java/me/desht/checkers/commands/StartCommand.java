package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class StartCommand extends AbstractCheckersCommand {

	public StartCommand() {
		super("checkers start", 0, 1);
		setPermissionNode("checkers.commands.start");
		setUsage("/<command> start [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		Player player = (Player) sender;
		if (args.length >= 1) {
			CheckersGameManager.getManager().getGame(args[0]).start(player.getUniqueId().toString());
		} else {
			CheckersGameManager.getManager().getCurrentGame((Player) sender, true).start(player.getUniqueId().toString());
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1 && sender instanceof Player) {
			return getPlayerInGameCompletions(plugin, (Player) sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

}
