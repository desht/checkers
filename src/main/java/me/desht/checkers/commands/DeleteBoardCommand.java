package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.Messages;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DeleteBoardCommand extends AbstractCheckersCommand {

	public DeleteBoardCommand() {
		super("checkers delete board", 1);
		setPermissionNode("checkers.commands.delete.board");
		setUsage("/<command> delete board <board-name>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
		String boardName = bv.getName();
		bv.deletePermanently();
		MiscUtil.statusMessage(sender, Messages.getString("Board.boardDeleted", boardName)); //$NON-NLS-1$
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getBoardCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
