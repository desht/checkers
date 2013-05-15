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
import java.util.logging.Level;

import me.desht.checkers.commands.CreateBoardCommand;
import me.desht.checkers.commands.DeleteBoardCommand;
import me.desht.checkers.commands.ListBoardCommand;
import me.desht.checkers.commands.RedrawCommand;
import me.desht.checkers.listeners.BlockListener;
import me.desht.checkers.listeners.PlayerListener;
import me.desht.checkers.view.BoardView;
import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.PluginVersionListener;
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

public class CheckersPlugin extends JavaPlugin implements ConfigurationListener, PluginVersionListener {

	private static CheckersPlugin instance = null;

	private Persistence persistenceHandler;
	private ConfigurationManager configManager;
	private boolean startupFailed = false;
	private WorldEditPlugin worldEditPlugin;
	private Economy economy;
	private final CommandManager cmds = new CommandManager(this);
	private final ResponseHandler responseHandler = new ResponseHandler(this);

	@Override
	public void onLoad() {
		ConfigurationSerialization.registerClass(BoardView.class);
		ConfigurationSerialization.registerClass(PersistableLocation.class);
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
		LogUtils.setLogLevel(Level.FINE);  // TODO - temporary

		//		new PluginVersionChecker(this, this);

		DirectoryStructure.setup();

		Messages.init(getConfig().getString("locale", "default"));

		persistenceHandler = new Persistence();

		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(new PlayerListener(this), this);
		pm.registerEvents(new BlockListener(this), this);

		registerCommands();

		MessagePager.setPageCmd("/checkers page [#|n|p]");
		MessagePager.setDefaultPageSize(getConfig().getInt("pager.lines", 0));

		setupVault(pm);
		setupWorldEdit(pm);

		persistenceHandler.reload();
	}

	private void registerCommands() {
		cmds.registerCommand(new CreateBoardCommand());
		cmds.registerCommand(new DeleteBoardCommand());
		cmds.registerCommand(new RedrawCommand());
		cmds.registerCommand(new ListBoardCommand());
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

	public Persistence getPersistenceHandler() {
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
			LogUtils.fine("WorldEdit plugin detected: chess board terrain saving enabled.");
		} else {
			LogUtils.warning("WorldEdit plugin not detected: chess board terrain saving disabled.");
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

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onVersionChanged(int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getPreviousVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPreviousVersion(String currentVersion) {
		// TODO Auto-generated method stub
	}
}
