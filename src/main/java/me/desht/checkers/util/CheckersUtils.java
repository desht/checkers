package me.desht.checkers.util;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.dhutils.ItemNames;
import me.desht.dhutils.block.MaterialWithData;
import org.bukkit.material.MaterialData;

public class CheckersUtils {

	public static MaterialData getWandMaterial() {
		String wand = CheckersPlugin.getInstance().getConfig().getString("wand_item");
		if (wand.isEmpty() || wand.equalsIgnoreCase("*")) {
			return null;
		}
		MaterialWithData mat = MaterialWithData.get(wand);
		return mat == null ? null : mat.getMaterialData();
	}

	public static String getWandDescription() {
		MaterialData mat = getWandMaterial();

		return mat == null ? Messages.getString("Misc.anything") : ItemNames.lookup(mat.toItemStack());
	}

	public static String milliSecondsToHMS(long l) {
		l /= 1000;

		long secs = l % 60;
		long hrs = l / 3600;
		long mins = (l - (hrs * 3600)) / 60;

		return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs);
	}
}
