package me.desht.checkers.responses;

import me.desht.checkers.Messages;
import me.desht.checkers.view.BoardRotation;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.responsehandler.ExpectBase;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BoardCreationHandler extends ExpectBase {

	private final String boardName;
	private final String boardStyle;
	private final int size;
	private Location loc;

	public BoardCreationHandler(String boardName, String boardStyle, int size) {
		this.boardName = boardName;
		this.boardStyle = boardStyle;
		this.size = size;
	}

	public void setLocation(Location loc) {
		this.loc = loc;
	}

	@Override
	public void doResponse(UUID playerId) {
		Player player = Bukkit.getPlayer(playerId);
		if (player == null) {
			LogUtils.warning("Board creation: player ID " + playerId + " gone offline?");
			return;
		}

		BoardView view = BoardViewManager.getManager().createBoard(boardName, loc, BoardRotation.getRotation(player), boardStyle, size);

		MiscUtil.statusMessage(player, Messages.getString("Board.boardCreated",
		                                                  view.getName(), MiscUtil.formatLocation(view.getBoard().getA1Center().getLocation())));
	}
}
