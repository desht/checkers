package me.desht.checkers.ai;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.DirectoryStructure;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.PlayerColour;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.JARUtil;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class AIFactory {
	private static final String AI_ALIASES_FILE = "AI.yml";
	private static final String AI_CORE_DEFS = "/AI_settings.yml";

	private final HashMap<String, CheckersAI> runningAIs = new HashMap<String, CheckersAI>();
	private final Map<String, AIDefinition> allAliases = new HashMap<String, AIDefinition>();
	private final Map<String, AIDefinition> coreDefs = new HashMap<String, AIDefinition>();

	public AIFactory() {
		loadAIDefinitions();
	}

	public CheckersAI getNewAI(String aiName, CheckersGame game, PlayerColour colour) {
		return getNewAI(game, aiName, false, colour);
	}

	public CheckersAI getNewAI(CheckersGame game, String aiName, boolean forceNew, PlayerColour colour) {
		if (!forceNew) {
			int max = CheckersPlugin.getInstance().getConfig().getInt("ai.max_ai_games");
			if (max == 0) {
				throw new CheckersException(Messages.getString("AI.AIdisabled"));
			} else if (runningAIs.size() >= max) {
				throw new CheckersException(Messages.getString("AI.noAvailableAIs", max));
			}
		}

		AIDefinition aiDef = getAIDefinition(aiName);
		if (aiDef == null) {
			throw new CheckersException(Messages.getString("AI.AInotFound", aiName));
		} else if (runningAIs.containsKey(aiDef.getName())) {
			throw new CheckersException(Messages.getString("AI.AIbusy"));
		}
		CheckersAI ai = aiDef.createInstance(game, colour);
		runningAIs.put(aiName, ai);

		return ai;
	}

	void deleteAI(CheckersAI ai) {
		runningAIs.remove(ai.getName());
	}

	/**
	 * Check if the given AI name is available (i.e. not in a game).
	 *
	 * @param aiName the AI name
	 * @return true if this AI is available
	 */
	public boolean isAvailable(String aiName) {
		return !runningAIs.containsKey(aiName);
	}

	/**
	 * Clear down all running AIs. Called on disable.
	 */
	public void clearDown() {
		List<CheckersAI> l = new ArrayList<CheckersAI>();
		for (Entry<String, CheckersAI> e : runningAIs.entrySet()) {
			l.add(e.getValue());
		}
		for (CheckersAI ai : l) {
			ai.delete();
		}
	}

	public List<AIDefinition> listAIDefinitions() {
		return listAIDefinitions(true);
	}
	public List<AIDefinition> listAIDefinitions(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(allAliases.keySet());
			List<AIDefinition> res = new ArrayList<AIDefinition>();
			for (String name : sorted) {
				res.add(allAliases.get(name));
			}
			return res;
		} else {
			return new ArrayList<AIDefinition>(allAliases.values());
		}
	}

	/**
	 * Return the AI definition for the given AI name.
	 *
	 * @param aiName the AI name
	 * @return the AI definition
	 */
	public AIDefinition getAIDefinition(String aiName) {
		if (aiName.startsWith(CheckersAI.AI_PREFIX)) {
			aiName = aiName.substring(CheckersAI.AI_PREFIX.length());
		}
		if (allAliases.containsKey(aiName)) {
			return allAliases.get(aiName);
		} else {
			return coreDefs.get(aiName);
		}
	}
	public AIDefinition getAIDefinition(String aiName, boolean force) {
		AIDefinition def = getAIDefinition(aiName);
		if (def == null && force) {
			throw new CheckersException(Messages.getString("AI.AInotFound", aiName));
		}
		return def;
	}

	/**
	 * Get the name of a random free and enabled AI.
	 *
	 * @return the AI name
	 * @throws CheckersException if there are no free AIs
	 */
	public String getFreeAIName() {
		List<String> free = new ArrayList<String>();
		for (String k : allAliases.keySet()) {
			if (isAvailable(k) && allAliases.get(k).isEnabled()) {
				free.add(k);
			}
		}
		if (free.size() == 0)
			throw new CheckersException(Messages.getString("AI.noAvailableAIs", allAliases.size()));

		return CheckersAI.AI_PREFIX + free.get(new Random().nextInt(free.size()));
	}

	public void loadAIDefinitions() {
		YamlConfiguration coreAIdefs;

		allAliases.clear();

		// first pull in the core definitions from the JAR file resource...
		try {
			JARUtil ju = new JARUtil(CheckersPlugin.getInstance());
			InputStream in = ju.openResourceNoCache(AI_CORE_DEFS);
			coreAIdefs = YamlConfiguration.loadConfiguration(in);
		} catch (Exception e) {
			LogUtils.severe("Can't load AI definitions: " + e.getMessage());
			return;
		}

		// now load the aliases file
		File aiAliasesFile = new File(DirectoryStructure.getPluginDirectory(), AI_ALIASES_FILE);
		Configuration aliasesConf = YamlConfiguration.loadConfiguration(aiAliasesFile);
		for (String alias : aliasesConf.getKeys(false)) {
			ConfigurationSection aliasConf = aliasesConf.getConfigurationSection(alias);
			String ai = aliasConf.getString("ai");
			if (!coreAIdefs.contains(ai)) {
				LogUtils.warning("AI aliases file " + aiAliasesFile + " refers to non-existent AI definition: " + alias);
				continue;
			}
			ConfigurationSection core = coreAIdefs.getConfigurationSection(ai);
			for (String key : core.getKeys(false)) {
				if (!aliasConf.contains(key)) {
					aliasConf.set(key, core.get(key));
				}
			}

			try {
				AIDefinition aiDef = new AIDefinition(alias, aliasConf);
				allAliases.put(alias, aiDef);
				coreDefs.put(ai, aiDef);
			} catch (ClassNotFoundException e) {
				LogUtils.warning("unknown class '" + aliasConf.getString("class") + "' for AI [" + alias + "]: skipped");
			} catch (ClassCastException e) {
				LogUtils.warning("class '" + aliasConf.getString("class") + "'for AI [" + alias + "] is not a CheckersAI subclass: skipped");
			}
		}

		Debugger.getInstance().debug("Loaded " + allAliases.size() + " AI definitions");
	}

	public class AIDefinition {
		private final ConfigurationSection params;
		private final Class<? extends CheckersAI> aiImplClass;
		private final String name;

		public AIDefinition(String name, ConfigurationSection conf) throws ClassNotFoundException {
			this.name = name;
			this.params = new MemoryConfiguration();

			String className = conf.getString("class", "me.desht.checkers.ai.engines.Standard");
			if (className.indexOf('.') == -1)
				className = "me.desht.checkers.ai.engines." + className;
			aiImplClass = Class.forName(className).asSubclass(CheckersAI.class);

			for (String k : conf.getKeys(false)) {
				params.set(k, conf.get(k));
			}

			Debugger.getInstance().debug(2, "loaded " + aiImplClass.getName() + " for AI " + name);
		}

		public CheckersAI createInstance(CheckersGame game, PlayerColour colour) {
			try {
				Constructor<? extends CheckersAI> ctor = aiImplClass.getDeclaredConstructor(String.class, CheckersGame.class, PlayerColour.class, ConfigurationSection.class);
				return ctor.newInstance(name, game, colour, params);
			} catch (Exception e) {
				LogUtils.warning("Caught " + e.getClass().getName() + " while loading AI " + name);
				LogUtils.warning("  Exception message: " + e.getMessage());
				e.printStackTrace();
				throw new CheckersException("internal error while creating AI " + name);
			}
		}

		public String getImplClassName() {
			return aiImplClass.getSimpleName();
		}

		public String getName() {
			return name;
		}

		public String getDisplayName() {
			return CheckersAI.AI_PREFIX + name;
		}

		public List<String> getDetails() {
			List<String> res = new ArrayList<String>();
			res.add("AI " + getDisplayName() + " (" + getImplClassName() + ") :");
			for (String k : MiscUtil.asSortedList(params.getKeys(false))) {
				res.add(ChatColor.DARK_RED + "* " + ChatColor.WHITE + k + ": " + ChatColor.YELLOW + params.get(k));
			}
			return res;
		}

		public String getEngine() {
			return getParams().getString("engine");
		}

		public double getPayoutMultiplier() {
			return getParams().getDouble("payout_multiplier", 1.0);
		}

		public String getComment() {
			return getParams().getString("comment");
		}

		public boolean isEnabled() {
			return getParams().getBoolean("enabled", true);
		}

		public ConfigurationSection getParams() {
			return params;
		}
	}

}
