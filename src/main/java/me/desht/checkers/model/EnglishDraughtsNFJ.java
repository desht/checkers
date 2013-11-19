package me.desht.checkers.model;

public class EnglishDraughtsNFJ extends EnglishDraughts {
	public EnglishDraughtsNFJ(Position position) {
		super(position);
	}

	@Override
	public String getId() {
		return "englishDraughtsNFJ";
	}

	@Override
	public boolean isForcedJump() {
		return false;
	}
}
