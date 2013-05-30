package me.desht.checkers.commands;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class SaveCommand extends AbstractCheckersCommand {

	public SaveCommand() {
		super("checkers save", 0, 0);
		setPermissionNode("checkers.commands.save");
		setUsage("/<command> save");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		((CheckersPlugin) plugin).getPersistenceHandler().save();
		MiscUtil.statusMessage(sender, Messages.getString("Misc.dataSaved"));
		return true;
	}

}
