package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ResignCommand extends AbstractCheckersCommand {

	public ResignCommand() {
		super("checkers resign", 0, 1);
		setPermissionNode("checkers.commands.resign");
		setUsage("/<command> resign [<game>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) {
		notFromConsole(player);

		CheckersGame game = null;
		if (args.length >= 1) {
			game = CheckersGameManager.getManager().getGame(args[0]);
		} else {
			game = CheckersGameManager.getManager().getCurrentGame(player.getName(), true);
		}

		if (game != null) {
			game.resign(player.getName());
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
