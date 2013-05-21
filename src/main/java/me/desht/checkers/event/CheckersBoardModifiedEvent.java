package me.desht.checkers.event;

import java.util.HashSet;
import java.util.Set;

import me.desht.checkers.view.BoardView;

import org.bukkit.event.HandlerList;

public class CheckersBoardModifiedEvent extends CheckersBoardEvent {

	private static final HandlerList handlers = new HandlerList();

	private final Set<String> changed;

	public CheckersBoardModifiedEvent(BoardView boardView, Set<String> changedAttributes) {
		super(boardView);
		changed = new HashSet<String>(changedAttributes);
	}

	public Set<String> getChangedAttributes() {
		return changed;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
