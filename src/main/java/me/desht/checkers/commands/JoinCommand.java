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
			CheckersGame game = gameManager.getGame(gameName);
			if (game.getInvitedId().equals(player.getUniqueId())) {
				playingAs = game.addPlayer(player.getUniqueId().toString(), player.getDisplayName());
			} else {
				throw new CheckersException(Messages.getString("Game.notInvited"));
			}
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
//		PlayerColour playingAs = game.getPlayer(player.getUniqueId().toString()).getColour();
		MiscUtil.statusMessage(sender, Messages.getString("Game.joinedGame", game.getName(), playingAs.getDisplayColour()));

		return true;
	}

}
