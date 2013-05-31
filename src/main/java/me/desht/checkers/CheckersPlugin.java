package me.desht.checkers;

/*
    This file is part of Checkers

    Checkers is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Checkers is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Checkers.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;

import me.desht.checkers.ai.AIFactory;
import me.desht.checkers.commands.AbstractCheckersCommand;
import me.desht.checkers.listeners.FlightListener;
import me.desht.checkers.listeners.PlayerListener;
import me.desht.checkers.listeners.PlayerTracker;
import me.desht.checkers.listeners.ProtectionListener;
import me.desht.checkers.listeners.WorldListener;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.Duration;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.SpecialFX;
import me.desht.dhutils.commands.CommandManager;
import me.desht.dhutils.nms.NMSHelper;
import me.desht.dhutils.responsehandler.ResponseHandler;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class CheckersPlugin extends JavaPlugin implements ConfigurationListener {

	private static CheckersPlugin instance = null;

	private boolean startupFailed = false;
	private final PersistenceHandler persistenceHandler = new PersistenceHandler();
	private final CommandManager cmds = new CommandManager(this);
	private final ResponseHandler responseHandler = new ResponseHandler(this);
	private ConfigurationManager configManager;
	private WorldEditPlugin worldEditPlugin;
	private Economy economy;
	private SpecialFX fx;
	private TickTask tickTask;
	private FlightListener flightListener;
	private PlayerTracker playerTracker;
	private AIFactory aiFactory;

	@Override
	public void onLoad() {
		ConfigurationSerialization.registerClass(BoardView.class);
		ConfigurationSerialization.registerClass(PersistableLocation.class);
		ConfigurationSerialization.registerClass(TwoPlayerClock.class);
	}

	@Override
	public void onEnable() {
		instance = this;

		LogUtils.init(this);

		if (!setupNMS()) {
			return;
		}

		configManager = new ConfigurationManager(this, this);

		MiscUtil.init(this);
		MiscUtil.setColouredConsole(getConfig().getBoolean("coloured_console"));

		LogUtils.setLogLevel(getConfig().getString("log_level", "INFO"));

		DirectoryStructure.setup();

		Messages.init(getConfig().getString("locale", "default"));

		PluginManager pm = this.getServer().getPluginManager();
		playerTracker = new PlayerTracker(this);
		flightListener = new FlightListener(this);
		pm.registerEvents(new PlayerListener(this), this);
		pm.registerEvents(new ProtectionListener(this), this);
		pm.registerEvents(new WorldListener(this), this);
		pm.registerEvents(playerTracker, this);
		pm.registerEvents(flightListener, this);

		registerCommands();

		MessagePager.setPageCmd("/checkers page [#|n|p]");
		MessagePager.setDefaultPageSize(getConfig().getInt("pager.lines", 0));

		setupVault(pm);
		setupWorldEdit(pm);

		aiFactory = new AIFactory();

		fx = new SpecialFX(getConfig().getConfigurationSection("effects"));

		persistenceHandler.reload();

		tickTask = new TickTask();
		tickTask.runTaskTimer(this, 20L, 20L);
	}

	private void registerCommands() {
		@SuppressWarnings("unused")
		Class<AbstractCheckersCommand> c = AbstractCheckersCommand.class;
		cmds.registerAllCommands("me.desht.checkers.commands");
	}

	@Override
	public void onDisable() {
		if (startupFailed) return;

		persistenceHandler.save();

		instance = null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return cmds.dispatch(sender, command, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		return cmds.onTabComplete(sender, command, label, args);
	}

	public static CheckersPlugin getInstance() {
		return instance;
	}

	public PersistenceHandler getPersistenceHandler() {
		return persistenceHandler;
	}

	public WorldEditPlugin getWorldEdit() {
		return worldEditPlugin;
	}

	public Economy getEconomy() {
		return economy;
	}

	public ConfigurationManager getConfigManager() {
		return configManager;
	}

	public ResponseHandler getResponseHandler() {
		return responseHandler;
	}

	public SpecialFX getFX() {
		return fx;
	}

	public PlayerTracker getPlayerTracker() {
		return playerTracker;
	}

	private void setupVault(PluginManager pm) {
		Plugin vault =  pm.getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			LogUtils.fine("Loaded Vault v" + vault.getDescription().getVersion());
			if (!setupEconomy()) {
				LogUtils.warning("No economy plugin detected - game stakes not available");
			}
		} else {
			LogUtils.warning("Vault not loaded: game stakes not available");
		}
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	private void setupWorldEdit(PluginManager pm) {
		Plugin p = pm.getPlugin("WorldEdit");
		if (p != null && p instanceof WorldEditPlugin) {
			worldEditPlugin = (WorldEditPlugin) p;
			LogUtils.fine("WorldEdit plugin detected: board terrain saving enabled.");
		} else {
			LogUtils.warning("WorldEdit plugin not detected: board terrain saving disabled.");
		}
	}

	private boolean setupNMS() {
		try {
			NMSHelper.init(this);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			String url = getDescription().getWebsite();
			LogUtils.severe("Checkers version " + getDescription().getVersion() + " is not compatible with this CraftBukkit version.");
			LogUtils.severe("Check " + url + " for information on updated builds.");
			LogUtils.severe("Plugin disabled.");
			startupFailed = true;
			setEnabled(false);
			return false;
		}
	}

	private void updateAllControlPanels() {
		for (BoardView bv : BoardViewManager.getManager().listBoardViews()) {
			bv.getControlPanel().repaintControls();
			bv.getControlPanel().updateClocks();
		}
	}

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.startsWith("auto_delete.") || key.equals("forfeit_timeout")) {
			String dur = newVal.toString();
			try {
				new Duration(dur);
			} catch (NumberFormatException e) {
				throw new DHUtilsException("Invalid duration: " + dur);
			}
		} else if (key.startsWith("effects.") && getConfig().get(key) instanceof String) {
			// this will throw an IllegalArgumentException if the value is no good
			SpecialFX.SpecialEffect e = fx.new SpecialEffect(newVal.toString(), 1.0f);
			e.play(null);
		} else if (key.equals("version")) {
			throw new DHUtilsException("'version' config item may not be changed");
		}
	}

	@Override
		public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
			if (key.equalsIgnoreCase("locale")) {
				Messages.setMessageLocale(newVal.toString());
				// redraw control panel signs in the right language
				updateAllControlPanels();
			} else if (key.equalsIgnoreCase("log_level")) {
				LogUtils.setLogLevel(newVal.toString());
			} else if (key.equalsIgnoreCase("teleporting")) {
				updateAllControlPanels();
			} else if (key.equalsIgnoreCase("flying.allowed")) {
				flightListener.setEnabled((Boolean) newVal);
			} else if (key.equalsIgnoreCase("flying.captive")) {
				flightListener.setCaptive((Boolean) newVal);
			} else if (key.equalsIgnoreCase("flying.upper_limit") || key.equalsIgnoreCase("flying.outer_limit")) {
				flightListener.recalculateFlightRegions();
			} else if (key.equalsIgnoreCase("flying.fly_speed") || key.equalsIgnoreCase("flying.walk_speed")) {
				flightListener.updateSpeeds();
			} else if (key.equalsIgnoreCase("pager.enabled")) {
				if ((Boolean) newVal) {
					MessagePager.setDefaultPageSize();
				} else {
					MessagePager.setDefaultPageSize(Integer.MAX_VALUE);
				}
			} else if (key.startsWith("effects.")) {
				fx = new SpecialFX(getConfig().getConfigurationSection("effects"));
	//		} else if (key.startsWith("database.")) {
	//			Results.shutdown();
	//			if (Results.getResultsHandler() == null) {
	//				LogUtils.warning("DB connection cannot be re-established.  Check your settings.");
	//			}
			} else if (key.equals("coloured_console")) {
				MiscUtil.setColouredConsole((Boolean)newVal);
			}
		}

	public AIFactory getAIFactory() {
		return aiFactory;
	}
}
