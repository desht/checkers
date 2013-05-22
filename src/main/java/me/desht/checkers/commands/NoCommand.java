package me.desht.checkers.commands;

import me.desht.checkers.responses.YesNoResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class NoCommand extends AbstractCheckersCommand {

	public NoCommand() {
		super("checkers no", 0, 0);
		setUsage("/checkers no");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		YesNoResponse.handleYesNoResponse((Player)sender, false);
		return true;
	}

}
