package me.desht.checkers.commands;

import java.util.List;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.CheckersValidate;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.dhutils.Duration;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class WinCommand extends AbstractCheckersCommand {

	public WinCommand() {
		super("checkers win", 0, 0);
		setPermissionNode("checkers.commands.win");
		setUsage("/<command> win");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(sender.getName(), true);

		CheckersPlayer cp = game.getPlayer(sender.getName());
		CheckersValidate.notNull(cp, Messages.getString("Game.notInGame"));
		CheckersPlayer other = game.getPlayer(cp.getColour().getOtherColour());
		if (other == null || !other.isHuman() || other.isAvailable()) {
			throw new CheckersException(Messages.getString("Misc.otherPlayerMustBeOffline"));
		}

		String timeout = plugin.getConfig().getString("forfeit_timeout");
		Duration duration;
		try {
			duration = new Duration(timeout);
		} catch (IllegalArgumentException e) {
			throw new CheckersException("invalid timeout: " + timeout);
		}
		long leftAt = ((CheckersPlugin)plugin).getPlayerTracker().getPlayerLeftAt(other.getName());
		if (leftAt == 0) {
			String msg = Messages.getString("Misc.otherPlayerMustBeOffline");
			if (!other.isAvailable()) {
				msg += " " + Messages.getString("Misc.otherPlayerNeverRejoined");
			}
			throw new CheckersException(msg);
		}

		long now = System.currentTimeMillis();
		long elapsed = now - leftAt;
		if (elapsed >= duration.getTotalDuration()) {
			game.forfeit(other.getName());
		} else {
			throw new CheckersException(Messages.getString("Misc.needToWait", (duration.getTotalDuration() - elapsed) / 1000));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		showUsage(sender);
		return noCompletions(sender);
	}

}
