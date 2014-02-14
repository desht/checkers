package me.desht.checkers.commands;

import java.util.Arrays;
import java.util.List;

import me.desht.checkers.CheckersException;
import me.desht.checkers.Messages;
import me.desht.checkers.results.Results;
import me.desht.checkers.results.ScoreRecord;
import me.desht.dhutils.MessagePager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListTopCommand extends AbstractCheckersCommand {

	public ListTopCommand() {
		super("checkers list top", 0, 3);
		setPermissionNode("checkers.commands.list.top");
		setUsage("/<command> list top [<n>] [ladder|league] [-ai] [-r]");
		setOptions("ai", "r");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		final Results results = Results.getResultsHandler();
		if (results == null) {
			throw new CheckersException("Result data is not available at this time.");
		}
		if (getBooleanOption("r")) {
			results.rebuildViews();
		}
		int n = 5;
		if (args.length > 0) {
			try {
				n = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				throw new CheckersException(Messages.getString("Misc.invalidNumeric", args[0]));
			}
		}
		String viewName = args.length > 1 ? args[1] : "ladder";
		boolean excludeAI = getBooleanOption("ai");

		List<ScoreRecord> scores = results.getView(viewName).getScores(n, excludeAI);
		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
		int row = 1;
		for (ScoreRecord sr : scores) {
			pager.add(MessagePager.BULLET + Messages.getString("Misc.scoreRecord", row, sr.getPlayer(), sr.getScore()));
			row++;
		}
		pager.showPage();
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 2) {
			return filterPrefix(sender, Arrays.asList("ladder", "league"), args[1]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
