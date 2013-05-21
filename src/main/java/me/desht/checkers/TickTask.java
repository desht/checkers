package me.desht.checkers;

import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;

import org.bukkit.scheduler.BukkitRunnable;

public class TickTask extends BukkitRunnable {

	@Override
	public void run() {
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			bv.tick();
		}
	}
}
