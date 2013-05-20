package me.desht.checkers.util;

import java.text.DecimalFormat;

import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.MaterialWithData;
import net.milkbowl.vault.economy.Economy;

public class CheckersUtils {

	public static String formatStakeStr(double stake) {
		Economy economy = CheckersPlugin.getInstance().getEconomy();
		try {
			if (economy != null && economy.isEnabled()) {
				return economy.format(stake);
			}
		} catch (Exception e) {
			LogUtils.warning("Caught exception from " + economy.getName() + " while trying to format quantity " + stake + ":");
			e.printStackTrace();
			LogUtils.warning("ChessCraft will continue but you should verify your economy plugin configuration.");
		}
		return new DecimalFormat("#0.00").format(stake);
	}

	public static int getWandId() {
		String wand = CheckersPlugin.getInstance().getConfig().getString("wand_item"); //$NON-NLS-1$
		if (wand.isEmpty() || wand.equalsIgnoreCase("*")) {
			return -1;
		}
		MaterialWithData mat = MaterialWithData.get(wand);
		return mat == null ? 0 : mat.getId();
	}

	public static String getWandDescription() {
		int id = getWandId();

		return id < 0 ? Messages.getString("Misc.anything") : MaterialWithData.get(id).toString();
	}

	public static String milliSecondsToHMS(long l) {
		l /= 1000;

		long secs = l % 60;
		long hrs = l / 3600;
		long mins = (l - (hrs * 3600)) / 60;

		return String.format("%1$02d:%2$02d:%3$02d", hrs, mins, secs);
	}
}
