package me.desht.checkers.model;

import me.desht.checkers.CheckersException;
import me.desht.dhutils.MiscUtil;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class GameRules {
	private static final Map<String,GameRules> allRulesets = new LinkedHashMap<String, GameRules>();

	/**
	 * Get the rule identifier string, used by getRules(String)
	 *
	 * @return the rule ID
	 */
	public String getId() {
		return getClass().getSimpleName();
	}

	/**
	 * Get the number of squares on a board edge.
	 *
	 * @return the number of squares on an edge
	 */
	public abstract int getBoardSize();

	/**
	 * Get the number of rows of pieces each player has at the start of the game.
	 *
	 * @return the number of rows of pieces
	 */
	public abstract int getPieceRowCount();

	/**
	 * Get who moves first.
	 *
	 * @return the colour of the player to move first
	 */
	public abstract PlayerColour getWhoMovesFirst();

	/**
	 * Check if this ruleset enforces jumps when they are possible.
	 *
	 * @return true if jump moves are enforced, false otherwise
	 */
	public abstract boolean isForcedJump();

	/**
	 * Check if this ruleset allows promotion on a jump which is not the final jump in a chain.
	 *
	 * @return true if promotion is allowed, false otherwise
	 */
	public abstract boolean allowChainedJumpPromotion();

	/**
	 * Check if the given player colour can move from the given board square in the given direction
	 *
	 * @param who the player colour to check
	 * @param square the board square
	 * @param direction the direction to move
	 * @return true if the move is legal, false otherwise
	 */
	public abstract List<Move> getMoves(Position position, PlayerColour who, RowCol square, MoveDirection direction);

	/**
	 * Check if the given player colour can jump from the given board square in the given direction
	 *
	 * @param who the player colour to check
	 * @param square the board square
	 * @param direction the direction to move
	 * @return true if the jump is legal, false otherwise
	 */
	public abstract List<Move> getJumps(Position position, PlayerColour who, RowCol square, MoveDirection direction);

	/**
	 * Check that the given square is actually isValidSquare.
	 *
	 * @param square the board square
	 * @return true if the square is valid for this ruleset, false otherwise
	 */
	public boolean isValidSquare(RowCol square) {
		return square.getCol() >= 0 && square.getRow() >= 0 && square.getCol() < getBoardSize() && square.getRow() < getBoardSize();
	}

	/**
	 * Calculate all possible legal moves for the given player colour
	 *
	 * @param who the colour to calculate for
	 * @return an array of legal moves
	 */
	public Move[] calculateLegalMoves(Position position, PlayerColour who) {
		List<Move> res = new ArrayList<Move>();

		// get all the possible jumps that can be made
		for (int row = 0; row < getBoardSize(); row++) {
			for (int col = 0; col < getBoardSize(); col++) {
				RowCol square = RowCol.get(row, col);
				if (position.getPieceAt(square).getColour() == who) {
					for (MoveDirection dir : MoveDirection.values()) {
						List<Move> jumps = getJumps(position, who, square, dir);
						if (jumps != null) {
							res.addAll(jumps);
						}
					}
				}
			}
		}

		// if there are any jumps available and this ruleset enforces jumping,
		// don't calculate any non-jump moves
		if (res.isEmpty() || !isForcedJump()) {
			for (int row = 0; row < getBoardSize(); row++) {
				for (int col = 0; col < getBoardSize(); col++) {
					RowCol square = RowCol.get(row, col);
					if (position.getPieceAt(square).getColour() == who) {
						for (MoveDirection dir : MoveDirection.values()) {
							List<Move> moves = getMoves(position, who, square, dir);
							if (moves != null) {
								res.addAll(moves);
							}
						}
					}
				}
			}
		}

		return res.toArray(new Move[res.size()]);
	}

	/**
	 * Get a list of the legal moves that can be made from the given square.
	 *
	 * @param square the board square
	 * @param onlyJumps true if only jump moves should be returned
	 * @return a list of the legal moves
	 */
	public Move[] getLegalMoves(Position position, RowCol square, boolean onlyJumps) {
		if (position.getPieceAt(square).getColour() != position.getToMove()) {
			return new Move[0];
		}
		List<Move> res = new ArrayList<Move>();
		for (MoveDirection dir : MoveDirection.values()) {
			List<Move> jumps = getJumps(position, position.getToMove(), square, dir);
			if (jumps != null) {
				res.addAll(jumps);
			}
		}
		if (!onlyJumps && (res.isEmpty() || !isForcedJump())) {
			for (MoveDirection dir : MoveDirection.values()) {
				List<Move> moves = getMoves(position, position.getToMove(), square, dir);
				if (moves != null) {
					res.addAll(moves);
				}
			}
		}
		return res.toArray(new Move[res.size()]);
	}

	private static void registerRules(Class<? extends GameRules> ruleClass) {
		try {
			Constructor<? extends GameRules> ctor = ruleClass.getDeclaredConstructor();
			GameRules rules = ctor.newInstance();
			allRulesets.put(rules.getId(), rules);
		} catch (Exception e) {
			throw new CheckersException("can't instantiate ruleset: " + ruleClass.getName() + ": " + e.getMessage());
		}
	}

	public static void registerRulesets() {
		registerRules(EnglishDraughts.class);
		registerRules(EnglishDraughtsNFJ.class);
		registerRules(InternationalDraughts.class);
		registerRules(CanadianCheckers.class);
		registerRules(BrazilianDraughts.class);
	}

	public static GameRules getRules(String ruleId) {
		return allRulesets.get(ruleId);
	}


	public static List<GameRules> getMatchingRules(int size) {
		List<GameRules> res = new ArrayList<GameRules>();
		for (String ruleId : MiscUtil.asSortedList(allRulesets.keySet())) {
			GameRules r = getRules(ruleId);
			if (r.getBoardSize() == size) {
				res.add(r);
			}
		}
		return res;
	}
}
