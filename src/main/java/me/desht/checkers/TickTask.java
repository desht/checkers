package me.desht.checkers;

import java.util.ArrayList;
import java.util.List;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;

import org.bukkit.scheduler.BukkitRunnable;

public class TickTask extends BukkitRunnable {

	@Override
	public void run() {
		List<CheckersGame> games = new ArrayList<CheckersGame>(CheckersGameManager.getManager().listGames());
		for (CheckersGame game : games) {
			game.tick();
		}
	}

}
