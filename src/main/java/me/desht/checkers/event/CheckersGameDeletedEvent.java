package me.desht.checkers.event;

import me.desht.checkers.CheckersGame;

import org.bukkit.event.HandlerList;

public class CheckersGameDeletedEvent extends CheckersGameEvent {

	private static final HandlerList handlers = new HandlerList();

	public CheckersGameDeletedEvent(CheckersGame game) {
		super(game);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
