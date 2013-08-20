package me.desht.checkers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import me.desht.dhutils.JARUtil;
import me.desht.dhutils.JARUtil.ExtractWhen;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

public class DirectoryStructure {
	private static File pluginDir = new File("plugins", "Checkers"); //$NON-NLS-1$ //$NON-NLS-2$
	private static JARUtil jarUtil;

	private static File boardStyleDir, schematicsDir;
	private static File dataDir, gamePersistDir, boardPersistDir, languagesDir, resultsDir;
	private static final String boardStyleFoldername = "board_styles"; //$NON-NLS-1$
	private static final String schematicsFoldername = "schematics"; //$NON-NLS-1$
	private static final String languageFoldername = "lang"; //$NON-NLS-1$
	private static final String datasaveFoldername = "data"; //$NON-NLS-1$
	private static final String gamesFoldername = "games"; //$NON-NLS-1$
	private static final String boardsFoldername = "boards"; //$NON-NLS-1$
	private static final String resultsFoldername = "results"; //$NON-NLS-1$
	private static File persistFile;
	private static final String persistFilename = "persist.yml"; //$NON-NLS-1$

	public static void setup(CheckersPlugin plugin) {
		pluginDir = CheckersPlugin.getInstance().getDataFolder();
		jarUtil = new JARUtil(plugin);

		setupDirectoryStructure();
		try {
			extractResources();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static File getPluginDirectory() {
		return pluginDir;
	}

	public static File getBoardStyleDirectory() {
		return boardStyleDir;
	}

	public static File getSchematicsDirectory() {
		return schematicsDir;
	}

	public static File getGamesPersistDirectory() {
		return gamePersistDir;
	}

	public static File getBoardPersistDirectory() {
		return boardPersistDir;
	}

	public static File getLanguagesDirectory() {
		return languagesDir;
	}

	public static File getPersistFile() {
		return persistFile;
	}

	public static File getResultsDir() {
		return resultsDir;
	}

	private static void setupDirectoryStructure() {
		// directories
		boardStyleDir = new File(pluginDir, boardStyleFoldername);
		dataDir = new File(pluginDir, datasaveFoldername);
		gamePersistDir = new File(dataDir, gamesFoldername);
		boardPersistDir = new File(dataDir, boardsFoldername);
		schematicsDir = new File(boardPersistDir, schematicsFoldername);
		languagesDir = new File(pluginDir, languageFoldername);
		resultsDir = new File(dataDir, resultsFoldername);

		// files
		persistFile = new File(dataDir, persistFilename);

		// [plugins]/Checkers
		createDir(pluginDir);
		// [plugins]/Checkers/lang
		createDir(languagesDir);
		// [plugins]/Checkers/board_styles
		createDir(boardStyleDir);
		createDir(new File(boardStyleDir, "custom"));
		// [plugins]/Checkers/data
		createDir(dataDir);
		// [plugins]/Checkers/data/games
		createDir(gamePersistDir);
		// [plugins]/Checkers/data/boards
		createDir(boardPersistDir);
		// [plugins]/Checkers/data/results
		createDir(resultsDir);

		// saved board schematics may need to be moved into their new location
		File oldSchematicsDir = new File(pluginDir, "schematics"); //$NON-NLS-1$
		if (oldSchematicsDir.isDirectory()) {
			if (!oldSchematicsDir.renameTo(schematicsDir)) {
				LogUtils.warning("Can't move " + oldSchematicsDir + " to " + schematicsDir); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			// [plugins]/Checkers/data/boards/schematics
			createDir(schematicsDir);
		}
	}

	private static void extractResources() throws IOException {
		for (String s : MiscUtil.listFilesinJAR(jarUtil.getJarFile(), "datafiles/board_styles",	".yml")) {
			jarUtil.extractResource(s, boardStyleDir);
		}

		jarUtil.extractResource("/timecontrols.yml", pluginDir, ExtractWhen.IF_NOT_EXISTS);
		jarUtil.extractResource("/AI.yml", pluginDir, ExtractWhen.IF_NOT_EXISTS);

		// message resources no longer extracted here - this is now done by Messages.loadMessages()
	}

	private static void createDir(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			LogUtils.warning("Can't make directory " + dir.getName()); //$NON-NLS-1$
		}
	}

	/**
	 * Find a YAML resource file in the given directory.  Look first in the custom/ subdirectory
	 * and then in the directory itself.
	 * 
	 * @param dir
	 * @param filename
	 * @param saving
	 * @return
	 * @throws CheckersException
	 */
	public static File getResourceFileForLoad(File dir, String filename) {
		// try the lower-cased form first, if that fails try the exact filename
		File f = new File(dir, "custom" + File.separator + filename.toLowerCase() + ".yml");
		if (!f.exists()) {
			f = new File(dir, "custom" + File.separator + filename + ".yml");
		}
		if (!f.exists()) {
			f = new File(dir, filename.toLowerCase() + ".yml");
			if (!f.exists()) {
				throw new CheckersException("resource file '" + f + "' is not readable");
			}
		}
		return f;
	}

	/**
	 * Check if the given file is a custom resource, i.e. it's a custom/ subdirectory.
	 * 
	 * @param path
	 * @return
	 */
	public static boolean isCustom(File path) {
		return path.getParentFile().getName().equalsIgnoreCase("custom");
	}

	/** 
	 * Find a YAML resource in the custom/ subdirectory of the given directory.
	 * 
	 * @param dir
	 * @param filename
	 * @return
	 * @throws CheckersException
	 */
	public static File getResourceFileForSave(File dir, String filename) {
		File f = new File(dir, "custom" + File.separator + filename.toLowerCase() + ".yml");
		return f;
	}

	public static final FilenameFilter ymlFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".yml");
		}
	};
}