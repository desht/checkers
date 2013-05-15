package me.desht.checkers.event;

import me.desht.checkers.view.BoardView;

import org.bukkit.event.HandlerList;

public class CheckersBoardCreatedEvent extends CheckersBoardEvent {
	private static final HandlerList handlers = new HandlerList();

	public CheckersBoardCreatedEvent(BoardView boardView) {
		super(boardView);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
