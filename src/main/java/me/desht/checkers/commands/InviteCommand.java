package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class InviteCommand extends AbstractCheckersCommand {

	public InviteCommand() {
		super("checkers invite", 0, 1);
		setPermissionNode("checkers.commands.invite");
		setUsage("/checkers invite [<player-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(sender.getName(), true);
		String invitee = args.length > 0 ? args[0] : null;
		game.invitePlayer(sender.getName(), invitee);

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return null; // TODO: getPlayerCompletions(plugin, sender, args[0], false);
		default:
			return noCompletions(sender);
		}
	}
}
