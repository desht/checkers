package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.Messages;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListBoardCommand extends AbstractCheckersCommand {

	public ListBoardCommand() {
		super("checkers list board", 0, 1);
		setPermissionNode("checkers.commands.list.board");
		setUsage("/<command> list board");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (BoardViewManager.getManager().listBoardViews().isEmpty()) {
			MiscUtil.statusMessage(sender, Messages.getString("Board.noBoards"));
			return true;
		}

		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
		if (args.length >= 1) {
			List<String> l = BoardViewManager.getManager().getBoardView(args[0]).getBoardDetail();
			pager.add(l);
		} else {
			for (BoardView bv : BoardViewManager.getManager().listBoardViewsSorted()) {
				String gameName = bv.getGame() != null ? bv.getGame().getName() : Messages.getString("Game.noGame");
				Location a1 = bv.getBoard().getA1Center().getLocation();
				String size = bv.getBoard().getSize() + "x" + bv.getBoard().getSize();
				pager.add(MessagePager.BULLET + Messages.getString("Board.boardList", bv.getName(), MiscUtil.formatLocation(a1),
				                                                   size, bv.getBoard().getBoardStyle().getName(), gameName));
			}
		}
		pager.showPage();
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
