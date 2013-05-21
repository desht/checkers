package me.desht.checkers.commands;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TeleportCommand extends AbstractCheckersCommand {

	public TeleportCommand() {
		super("checkers tp", 0, 2);
		addAlias("checkers teleport");
		setPermissionNode("chesscraft.commands.teleport");
		setUsage(new String[] {
				"/checkers tp [<game-name>]",
				"/checkers tp -b <board-name>",
				"/checkers tp -set [<board-name>]",
				"/checkers tp -clear [<board-name>]",
				"/checkers tp -list"
		});
		setOptions(new String[] { "b", "set", "clear", "list" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (getBooleanOption("list")) {
			showTeleportDests(sender);
			return true;
		}
		notFromConsole(sender);

		if (!plugin.getConfig().getBoolean("teleporting")) {
			throw new CheckersException(Messages.getString("Misc.noTeleporting"));
		}

		Player player = (Player)sender;

		if (getBooleanOption("set")) {
			PermissionUtils.requirePerms(sender, "chesscraft.commands.teleport.set");
			if (args.length == 0) {
				// set global teleport-out location
				BoardViewManager.getManager().setGlobalTeleportOutDest(player.getLocation());
				MiscUtil.statusMessage(player, Messages.getString("Misc.globalTeleportSet")); 
			} else {
				// set per-board teleport-out location
				BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
				bv.setTeleportOutDestination(player.getLocation());
				MiscUtil.statusMessage(player, Messages.getString("Misc.boardTeleportSet", bv.getName()));
			}
		} else if (getBooleanOption("clear")) {
			PermissionUtils.requirePerms(sender, "chesscraft.commands.teleport.set");
			if (args.length == 0) {
				// clear global teleport-out location
				BoardViewManager.getManager().setGlobalTeleportOutDest(null);
				MiscUtil.statusMessage(player, Messages.getString("Misc.globalTeleportCleared"));
			} else {
				// clear per-board teleport-out location
				BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
				bv.setTeleportOutDestination(null);
				MiscUtil.statusMessage(player, Messages.getString("Misc.boardTeleportCleared", bv.getName()));
			}
		} else if (getBooleanOption("b") && args.length > 0) {
			// teleport to board
			PermissionUtils.requirePerms(sender, "chesscraft.commands.teleport.board");
			BoardView bv = BoardViewManager.getManager().getBoardView(args[0]);
			((CheckersPlugin) plugin).getPlayerTracker().teleportPlayer(player, bv.getTeleportInDestination());
		} else if (args.length == 0) {
			// teleport out of (or back to) current game
			BoardViewManager.getManager().teleportOut(player);
		} else {
			// teleport to game
			CheckersGame game = CheckersGameManager.getManager().getGame(args[0], true);
			BoardView bv = BoardViewManager.getManager().findBoardForGame(game);
			((CheckersPlugin) plugin).getPlayerTracker().teleportPlayer(player, bv.getTeleportInDestination());
		}

		return true;
	}

	private void showTeleportDests(CommandSender sender) {
		String bullet = MessagePager.BULLET + ChatColor.DARK_PURPLE;
		MessagePager pager = MessagePager.getPager(sender).clear();
		Location loc = BoardViewManager.getManager().getGlobalTeleportOutDest();
		if (loc != null) {
			pager.add(bullet + ChatColor.YELLOW + "[GLOBAL]" + ChatColor.WHITE + ": " + MiscUtil.formatLocation(loc));
		}
		for (BoardView bv : BoardViewManager.getManager().listBoardViewsSorted()) {
			if (bv.hasTeleportOutDestination()) {
				loc = bv.getTeleportOutDestination();
				pager.add(bullet + ChatColor.YELLOW + bv.getName() + ChatColor.WHITE + ": " + MiscUtil.formatLocation(loc));
			}
		}
		pager.showPage();
	}

}
