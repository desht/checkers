package me.desht.checkers.commands;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RedrawCommand extends AbstractCheckersCommand {

	public RedrawCommand() {
		super("checkers redraw", 0, 1);
		setPermissionNode("checkers.commands.redraw");
		setUsage("/<command> redraw [<board-name>]");
		setOptions("all");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length >= 1) {
			// redraw named board
			BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
			repaintBoard(bv);
			MiscUtil.statusMessage(sender, Messages.getString("Board.boardRedrawn", bv.getName())); //$NON-NLS-1$
		} else if (getBooleanOption("all")) {
			// redraw ALL boards
			for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
				repaintBoard(bv);
			}
			MiscUtil.statusMessage(sender, Messages.getString("Board.allBoardsRedrawn")); //$NON-NLS-1$
		} else {
			// redraw board caller is standing on, if any
			notFromConsole(sender);
			Player player = (Player) sender;
			BoardView bv = BoardViewManager.getManager().partOfBoard(player.getLocation());
			if (bv == null) {
				throw new CheckersException(Messages.getString("Board.notOnBoard"));
			}
			repaintBoard(bv);
			MiscUtil.statusMessage(sender, Messages.getString("Board.boardRedrawn", bv.getName())); //$NON-NLS-1$
		}
		return true;
	}

	private void repaintBoard(BoardView bv) {
		bv.getBoard().reloadBoardStyle();
		bv.repaint();
	}
}
