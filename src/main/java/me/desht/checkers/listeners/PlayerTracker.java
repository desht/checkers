package me.desht.checkers.listeners;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.game.CheckersGame.GameState;
import me.desht.checkers.game.CheckersGameManager;
import me.desht.checkers.player.CheckersPlayer;
import me.desht.dhutils.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTracker extends CheckersBaseListener {

	private final Map<UUID, PersistableLocation> lastPos = new HashMap<UUID, PersistableLocation>();
	private final Map<UUID, Long> loggedOutAt = new HashMap<UUID, Long>();

	public PlayerTracker(CheckersPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		StringBuilder games = new StringBuilder();
		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			CheckersPlayer cp = game.getPlayer(event.getPlayer().getUniqueId().toString());
			if (cp != null) {
				playerRejoined(event.getPlayer());
				CheckersPlayer other = game.getPlayer(cp.getColour().getOtherColour());
				if (other != null) {
					other.alert(Messages.getString("Game.playerBack", event.getPlayer().getDisplayName()));
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
		String timeout = plugin.getConfig().getString("forfeit_timeout");
		Duration duration;
		try {
			duration = new Duration(timeout);
		} catch (IllegalArgumentException e) {
			LogUtils.warning("invalid value for forfeit_timeout: " + timeout);
			duration = new Duration(0);
		}
		for (CheckersGame game : CheckersGameManager.getManager().listGames()) {
			if (game.hasPlayer(event.getPlayer())) {
				playerLeft(event.getPlayer());
				if (duration.getTotalDuration() > 0 && game.getState() == GameState.RUNNING) {
					game.alert(Messages.getString("Game.playerQuit", event.getPlayer().getDisplayName(), duration.getTotalDuration() / 1000));
				}
				game.playerLeft(event.getPlayer());
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
		if (!lastPos.containsKey(player.getUniqueId())) {
			return null;
		} else {
			return lastPos.get(player.getUniqueId()).getLocation();
		}
	}

	private void setLastPos(Player player, Location loc) {
		lastPos.put(player.getUniqueId(), new PersistableLocation(loc));
	}

	public void playerLeft(Player player) {
		loggedOutAt.put(player.getUniqueId(), System.currentTimeMillis());
	}

	public void playerRejoined(Player player) {
		loggedOutAt.remove(player.getUniqueId());
	}

	public long getPlayerLeftAt(String who) {
		if (!MiscUtil.looksLikeUUID(who)) return 0;
		UUID uuid = UUID.fromString(who);
		return loggedOutAt.containsKey(uuid) ? loggedOutAt.get(uuid) : 0;
	}
}
