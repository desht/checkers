package me.desht.checkers.listeners;

import java.util.HashMap;
import java.util.Map;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.dhutils.Duration;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerTracker extends CheckersBaseListener {

	private final Map<String, PersistableLocation> lastPos = new HashMap<String, PersistableLocation>();
	private final Map<String, Long> loggedOutAt = new HashMap<String, Long>();

	public PlayerTracker(CheckersPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		StringBuilder games = new StringBuilder();
		String who = event.getPlayer().getName();
		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			CheckersPlayer cp = game.getPlayer(who);
			if (cp != null) {
				playerRejoined(who);
				CheckersPlayer other = game.getPlayer(cp.getColour().getOtherColour());
				if (other != null) {
					other.alert(Messages.getString("Game.playerBack", who));
				}
				games.append(" ").append(game.getName());
			}
		}
		if (games.length() > 0) {
			MiscUtil.alertMessage(event.getPlayer(), Messages.getString("Game.currentGames", games));
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		String who = event.getPlayer().getName();
		String timeout = plugin.getConfig().getString("forfeit_timeout");
		Duration duration;
		try {
			duration = new Duration(timeout);
		} catch (IllegalArgumentException e) {
			LogUtils.warning("invalid value for forfeit_timeout: " + timeout);
			duration = new Duration(0);
		}
		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			if (game.hasPlayer(who)) {
				game.playerLeft(who);
				playerLeft(who);
				if (duration.getTotalDuration() > 0 && game.getState() == GameState.RUNNING) {
					game.alert(Messages.getString("Game.playerQuit", who, duration.getTotalDuration() / 1000));
				}
			}
		}
		MessagePager.deletePager(event.getPlayer());
	}

	public void teleportPlayer(Player player, Location loc) {
		setLastPos(player, player.getLocation());
		plugin.getFX().playEffect(player.getLocation(), "teleport_out");
		player.teleport(loc);
		plugin.getFX().playEffect(player.getLocation(), "teleport_in");
	}

	public Location getLastPos(Player player) {
		if (!lastPos.containsKey(player.getName())) {
			return null;
		} else {
			return lastPos.get(player.getName()).getLocation();
		}
	}

	private void setLastPos(Player player, Location loc) {
		lastPos.put(player.getName(), new PersistableLocation(loc));
	}

	public void playerLeft(String who) {
		loggedOutAt.put(who, System.currentTimeMillis());
	}

	public void playerRejoined(String who) {
		loggedOutAt.remove(who);
	}

	public long getPlayerLeftAt(String who) {
		return loggedOutAt.containsKey(who) ? loggedOutAt.get(who) : 0;
	}
}
