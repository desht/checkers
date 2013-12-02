package me.desht.checkers.model;

public class BrazilianDraughts extends InternationalDraughts {
	public BrazilianDraughts() {
	}

	@Override
	public int getBoardSize() {
		return 8;
	}

	@Override
	public int getPieceRowCount() {
		return 3;
	}
}
