package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.view.BoardStyle;
import me.desht.dhutils.MessagePager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListStylesCommand extends AbstractCheckersCommand {

	public ListStylesCommand() {
		super("checkers list style");
		setPermissionNode("checkers.commands.list.style");
		setUsage("/checkers list style");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

		MessagePager p = MessagePager.getPager(sender).clear().setParseColours(true);

		List<BoardStyle> l = getAllBoardStyles();
		p.add(ChatColor.AQUA.toString() + l.size() + " board styles:");
		for (BoardStyle boardStyle : l) {
			int sq = boardStyle.getSquareSize();
			int h = boardStyle.getHeight();
			int fw = boardStyle.getFrameWidth();
			String custom = boardStyle.isCustom() ? ChatColor.GOLD + " [c]" : "";
			p.add(MessagePager.BULLET + String.format("%s%s&e: sq=%d, h=%d, f=%d, (%dx%dx%d)",
			                                          boardStyle.getName(), custom, sq, h, fw,
			                                          (sq * 8) + (fw * 2), (sq * 8) + (fw * 2), h));
		}

		p.showPage();
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		showUsage(sender);
		return noCompletions(sender);
	}
}
