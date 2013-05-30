package me.desht.checkers.commands;

import me.desht.checkers.responses.YesNoResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class YesCommand extends AbstractCheckersCommand {

	public YesCommand() {
		super("checkers yes", 0, 0);
		setUsage("/<command> yes");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		YesNoResponse.handleYesNoResponse((Player)sender, true);
		return true;
	}
}
