package me.desht.checkers.commands;

import java.util.ArrayList;
import java.util.List;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.ai.AIFactory.AIDefinition;
import me.desht.dhutils.MessagePager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ListAICommand extends AbstractCheckersCommand {

	public ListAICommand() {
		super("checkers list ai", 0, 1);
		setPermissionNode("checkers.commands.list.ai");
		setUsage("/checkers list ai");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		CheckersPlugin cPlugin = (CheckersPlugin) plugin;

		MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);

		if (args.length == 0) {
			List<AIDefinition> aiDefs = cPlugin.getAIFactory().listAIDefinitions(true);
			List<String> lines = new ArrayList<String>(aiDefs.size());
			for (AIDefinition aiDef : aiDefs) {
				if (!aiDef.isEnabled())
					continue;
				String line = Messages.getString("AI.AIList", aiDef.getName(), aiDef.getImplClassName(), aiDef.getComment());
				if (cPlugin.getEconomy() != null) {
					line = line + ", " + Messages.getString("AI.AIpayout", (int) (aiDef.getPayoutMultiplier() * 100));
				}
				lines.add(MessagePager.BULLET +  line);
			}
			pager.add(lines);
		} else {
			AIDefinition aiDef = cPlugin.getAIFactory().getAIDefinition(args[0], true);
			pager.add(aiDef.getDetails());
		}

		pager.showPage();
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getPlayerCompletions(plugin, sender, args[0], true);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

}
