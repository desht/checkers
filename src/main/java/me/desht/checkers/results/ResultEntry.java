package me.desht.checkers.results;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameResult;
import me.desht.checkers.model.PlayerColour;
import me.desht.dhutils.Debugger;

import java.sql.*;

public class ResultEntry implements DatabaseSavable {

	private final String playerWhite, playerBlack;
	private final String gameName;
	private final long startTime, endTime;
	private final GameResult result;
	private final String pdnResult;

	ResultEntry(CheckersGame game) {
		playerWhite = game.getPlayer(PlayerColour.WHITE).getName();
		playerBlack = game.getPlayer(PlayerColour.BLACK).getName();
		gameName = game.getName();
		startTime = game.getStarted();
		endTime = game.getFinished();
		result = game.getResult();
		pdnResult = game.getPDNResult();
	}

	ResultEntry(String plw, String plb, String gn, long start, long end, String pdnRes, GameResult rt) {
		playerWhite = plw;
		playerBlack = plb;
		gameName = gn;
		startTime = start;
		endTime = end;
		result = rt;
		pdnResult = pdnRes;
	}

	ResultEntry(ResultSet rs) throws SQLException {
		playerWhite = rs.getString("playerwhite");
		playerBlack = rs.getString("playerBlack");
		gameName = rs.getString("gameName");
		startTime = rs.getDate("startTime").getTime();
		endTime = rs.getDate("endTime").getTime();
		result = GameResult.valueOf(rs.getString("result"));
		pdnResult = rs.getString("pdnResult");
	}

	public String getPlayerWhite() {
		return playerWhite;
	}

	public String getPlayerBlack() {
		return playerBlack;
	}

	public String getGameName() {
		return gameName;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public GameResult getResult() {
		return result;
	}

	public String getPdnResult() {
		return pdnResult;
	}

	public String getWinner() {
		if (pdnResult.equals("1-0")) {
			return playerWhite;
		} else if (pdnResult.equals("0-1")) {
			return playerBlack;
		} else {
			return null;
		}
	}

	public String getLoser() {
		if (pdnResult.equals("1-0")) {
			return playerBlack;
		} else if (pdnResult.equals("0-1")) {
			return playerWhite;
		} else {
			return null;
		}
	}

	public void saveToDatabase(Connection connection) throws SQLException {
		String tableName = Results.getResultsHandler().getTableName("results");
		PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO " + tableName + " (playerWhite, playerBlack, gameName, startTime, endTime, result, pdnResult)" +
				" VALUES (?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, playerWhite);
		stmt.setString(2, playerBlack);
		stmt.setString(3, gameName);
		stmt.setTimestamp(4, new Timestamp(startTime));
		stmt.setTimestamp(5, new Timestamp(endTime));
		stmt.setString(6, result.toString());
		stmt.setString(7, pdnResult);
		Debugger.getInstance().debug("execute SQL: " + stmt);
		stmt.executeUpdate();
	}
}

