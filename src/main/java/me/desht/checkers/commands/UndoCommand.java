package me.desht.checkers.commands;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UndoCommand extends AbstractCheckersCommand {

	public UndoCommand() {
		super("checkers undo");
		setPermissionNode("checkers.commands.undo");
		setUsage("/<command> undo");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame((Player) sender, true);

		game.offerUndoMove(sender.getName());

		return true;
	}

}
