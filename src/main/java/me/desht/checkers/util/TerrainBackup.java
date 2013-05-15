package me.desht.checkers.util;

import java.io.File;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.DirectoryStructure;
import me.desht.checkers.view.BoardView;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.TerrainManager;

import org.bukkit.Location;

public class TerrainBackup {

	public static boolean save(BoardView view) {
		boolean saved = false;
		try {
			TerrainManager tm = new TerrainManager(CheckersPlugin.getInstance().getWorldEdit(), view.getWorld());
			Cuboid c = view.getBoard().getFullBoard();
			Location l1 = c.getLowerNE();
			Location l2 = c.getUpperSW();
			tm.saveTerrain(new File(DirectoryStructure.getSchematicsDirectory(), view.getName()), l1, l2);
			saved = true;
		} catch (Exception e) {
			LogUtils.warning(e.getMessage());
		}
		return saved;
	}

	public static boolean reload(BoardView view) {	
		boolean restored = false;
		try {
			TerrainManager tm = new TerrainManager(CheckersPlugin.getInstance().getWorldEdit(), view.getWorld());
			tm.loadSchematic(new File(DirectoryStructure.getSchematicsDirectory(), view.getName()));
			restored = true;
		} catch (Exception e) {
			LogUtils.warning(e.getMessage());
		}
		return restored;
	}
}