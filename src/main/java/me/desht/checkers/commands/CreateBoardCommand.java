package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.CheckersValidate;
import me.desht.checkers.Messages;
import me.desht.checkers.responses.BoardCreationHandler;
import me.desht.checkers.view.BoardStyle;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class CreateBoardCommand extends AbstractCheckersCommand {

	public CreateBoardCommand() {
		super("checkers create board", 1);
		setUsage("/<command> create board <board-name> [-style <board-style>] [-size <size>]");
		setOptions(new String[] { "style:s", "size:i" });
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

		String boardStyleName = getStringOption("style", BoardStyle.DEFAULT_BOARD_STYLE);
		int size = getIntOption("size", 8);
		CheckersValidate.isTrue(size == 8 || size == 10 || size == 12, Messages.getString("Board.invalidSize", size));

		MiscUtil.statusMessage(sender, Messages.getString("Board.boardCreationPrompt", name));
		CheckersPlugin.getInstance().getResponseHandler().expect(sender.getName(), new BoardCreationHandler(name, boardStyleName, size));

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
