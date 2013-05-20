package me.desht.checkers.view.controlpanel;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.view.BoardViewManager;

import org.bukkit.event.player.PlayerInteractEvent;

public class TeleportButton extends AbstractSignButton {

	public TeleportButton(ControlPanel panel) {
		super(panel, "teleportOutBtn", "teleport", 4, 0);
	}

	@Override
	public void execute(PlayerInteractEvent event) {
		if (CheckersPlugin.getInstance().getConfig().getBoolean("teleporting")) {
			BoardViewManager.getManager().teleportOut(event.getPlayer());
		}
	}

	@Override
	public boolean isEnabled() {
		return CheckersPlugin.getInstance().getConfig().getBoolean("teleporting");
	}

}
