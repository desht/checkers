package me.desht.checkers.commands;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.PlayerColour;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class JoinCommand extends AbstractCheckersCommand {

	public JoinCommand() {
		super("checkers join", 0, 1);
		setPermissionNode("checkers.commands.join");
		setUsage("/<command> join [<game-name>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		Player player = (Player) sender;

		CheckersGameManager gameManager = CheckersGameManager.getManager();

		PlayerColour playingAs = null;
		String gameName = null;
		if (args.length >= 1) {
			gameName = args[0];
			playingAs = gameManager.getGame(gameName).addPlayer(player.getUniqueId().toString(), player.getDisplayName());
		} else {
			// find a game (or games) with an invitation for us
			for (CheckersGame game : gameManager.listGames()) {
				if (game.getInvitedId().equals(player.getUniqueId())) {
					playingAs = game.addPlayer(player.getUniqueId().toString(), player.getDisplayName());
					gameName = game.getName();
				}
			}
			if (playingAs == null) {
				throw new CheckersException(Messages.getString("Game.noPendingInvitation"));
			}
		}

		CheckersGame game = gameManager.getGame(gameName);
		gameManager.setCurrentGame(player, game);
		MiscUtil.statusMessage(sender, Messages.getString("Game.joinedGame", game.getName(), playingAs.getDisplayColour()));

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1 && sender instanceof Player) {
			return getInvitedGameCompletions((Player) sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

	private List<String> getInvitedGameCompletions(Player player, String prefix) {
		List<String> res = new ArrayList<String>();

		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix) && game.isOpenInvite() || player.getUniqueId().equals(game.getInvitedId())) {
				res.add(game.getName());
			}
		}
		return getResult(res, player, true);
	}
}
