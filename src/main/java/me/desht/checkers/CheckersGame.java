package me.desht.checkers;

import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.model.Position;
import me.desht.checkers.model.SimplePosition;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.checkers.view.BoardView;

public class CheckersGame {
	public enum GameResult {
		GAME_WON, DRAW_AGREED, ABANDONED,
	}

	public static final Object OPEN_INVITATION = "*";

	private final String gameName;
	private final Position position;
	private final BoardView view;
	private final CheckersPlayer[] players = new CheckersPlayer[2];

	public CheckersGame(String gameName, BoardView bv, String creatorName, int colour) {
		this.gameName = gameName;
		this.position = new SimplePosition();
		this.view = bv;
	}

	public String getName() {
		return gameName;
	}

	public Position getPosition() {
		return position;
	}

	public BoardView getView() {
		return view;
	}

	public CheckersPlayer getPlayer(PlayerColour colour) {
		return players[colour.getIndex()];
	}

	public void save() {
		// TODO Auto-generated method stub

	}

	public void deleteTemporary() {
		// TODO Auto-generated method stub
		
	}

	public double getStake() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getInvited() {
		// TODO Auto-generated method stub
		return null;
	}

	public void alert(String playerName, String message) {
		// TODO Auto-generated method stub
		
	}

	public void drawn(GameResult drawAgreed) {
		// TODO Auto-generated method stub
		
	}

	public void swapColours() {
		// TODO Auto-generated method stub
		
	}

}
