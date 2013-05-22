package me.desht.checkers.commands;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class OfferDrawCommand extends AbstractCheckersCommand {

	public OfferDrawCommand() {
		super("checkers offer draw", 0, 0);
		setPermissionNode("checkers.commands.offer.draw");
		setUsage("/checkers offer draw");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) {
		notFromConsole(player);

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(player.getName(), true);
		game.offerDraw(player.getName());

		return true;
	}

}
