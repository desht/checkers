package me.desht.checkers.commands;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.Checkers;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class MoveCommand extends AbstractCheckersCommand {

	public MoveCommand() {
		super("checkers move", 2, 2);
		setPermissionNode("checkers.commands.move");
		setUsage("/checkers move <from> <to>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(sender.getName(), true);

		int from, to;
		try {
			from = Integer.parseInt(args[0]);
			to = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new CheckersException(Messages.getString("Misc.invalidNumeric", args[0], args[1]));
		}

		int fromSqi = Checkers.checkersNotationToSqi(from);
		int toSqi = Checkers.checkersNotationToSqi(to);
		game.doMove(sender.getName(), fromSqi, toSqi);

		MiscUtil.statusMessage(sender, Messages.getString("Game.youPlayed", from, to));

		return true;
	}

}
