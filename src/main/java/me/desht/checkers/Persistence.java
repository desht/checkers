package me.desht.checkers;

import java.io.File;
import java.io.IOException;

import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Persistence {
	public void reload() {
		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			game.deleteTemporary();
		}
		for (BoardView view : BoardViewManager.getManager().listBoardViews()) {
			view.deleteTemporary();
		}

		loadPersistedData();
	}

	private void loadPersistedData() {
		int nLoaded = 0;

		// load the boards, and any games on those boards
		for (File f : DirectoryStructure.getBoardPersistDirectory().listFiles(DirectoryStructure.ymlFilter)) {
			nLoaded += loadBoard(f) ? 1 : 0;
		}

		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			bv.getControlPanel().repaintControls();
		}

		LogUtils.fine("loaded " + nLoaded + " saved boards.");

	}

	public boolean loadBoard(File f) {
		LogUtils.fine("loading board: " + f);
		try {
			Configuration conf = MiscUtil.loadYamlUTF8(f);
			BoardView bv;
			if (conf.contains("board")) {
				bv = (BoardView) conf.get("board");
				BoardViewManager.getManager().registerView(bv);
				// load the board's game too, if there is one
				if (!bv.getSavedGameName().isEmpty()) {
					File gameFile = new File(DirectoryStructure.getGamesPersistDirectory(), bv.getSavedGameName() + ".yml");
					loadGame(gameFile);
				}
			} else {
				return false;
			}
		} catch (Exception e) {
			LogUtils.severe("can't load saved board from " + f.getName() + ": " + e.getMessage(), e);
			// TODO: restore terrain, if applicable?
			return false;
		}
		return true;
	}

	private void loadGame(File gameFile) {
		// TODO Auto-generated method stub
		LogUtils.fine("loading game: " + gameFile);
		try {
			Configuration conf = MiscUtil.loadYamlUTF8(gameFile);
			CheckersGame game = null;
			if (conf.contains("game")) {
				game = (CheckersGame) conf.get("game");
				CheckersGameManager.getManager().registerGame(game);
			} else {

			}
		} catch (Exception e) {
			LogUtils.severe("can't load saved game from " + gameFile.getName() + ": " + e.getMessage(), e);
		}
	}

	public void savePersistable(String tag, CheckersPersistable object) {
		YamlConfiguration conf = new YamlConfiguration();
		conf.set(tag, object);
		File file = new File(object.getSaveDirectory(), makeSafeFileName(object.getName()) + ".yml");
		try {
			conf.save(file);
		} catch (IOException e1) {
			LogUtils.severe("Can't save " + tag + " " + object.getName(), e1);
		}
	}

	public void unpersist(CheckersPersistable object) {
		File f = new File(object.getSaveDirectory(), makeSafeFileName(object.getName()) + ".yml");
		if (!f.delete()) {
			LogUtils.warning("Can't delete save file " + f);
		}
	}

	public static String makeSafeFileName(String name) {
		return name == null ? "" : name.replace("/", "-").replace("\\", "-").replace("?", "-").replace(":", ";").replace("%", "-").replace("|", ";").replace("\"", "'").replace("<", ",").replace(">", ".").replace("+", "=").replace("[", "(").replace("]", ")");
	}

	public static void requireSection(Configuration c, String key) {
		if (!c.contains(key))
			throw new CheckersException("missing required section '" + key + "'");
	}
}
