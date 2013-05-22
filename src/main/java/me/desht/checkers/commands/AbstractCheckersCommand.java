package me.desht.checkers.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.desht.checkers.DirectoryStructure;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.view.BoardStyle;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public abstract class AbstractCheckersCommand extends AbstractCommand {

	public AbstractCheckersCommand(String label) {
		super(label);
	}
	public AbstractCheckersCommand(String label, int minArgs) {
		super(label, minArgs);
	}
	public AbstractCheckersCommand(String label, int minArgs, int maxArgs) {
		super(label, minArgs, maxArgs);
	}

	protected List<String> getGameCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();

		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix)) {
				res.add(game.getName());
			}
		}
		return getResult(res, sender, true);
	}

	protected List<String> getPlayerInGameCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();

		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			if (game.getName().startsWith(prefix) && game.hasPlayer(sender.getName())) {
				res.add(game.getName());
			}
		}
		return getResult(res, sender, true);
	}

	protected List<String> getBoardCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();

		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			if (bv.getName().startsWith(prefix)) {
				res.add(bv.getName());
			}
		}
		return getResult(res, sender, true);
	}

	protected List<String> getBoardStyleCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> styleNames = new ArrayList<String>();
		for (BoardStyle style : getAllBoardStyles()) {
			styleNames.add(style.getName());
		}
		return filterPrefix(sender, styleNames, prefix);
	}

	protected List<BoardStyle> getAllBoardStyles() {
		Map<String, BoardStyle> res = new HashMap<String, BoardStyle>();

		File dir = DirectoryStructure.getBoardStyleDirectory();
		File customDir = new File(dir, "custom");

		for (File f : customDir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			res.put(styleName, BoardStyle.loadStyle(styleName));
		}
		for (File f : dir.listFiles(DirectoryStructure.ymlFilter)) {
			String styleName = f.getName().replaceAll("\\.yml$", "");
			if (res.containsKey(styleName)) continue;
			res.put(styleName, BoardStyle.loadStyle(styleName));
		}
		return MiscUtil.asSortedList(res.values());
	}
}
