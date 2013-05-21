package me.desht.checkers.commands;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.view.BoardStyle;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BoardSaveCommand extends AbstractCheckersCommand {

	public BoardSaveCommand() {
		super("checkers board save", 0, 1);
		setPermissionNode("checkers.commands.board.save");
		setUsage("/checkers board save [<new-style-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		BoardView bv = BoardViewManager.getManager().partOfBoard(((Player)sender).getLocation());
		if (bv == null) {
			throw new CheckersException(Messages.getString("Board.notOnBoard"));
		}
		BoardStyle style = bv.getBoard().getBoardStyle();

		String newStyleName = args.length > 0 ? args[0] : style.getName();

		BoardStyle newStyle = style.saveStyle(newStyleName);
		bv.getBoard().setBoardStyle(newStyle);
		bv.save();

		MiscUtil.statusMessage(sender, Messages.getString("Board.boardStyleSaved", bv.getName(), newStyleName));

		return true;
	}

}
