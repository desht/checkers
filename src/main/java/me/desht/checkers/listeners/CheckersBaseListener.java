package me.desht.checkers.listeners;

import org.bukkit.event.Listener;

import me.desht.checkers.CheckersPlugin;

public abstract class CheckersBaseListener implements Listener {
	protected final CheckersPlugin plugin;

	public CheckersBaseListener(CheckersPlugin plugin) {
		this.plugin = plugin;
	}
}
