package me.desht.checkers.commands;

import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.model.PlayerColour;

import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.checkers.view.controlpanel.SelectRulesButton;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CreateGameCommand extends AbstractCheckersCommand {

	public CreateGameCommand() {
		super("checkers create game");
		setPermissionNode("checkers.commands.create.game");
		setUsage("/<command> create game [-white] [<game-name>] [<board-name>]");
		setOptions(new String[] { "white", "rules:s" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);

		String gameName = args.length >= 1 ? args[0] : null;
		String boardName = args.length >= 2 ? args[1] : null;
		BoardView bv = BoardViewManager.getManager().getBoardView(boardName);
		PlayerColour colour = getBooleanOption("white") ? PlayerColour.WHITE : PlayerColour.BLACK;
		String ruleId;
		if (hasOption("rules")) {
			ruleId = getStringOption("rules");
			bv.getControlPanel().getButton(SelectRulesButton.class).setRuleset(ruleId);
		} else {
			ruleId = bv.getControlPanel().getButton(SelectRulesButton.class).getRuleset();
		}
		CheckersGameManager.getManager().createGame((Player) sender, gameName, bv, colour, ruleId);

		return true;
	}
}
