package me.desht.checkers.event;

import me.desht.checkers.view.BoardView;

import org.bukkit.event.HandlerList;

public class CheckersBoardDeletedEvent extends CheckersBoardEvent {
	private static final HandlerList handlers = new HandlerList();

	public CheckersBoardDeletedEvent(BoardView boardView) {
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
