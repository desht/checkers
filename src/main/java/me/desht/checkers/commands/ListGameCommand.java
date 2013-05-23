package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.PlayerColour;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListGameCommand extends AbstractCheckersCommand {

	private static final String TO_MOVE = ChatColor.GOLD + "\u261e " + ChatColor.RESET;

	public ListGameCommand() {
		super("checkers list game", 0, 1);
		setUsage("/checkers list game [<game-name>]");
		setPermissionNode("checkers.commands.list.game");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		CheckersGameManager manager = CheckersGameManager.getManager();

		if (manager.listGames().isEmpty()) {
			MiscUtil.statusMessage(sender, Messages.getString("Game.noCurrentGames"));
			return true;
		}

		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);

		if (args.length >= 1) {
			List<String> l = manager.getGame(args[0]).getGameDetail();
			pager.add(l);
		} else {
			for (CheckersGame game : manager.listGamesSorted()) {
				String name = game.getName();
				if (game == manager.getCurrentGame(sender.getName())) {
					name = ChatColor.BOLD + ChatColor.ITALIC.toString() + name + ChatColor.RESET;
				}
				String curMoveW = game.getPosition().getToMove() == PlayerColour.WHITE ? TO_MOVE : "";
				String curMoveB = game.getPosition().getToMove() == PlayerColour.BLACK ? TO_MOVE : "";
				String white = game.hasPlayer(PlayerColour.WHITE) ? game.getPlayer(PlayerColour.WHITE).getDisplayName() : "?";
				String black = game.hasPlayer(PlayerColour.BLACK) ? game.getPlayer(PlayerColour.BLACK).getDisplayName() : "?";
				String line = String.format(MessagePager.BULLET + "%s: %s%s (%s) v %s%s (%s)",
				                            name,
				                            curMoveB, black, PlayerColour.BLACK.getDisplayColour() + ChatColor.RESET,
				                            curMoveW, white, PlayerColour.WHITE.getDisplayColour() + ChatColor.RESET);
				if (!game.getInvited().isEmpty()) {
					line += Messages.getString("Game.invited", game.getInvited());
				}
				pager.add(line);
			}
		}
		pager.showPage();

		return true;
	}

}
