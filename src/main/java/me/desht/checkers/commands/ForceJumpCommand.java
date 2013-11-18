package me.desht.checkers.commands;

import me.desht.checkers.CheckersValidate;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.game.CheckersGame.GameState;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ForceJumpCommand extends AbstractCheckersCommand {

	public ForceJumpCommand() {
		super("checkers forcejump", 0, 0);
		setPermissionNode("checkers.commands.forcejump");
		setUsage("/<command> forcejump");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		CheckersGame game = CheckersGameManager.getManager().getCurrentGame(sender.getName(), true);

		CheckersValidate.isTrue(game.getState() == GameState.SETTING_UP, Messages.getString("Game.mustBeInSetup"));

//		game.getPosition().setForcedJump(!game.getPosition().isForcedJump());
//
//		game.alert(game.getPosition().isForcedJump() ? Messages.getString("Game.forceJumpEnabled") : Messages.getString("Game.forceJumpDisabled"));

		return true;
	}

}
