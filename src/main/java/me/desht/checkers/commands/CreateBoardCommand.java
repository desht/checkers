package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.responses.BoardCreationHandler;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class CreateBoardCommand extends AbstractCheckersCommand {

	public CreateBoardCommand() {
		super("checkers create board", 1);
		setUsage("/<command> create board <board-name> [-style <board-style>]");
		setOptions(new String[] { "style:s" });
		setPermissionNode("checkers.commands.create.board");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		if (args.length == 0 || args[0].startsWith("-")) {
			showUsage(sender);
			return true;
		}

		String name = args[0];

		String boardStyleName = getStringOption("style", "Standard");

		MiscUtil.statusMessage(sender, Messages.getString("Board.boardCreationPrompt", name)); //$NON-NLS-1$
		CheckersPlugin.getInstance().getResponseHandler().expect(sender.getName(), new BoardCreationHandler(name, boardStyleName));

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length >= 2 && args[args.length - 2].equals("-style")) {
			return getBoardStyleCompletions(plugin, sender, args[args.length - 1]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
