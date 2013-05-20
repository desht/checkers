package me.desht.checkers;

public class IllegalMoveException extends CheckersException {

	private static final long serialVersionUID = 1L;

	public IllegalMoveException(String message) {
		super(message);
	}

	public IllegalMoveException() {
		super(Messages.getString("Game.illegalMove"));
	}

}
