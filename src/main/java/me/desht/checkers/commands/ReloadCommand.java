package me.desht.checkers.commands;

import java.util.Arrays;
import java.util.List;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.TimeControlDefs;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.checkers.view.controlpanel.TimeControlButton;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand extends AbstractCheckersCommand {
	public ReloadCommand() {
		super("checkers reload", 1);
		setPermissionNode("chesscraft.commands.reload");
		setUsage("/checkers reload <ai|config|gamedata|timecontrols>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) throws CheckersException {
		boolean reloadPersisted = false;
		boolean reloadAI = false;
		boolean reloadConfig = false;
		boolean reloadTimeControls = false;

		for (String arg : args) {
			if (arg.startsWith("a")) {
				reloadAI = true;
			} else if (arg.startsWith("c")) {
				reloadConfig = true;
			} else if (arg.startsWith("g")) {
				reloadPersisted = true;
			} else if (arg.startsWith("t")) {
				reloadTimeControls = true;
			} else {
				showUsage(player);
				return true;
			}
		}

		if (reloadConfig) {
			plugin.reloadConfig();
			MiscUtil.statusMessage(player, Messages.getString("Misc.configReloaded"));
		}
		if (reloadAI) {
			((CheckersPlugin) plugin).getAIFactory().loadAIDefinitions();
			MiscUtil.statusMessage(player, Messages.getString("Misc.AIdefsReloaded"));
		}
		if (reloadPersisted) {
			((CheckersPlugin) plugin).getPersistenceHandler().reload();
			MiscUtil.statusMessage(player, Messages.getString("Misc.persistedReloaded"));
		}
		if (reloadTimeControls) {
			TimeControlDefs.loadBaseDefs();
			for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
				bv.getControlPanel().getButton(TimeControlButton.class).reloadDefs();
			}
			MiscUtil.statusMessage(player, Messages.getString("Misc.timeControlsReloaded"));
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return filterPrefix(sender, Arrays.asList(new String[] { "ai", "config", "gamedata", "timecontrols" }), args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

}
