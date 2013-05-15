package me.desht.checkers.event;

import me.desht.checkers.CheckersGame;

public abstract class CheckersGameEvent extends CheckersEvent {

	protected final CheckersGame game;

	public CheckersGameEvent(CheckersGame game) {
		this.game = game;
	}

	public CheckersGame getGame() {
		return game;
	}

}
