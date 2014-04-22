package me.desht.checkers.commands;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class OfferDrawCommand extends AbstractCheckersCommand {

	public OfferDrawCommand() {
		super("checkers offer draw", 0, 0);
		setPermissionNode("checkers.commands.offer.draw");
		setUsage("/<command> offer draw");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		Player player = (Player) sender;
		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(player, true);
		game.offerDraw(player.getUniqueId().toString());

		return true;
	}

}
