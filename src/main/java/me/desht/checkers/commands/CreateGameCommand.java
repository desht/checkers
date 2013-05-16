package me.desht.checkers.commands;

import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.PlayerColour;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CreateGameCommand extends AbstractCheckersCommand {

	public CreateGameCommand() {
		super("checkers create game");
		setPermissionNode("checkers.commands.create.game");
		setUsage("/checkers create game [-black] [<game-name>] [<board-name>]");
		setOptions("black");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		String gameName = args.length >= 1 ? args[0] : null;
		String boardName = args.length >= 2 ? args[1] : null;

		CheckersGameManager.getManager().createGame((Player) sender, gameName, boardName, getBooleanOption("black") ? PlayerColour.BLACK : PlayerColour.WHITE);

		return true;
	}

}
