package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DeleteGameCommand extends AbstractCheckersCommand {

	public DeleteGameCommand() {
		super("checkers delete game", 1, 1);
		setUsage("/checkers delete game <game-name>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String gameName = args[0];
		CheckersGame game = CheckersGameManager.getManager().getGame(gameName);
		gameName = game.getName();

		// bypass permission check if player is deleting their own game and it's still in setup phase
		if (!game.playerAllowedToDelete(sender.getName())) {
			PermissionUtils.requirePerms(sender, "checkers.commands.delete.game");
		}

		game.alert(Messages.getString("Game.gameDeletedAlert", sender.getName()));
		game.deletePermanently();
		MiscUtil.statusMessage(sender, Messages.getString("Game.gameDeleted", gameName));

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getGameCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
