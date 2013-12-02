package me.desht.checkers.model.rules;

public class CanadianCheckers extends InternationalDraughts {
	public CanadianCheckers() {
	}

	@Override
	public int getBoardSize() {
		return 12;
	}

	@Override
	public int getPieceRowCount() {
		return 5;
	}
}
