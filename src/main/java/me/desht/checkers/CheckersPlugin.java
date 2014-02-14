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

import java.io.IOException;
import java.util.List;

import me.desht.checkers.ai.AIFactory;
import me.desht.checkers.commands.BoardSaveCommand;
import me.desht.checkers.commands.BoardSetCommand;
import me.desht.checkers.commands.CreateBoardCommand;
import me.desht.checkers.commands.CreateGameCommand;
import me.desht.checkers.commands.DeleteBoardCommand;
import me.desht.checkers.commands.DeleteGameCommand;
import me.desht.checkers.commands.GetcfgCommand;
import me.desht.checkers.commands.InviteCommand;
import me.desht.checkers.commands.JoinCommand;
import me.desht.checkers.commands.ListAICommand;
import me.desht.checkers.commands.ListBoardCommand;
import me.desht.checkers.commands.ListGameCommand;
import me.desht.checkers.commands.ListStylesCommand;
import me.desht.checkers.commands.ListTopCommand;
import me.desht.checkers.commands.MoveCommand;
import me.desht.checkers.commands.NoCommand;
import me.desht.checkers.commands.OfferDrawCommand;
import me.desht.checkers.commands.OfferSwapCommand;
import me.desht.checkers.commands.RedrawCommand;
import me.desht.checkers.commands.ReloadCommand;
import me.desht.checkers.commands.ResignCommand;
import me.desht.checkers.commands.SaveCommand;
import me.desht.checkers.commands.SetcfgCommand;
import me.desht.checkers.commands.StakeCommand;
import me.desht.checkers.commands.StartCommand;
import me.desht.checkers.commands.TeleportCommand;
import me.desht.checkers.commands.TimeControlCommand;
import me.desht.checkers.commands.UndoCommand;
import me.desht.checkers.commands.WinCommand;
import me.desht.checkers.commands.YesCommand;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.listeners.FlightListener;
import me.desht.checkers.listeners.PlayerListener;
import me.desht.checkers.listeners.PlayerTracker;
import me.desht.checkers.listeners.ProtectionListener;
import me.desht.checkers.listeners.WorldListener;
import me.desht.checkers.model.rules.GameRules;
import me.desht.checkers.results.Results;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.*;
import me.desht.dhutils.commands.CommandManager;
import me.desht.dhutils.nms.NMSHelper;
import me.desht.dhutils.responsehandler.ResponseHandler;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Plotter;

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
	private SMSIntegration sms;
	private DynmapIntegration dynmapIntegration;

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

		configManager = new ConfigurationManager(this, this);

		Debugger.getInstance().setPrefix("[Checkers] ");
		Debugger.getInstance().setLevel(getConfig().getInt("debug_level"));
		Debugger.getInstance().setTarget(getServer().getConsoleSender());

		if (!setupNMS()) {
			return;
		}

		MiscUtil.init(this);
		MiscUtil.setColouredConsole(getConfig().getBoolean("coloured_console"));

		DirectoryStructure.setup(this);

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

		// this will cause saved results data to start being pulled in (async)
        Results.getResultsHandler();

		setupVault(pm);
		setupWorldEdit(pm);
		setupSMSIntegration(pm);
		setupDynmap(pm);

		GameRules.registerRulesets();

		aiFactory = new AIFactory();

		fx = new SpecialFX(getConfig().getConfigurationSection("effects"));

		persistenceHandler.reload();
		if (sms != null) {
			sms.setAutosave(true);
		}
		if (dynmapIntegration != null && dynmapIntegration.isEnabled()) {
			dynmapIntegration.setActive(true);
		}

		setupMetrics();

		tickTask = new TickTask();
		tickTask.runTaskTimer(this, 20L, 20L);
	}

	private void registerCommands() {
		cmds.registerCommand(new BoardSaveCommand());
		cmds.registerCommand(new BoardSetCommand());
		cmds.registerCommand(new CreateBoardCommand());
		cmds.registerCommand(new CreateGameCommand());
		cmds.registerCommand(new DeleteBoardCommand());
		cmds.registerCommand(new DeleteGameCommand());
		cmds.registerCommand(new GetcfgCommand());
		cmds.registerCommand(new InviteCommand());
		cmds.registerCommand(new JoinCommand());
		cmds.registerCommand(new ListAICommand());
		cmds.registerCommand(new ListBoardCommand());
		cmds.registerCommand(new ListGameCommand());
		cmds.registerCommand(new ListStylesCommand());
		cmds.registerCommand(new ListTopCommand());
		cmds.registerCommand(new MoveCommand());
		cmds.registerCommand(new NoCommand());
		cmds.registerCommand(new OfferDrawCommand());
		cmds.registerCommand(new OfferSwapCommand());
		cmds.registerCommand(new RedrawCommand());
		cmds.registerCommand(new ReloadCommand());
		cmds.registerCommand(new ResignCommand());
		cmds.registerCommand(new SaveCommand());
		cmds.registerCommand(new SetcfgCommand());
		cmds.registerCommand(new StakeCommand());
		cmds.registerCommand(new StartCommand());
		cmds.registerCommand(new TeleportCommand());
		cmds.registerCommand(new TimeControlCommand());
		cmds.registerCommand(new UndoCommand());
		cmds.registerCommand(new WinCommand());
		cmds.registerCommand(new YesCommand());
	}

	@Override
	public void onDisable() {
		if (startupFailed) return;

		Results.getResultsHandler().shutdown();

		if (dynmapIntegration != null) {
			dynmapIntegration.setActive(false);
		}

		persistenceHandler.save();

		tickTask.cancel();

		instance = null;

		Debugger.getInstance().debug("checkers disable complete");
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

	public DynmapIntegration getDynmapIntegration() {
		return dynmapIntegration;
	}

	private void setupMetrics() {
		if (!getConfig().getBoolean("mcstats")) {
			return;
		}
		try {
			Metrics metrics = new Metrics(this);

			metrics.createGraph("Boards Created").addPlotter(new Plotter() {
				@Override
				public int getValue() { return BoardViewManager.getManager().listBoardViews().size();	}
			});
			metrics.createGraph("Games in Progress").addPlotter(new Plotter() {
				@Override
				public int getValue() { return CheckersGameManager.getManager().listGames().size(); }
			});
			metrics.start();
		} catch (IOException e) {
			LogUtils.warning("Can't submit metrics data: " + e.getMessage());
		}
	}

	private void setupVault(PluginManager pm) {
		Plugin vault =  pm.getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			Debugger.getInstance().debug("Loaded Vault v" + vault.getDescription().getVersion());
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
			Debugger.getInstance().debug("WorldEdit plugin detected: board terrain saving enabled.");
		} else {
			LogUtils.warning("WorldEdit plugin not detected: board terrain saving disabled.");
		}
	}

	private void setupSMSIntegration(PluginManager pm) {
		try {
			Plugin p = pm.getPlugin("ScrollingMenuSign");
			if (p != null && p instanceof ScrollingMenuSign) {
				sms = new SMSIntegration(this, (ScrollingMenuSign) p);
				Debugger.getInstance().debug("ScrollingMenuSign plugin detected: Checkers menus created.");
			} else {
				Debugger.getInstance().debug("ScrollingMenuSign plugin not detected.");
			}
		} catch (NoClassDefFoundError e) {
			// this can happen if ScrollingMenuSign was disabled
			LogUtils.warning("ScrollingMenuSign plugin not detected (NoClassDefFoundError caught).");
		}
	}

	private void setupDynmap(PluginManager pm) {
		Plugin p = pm.getPlugin("dynmap");
		if (p != null) {
			dynmapIntegration = new DynmapIntegration(this, (DynmapAPI) p);
			Debugger.getInstance().debug("dynmap plugin detected.  Boards and games will be labelled.");
		} else {
			Debugger.getInstance().debug("dynmap plugin not detected.");
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
		} else if (key.equals("database.table_prefix") && newVal.toString().isEmpty()) {
			throw new DHUtilsException("'database.table_prefix' may not be empty");
		} else if (key.startsWith("default_rules")) {
			DHValidate.isTrue(GameRules.getRules(newVal.toString()) != null, "Unknown ruleset '" + newVal + "'");
		}
	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equalsIgnoreCase("locale")) {
			Messages.setMessageLocale(newVal.toString());
			// redraw control panel signs in the right language
			updateAllControlPanels();
		} else if (key.equalsIgnoreCase("debug_level")) {
			Debugger.getInstance().setLevel((Integer) newVal);
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
		} else if (key.startsWith("database.")) {
			Results.getResultsHandler().shutdown();
			if (Results.getResultsHandler() == null) {
				LogUtils.warning("DB connection cannot be re-established.  Check your settings.");
			}
		} else if (key.equals("coloured_console")) {
			MiscUtil.setColouredConsole((Boolean)newVal);
		} else if (key.startsWith("dynmap.") && dynmapIntegration != null) {
			dynmapIntegration.processConfig();
			dynmapIntegration.setActive(dynmapIntegration.isEnabled());
		}
	}

	public AIFactory getAIFactory() {
		return aiFactory;
	}
}
