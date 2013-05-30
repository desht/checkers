package me.desht.checkers.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.event.CheckersBoardModifiedEvent;
import me.desht.checkers.view.BoardStyle;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.AttributeCollection;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BoardSetCommand extends AbstractCheckersCommand {

	public BoardSetCommand() {
		super("checkers board set", 2);
		setPermissionNode("checkers.commands.board.set");
		setUsage("/<command> board set <attribute> <value> [<attribute> <value>...]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		if (args.length % 2 != 0) {
			showUsage(sender);
			return true;
		}

		BoardView bv = BoardViewManager.getManager().partOfBoard(((Player)sender).getLocation());
		if (bv == null) {
			throw new CheckersException(Messages.getString("Board.notOnBoard"));
		}
		BoardStyle style = bv.getBoard().getBoardStyle();
		AttributeCollection viewAttrs = bv.getAttributes();
		AttributeCollection styleAttrs = style.getAttributes();
		boolean styleHasChanged = false;
		Set<String> changedAttrs = new HashSet<String>();

		for (int i = 0; i < args.length; i += 2) {
			String attr = args[i];
			String val = args[i + 1];

			if (styleAttrs.contains(attr)) {
				styleAttrs.set(attr, val);
				styleHasChanged = true;
			} else if (viewAttrs.contains(attr)) {
				viewAttrs.set(attr, val);
			} else {
				throw new CheckersException("Unknown attribute '" + attr + "'.");
			}

			changedAttrs.add(attr);
		}

		MiscUtil.statusMessage(sender, Messages.getString("Board.boardStyleChanged", bv.getName()));
		if (styleHasChanged) {
			MiscUtil.statusMessage(sender, Messages.getString("Board.boardStyleSuggestSave"));
			bv.repaint();
		} else if (bv.getBoard().isRedrawNeeded()) {
			bv.repaint();
		} else {
			bv.getControlPanel().repaint(null);
		}

		bv.save();

		Bukkit.getPluginManager().callEvent(new CheckersBoardModifiedEvent(bv, changedAttrs));

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		BoardView bv = BoardViewManager.getManager().partOfBoard(((Player)sender).getLocation());
		int l = args.length;
		AttributeCollection styleAttrs = bv.getBoard().getBoardStyle().getAttributes();
		AttributeCollection viewAttrs = bv.getAttributes();
		if (args.length % 2 == 1) {
			// provide attribute completions
			List<String> attrs = new ArrayList<String>(styleAttrs.listAttributeKeys(false));
			attrs.addAll(new ArrayList<String>(viewAttrs.listAttributeKeys(false)));
			return filterPrefix(sender, attrs, args[l - 1]);
		} else {
			// provide value completions for last attribute
			String attr = args[l - 2];
			String desc = styleAttrs.contains(attr) ? styleAttrs.getDescription(attr) : viewAttrs.getDescription(attr);
			Object o = styleAttrs.contains(attr) ? styleAttrs.get(attr) : viewAttrs.get(attr);
			if (!desc.isEmpty())
				desc = ChatColor.GRAY.toString() + ChatColor.ITALIC + " [" + desc + "]";
			return getConfigValueCompletions(sender, attr, o, desc, args[l - 1]);
		}
	}
}
