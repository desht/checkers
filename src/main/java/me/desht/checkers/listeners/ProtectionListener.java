package me.desht.checkers.listeners;

import java.util.Iterator;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;

public class ProtectionListener extends CheckersBaseListener {

	private final BoardViewManager manager;

	private boolean buildAllowed;
	private boolean burnAllowed;
	private boolean creaturesAllowed;
	private boolean pvpAllowed;
	private boolean miscDamageAllowed;
	private boolean explosionsAllowed;

	public ProtectionListener(CheckersPlugin plugin) {
		super(plugin);
		manager = BoardViewManager.getManager();
	}

	public void updateSettings() {
		ConfigurationSection cs = plugin.getConfig().getConfigurationSection("protection");

		buildAllowed = !cs.getBoolean("no_building");
		burnAllowed = !cs.getBoolean("no_burning");
		creaturesAllowed = !cs.getBoolean("no_creatures");
		pvpAllowed = !cs.getBoolean("no_pvp");
		miscDamageAllowed = !cs.getBoolean("no_misc_damage");
		explosionsAllowed = !cs.getBoolean("no_explosions");
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (manager.partOfBoard(event.getBlockClicked().getLocation(), 0) != null) {
			event.setCancelled(true);
			// seems just cancelling the event doesn't stop the bucket getting filled?
			event.setItemStack(new ItemStack(Material.BUCKET, 1));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (manager.partOfBoard(event.getBlockClicked().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (manager.partOfBoard(event.getFrom(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent event) {
		if (buildAllowed) {
			return;
		}
		BoardView bv = manager.partOfBoard(event.getBlock().getLocation(), 0);
		if (bv == null) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (buildAllowed) {
			return;
		}
		BoardView bv = manager.partOfBoard(event.getBlock().getLocation(), 0);
		if (bv == null) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (buildAllowed) {
			return;
		}
		BoardView bv = manager.partOfBoard(event.getBlock().getLocation(), 0);
		if (bv == null) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (burnAllowed) {
			return;
		}
		if (manager.partOfBoard(event.getBlock().getLocation(), 1) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (burnAllowed) {
			return;
		}
		if (manager.partOfBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (manager.partOfBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		if (manager.partOfBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Cancelling liquid flow events makes it possible to use water & lava for walls & pieces.
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		if (manager.partOfBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		} else if (manager.partOfBoard(event.getToBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Snow doesn't usually form on boards due to the high light level.  But if the light level
	 * is dimmed, we might see boards getting covered.
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
		if (manager.partOfBoard(event.getBlock().getLocation(), 0) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (creaturesAllowed) {
			return;
		}

		Location loc = event.getLocation();
		for (BoardView bv : manager.listBoardViews()) {
			if (bv.getBoard().isPartOfBoard(loc)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if (!(event.getTarget() instanceof Player) || creaturesAllowed) {
			return;
		}

		if (manager.partOfBoard(event.getEntity().getLocation(), 0) != null
				|| manager.partOfBoard(event.getTarget().getLocation(), 0) != null) {
			event.setCancelled(true);
			// don't remove tame creatures
			if (!(event.getEntity() instanceof Tameable && ((Tameable) event.getEntity()).isTamed())) {
				event.getEntity().remove();
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (explosionsAllowed) {
			return;
		}

		Iterator<Block>	iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Location loc = iter.next().getLocation();
			if (manager.partOfBoard(loc) != null) {
				iter.remove();
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent dbeEvent = (EntityDamageByEntityEvent) event;
			if (dbeEvent.getDamager() == null) {
				return;
			}
			if (isAllowedPlayerAttack(dbeEvent.getDamager()) || isAllowedMonsterAttack(dbeEvent.getDamager())) {
				return;
			}

			Location attackerLoc = dbeEvent.getDamager().getLocation();
			Location defenderLoc = event.getEntity().getLocation();
			for (BoardView bv : manager.listBoardViews()) {
				if (bv.getBoard().isPartOfBoard(defenderLoc) || bv.getBoard().isPartOfBoard(attackerLoc)) {
					event.setCancelled(true);
					if ((event.getEntity() instanceof Player) && // victim is a player
							!(dbeEvent.getDamager() instanceof Player) // and attacker is a monster
							&& dbeEvent.getDamager() instanceof LivingEntity) {
						dbeEvent.getDamager().remove();
					}
					return;
				}
			}

		}
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		if (miscDamageAllowed) {
			return;
		}

		if (event.getCause() == DamageCause.SUFFOCATION) {
			BoardView bv = manager.partOfBoard(event.getEntity().getLocation(), 0);
			if (bv != null) {
				// player must have had a piece placed on them
				displacePlayerSafely(event);
				event.setCancelled(true);
			}
		} else {
			// any other damage to a player while on a board, e.g. falling off of a piece or viewing platform,
			// cactus/lava/fire on pieces, etc..
			BoardView bv = manager.partOfBoard(event.getEntity().getLocation(), 1);
			if (bv != null) {
				event.setCancelled(true);
				event.getEntity().setFireTicks(0);
			}
		}
	}

	/**
	 * Safely displace a player out of the way so they are not entombed by a piece
	 * 
	 * @param event	The suffocation event that triggered this
	 */
	private void displacePlayerSafely(EntityDamageEvent event) {
		final int MAX_DIST = 100;

		Player p = (Player) event.getEntity();
		Location loc = p.getLocation().clone();
		int n = 0;
		do {
			loc.add(0, 0, -1); // east
		} while (loc.getBlock().getTypeId() != 0 && loc.getBlock().getRelative(BlockFace.UP).getTypeId() != 0
				&& n < MAX_DIST);
		if (n >= MAX_DIST) {
			MiscUtil.errorMessage(p, Messages.getString("Misc.goingToSpawn"));
			p.teleport(p.getWorld().getSpawnLocation());
		} else {
			p.teleport(loc);
		}
	}

	private boolean isAllowedMonsterAttack(Entity damager) {
		return !(damager instanceof Player) && damager instanceof LivingEntity && creaturesAllowed;
	}

	private boolean isAllowedPlayerAttack(Entity damager) {
		return damager instanceof Player && pvpAllowed;
	}
}
