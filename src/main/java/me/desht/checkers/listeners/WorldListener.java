package me.desht.checkers.listeners;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.view.BoardViewManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener extends CheckersBaseListener {

	public WorldListener(CheckersPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onWorldLoaded(WorldLoadEvent event) {
		BoardViewManager.getManager().loadDeferred(event.getWorld().getName());
	}
}
