package me.desht.checkers;

import me.desht.checkers.model.Position;
import me.desht.checkers.model.SimplePosition;
import me.desht.checkers.view.BoardView;

public class CheckersGame {

	private final Position position;

	public CheckersGame(String gameName, BoardView bv, String playerName, int colour) {
		position = new SimplePosition();
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public void save() {
		// TODO Auto-generated method stub
		
	}

	public BoardView getView() {
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteTemporary() {
		// TODO Auto-generated method stub
		
	}

	public Position getPosition() {
		return position;
	}

}
