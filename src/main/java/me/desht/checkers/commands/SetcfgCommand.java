package me.desht.checkers.commands;

import java.util.ArrayList;
import java.util.List;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class SetcfgCommand extends AbstractCheckersCommand {

	public SetcfgCommand() {
		super("checkers setcfg", 2);
		setPermissionNode("checkers.commands.setcfg");
		setUsage("/<command> setcfg <config-key> <value>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String key = args[0], val = args[1];

		ConfigurationManager configManager = ((CheckersPlugin) plugin).getConfigManager();

		try {
			if (args.length > 2) {
				List<String> list = new ArrayList<String>(args.length - 1);
				for (int i = 1; i < args.length; i++)
					list.add(args[i]);
				configManager.set(key, list);	
			} else {
				configManager.set(key, val);
			}
			Object res = configManager.get(key);
			MiscUtil.statusMessage(sender, key + " is now set to '&e" + res + "&-'");
		} catch (CheckersException e) {
			MiscUtil.errorMessage(sender, e.getMessage());
			MiscUtil.errorMessage(sender, "Use /checkers getcfg to list all valid keys");
		} catch (IllegalArgumentException e) {
			MiscUtil.errorMessage(sender, e.getMessage());
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		ConfigurationSection config = plugin.getConfig();
		switch (args.length) {
		case 1:
			return getConfigCompletions(sender, config, args[0]);
		case 2:
			return getConfigValueCompletions(sender, args[0], config.get(args[0]), "", args[1]);
		default:
			return noCompletions(sender);
		}
	}
}
