package me.desht.checkers;

public class CheckersValidate {
	public static void isTrue(boolean cond, String err) {
		if (!cond) throw new CheckersException(err);
	}

	public static void isFalse(boolean cond, String err) {
		if (cond) throw new CheckersException(err);
	}

	public static void notNull(Object o, String err) {
		if (o == null) throw new CheckersException(err);
	}
}
