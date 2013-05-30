package me.desht.checkers.commands;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class OfferSwapCommand extends AbstractCheckersCommand {

	public OfferSwapCommand() {
		super("checkers offer swap", 0, 0);
		setPermissionNode("checkers.commands.offer.swap");
		setUsage("/<command> offer swap");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) {
		notFromConsole(player);

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(player.getName(), true);
		game.offerSwap(player.getName());

		return true;
	}

}
