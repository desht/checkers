package me.desht.checkers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PersistenceHandler {
	public void reload() {
		List<CheckersGame> games = new ArrayList<CheckersGame>(CheckersGameManager.getManager().listGames());
		for (CheckersGame game : games) {
			game.deleteTemporary();
		}
		List<BoardView> views = new ArrayList<BoardView>(BoardViewManager.getManager().listBoardViews());
		for (BoardView view : views) {
			view.deleteTemporary();
		}

		loadPersistedData();
	}

	public void save() {
		saveBoards();
		saveOtherPersistedData();
	}

	private void saveBoards() {
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			savePersistable("board", bv);
			if (bv.getGame() != null) {
				bv.getGame().save();
			}
		}
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

		loadOtherPersistedData();
	}

	public boolean loadBoard(File f) {
		LogUtils.fine("loading board: " + f);
		try {
			Configuration conf = MiscUtil.loadYamlUTF8(f);
			BoardView bv;
			if (conf.contains("board")) {
				bv = (BoardView) conf.get("board");
				if (bv.isWorldAvailable()) {
					BoardViewManager.getManager().registerView(bv);
					// load the board's game too, if there is one
					if (!bv.getSavedGameName().isEmpty()) {
						File gameFile = new File(DirectoryStructure.getGamesPersistDirectory(), bv.getSavedGameName() + ".yml");
						CheckersGame game = loadGame(gameFile);
						if (game != null) {
							bv.setGame(game);
						}
					}
				} else {
					BoardViewManager.getManager().deferLoading(bv.getBoard().getA1Center().getWorldName(), f);
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

	private CheckersGame loadGame(File gameFile) {
		LogUtils.fine("loading game: " + gameFile);
		try {
			Configuration conf = MiscUtil.loadYamlUTF8(gameFile);
			CheckersGame game = null;
			if (conf.contains("game")) {
				game = (CheckersGame) conf.get("game");
				CheckersGameManager.getManager().registerGame(game);
				return game;
			} else {
				LogUtils.severe("invalid game save file " + gameFile);
			}
		} catch (Exception e) {
			LogUtils.severe("can't load saved game from " + gameFile.getName() + ": " + e.getMessage(), e);
		}
		return null;
	}

	private void saveOtherPersistedData() {
		YamlConfiguration conf = new YamlConfiguration();
		for (Entry<String,String> e : CheckersGameManager.getManager().getCurrentGames().entrySet()) {
			conf.set("current_games." + e.getKey(), e.getValue());
		}

		Location loc = BoardViewManager.getManager().getGlobalTeleportOutDest();
		if (loc != null) {
			conf.set("teleport_out_dest", new PersistableLocation(loc));
		}

		try {
			conf.save(DirectoryStructure.getPersistFile());
		} catch (IOException e) {
			LogUtils.severe("Can't save " + DirectoryStructure.getPersistFile(), e);
		}
	}

	private void loadOtherPersistedData() {
		try {
			YamlConfiguration conf = MiscUtil.loadYamlUTF8(DirectoryStructure.getPersistFile());
			ConfigurationSection current = conf.getConfigurationSection("current_games");
			if (current != null) {
				for (String player : current.getKeys(false)) {
					try {
						CheckersGameManager.getManager().setCurrentGame(player, current.getString(player));
					} catch (CheckersException e) {
						LogUtils.warning("can't set current game for player " + player + ": " + e.getMessage());
					}
				}
			}
			if (conf.contains("teleport_out_dest")) {
				PersistableLocation pLoc = (PersistableLocation) conf.get("teleport_out_dest");
				BoardViewManager.getManager().setGlobalTeleportOutDest(pLoc.getLocation());
			}
		} catch (Exception e) {
			LogUtils.severe("Unexpected Error while loading " + DirectoryStructure.getPersistFile().getName());
			LogUtils.severe("Message: " + e.getMessage());
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
