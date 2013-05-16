package me.desht.checkers.player;

import java.lang.ref.WeakReference;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersPlugin;
import me.desht.checkers.Messages;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.model.Move;
import me.desht.checkers.model.PlayerColour;
import me.desht.checkers.responses.DrawResponse;
import me.desht.checkers.responses.SwapResponse;
import me.desht.checkers.responses.YesNoResponse;
import me.desht.checkers.util.CheckersUtils;
import me.desht.dhutils.MiscUtil;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class HumanCheckersPlayer extends CheckersPlayer {

	private WeakReference<Player> player;

	public HumanCheckersPlayer(String name, CheckersGame game, PlayerColour colour) {
		super(name, game, colour);

		Player p = Bukkit.getPlayerExact(name);
		if (p == null) {
			throw new CheckersException("Player " + name + " is not online");
		}
		player = new WeakReference<Player>(p);
	}

	private Player getBukkitPlayer() {
		return player.get();
	}

	@Override
	public String getDisplayName() {
		return ChatColor.GOLD + getName() + ChatColor.RESET;
	}

	@Override
	public void validateAffordability(String error) {
		if (error == null) {
			error = "Game.cantAffordToJoin";
		}
		double stake = getGame().getStake();
		Economy economy = CheckersPlugin.getInstance().getEconomy();
		if (economy != null && !economy.has(getName(), stake)) {
			throw new CheckersException(Messages.getString(error, CheckersUtils.formatStakeStr(stake)));
		}
	}

	@Override
	public void validateInvited(String error) {
		String invited = getGame().getInvited();
		if (!invited.equals(CheckersGame.OPEN_INVITATION) && !invited.equalsIgnoreCase(getName())) { 
			throw new CheckersException(Messages.getString(error));
		}
	}

	@Override
	public void promptForFirstMove() {
		alert(Messages.getString("Game.started", getColour().getDisplayColour(), CheckersUtils.getWandDescription()));
	}

	@Override
	public void promptForNextMove() {
		Player p = getBukkitPlayer();
		if (p == null)
			return;
		Move m = getGame().getPosition().getLastMove();
		alert(Messages.getString("Game.playerPlayedMove", getColour().getOtherColour().getDisplayColour(), m.toString()));
	}

	@Override
	public void alert(String message) {
		Player p = getBukkitPlayer();
		if (p != null) {
			MiscUtil.alertMessage(p, Messages.getString("Game.alertPrefix", getGame().getName()) + message);
		}
	}

	@Override
	public void statusMessage(String message) {
		Player p = getBukkitPlayer();
		if (p != null) {
			MiscUtil.statusMessage(p, message);
		}
	}

	@Override
	public void replayMoves() {
		// nothing to do here
	}

	@Override
	public void cleanup() {
		player = null;
	}

	@Override
	public boolean isHuman() {
		return true;
	}

	@Override
	public void withdrawFunds(double amount) {
		Economy economy = CheckersPlugin.getInstance().getEconomy();
		economy.withdrawPlayer(getName(), amount);
		alert(Messages.getString("Game.paidStake", CheckersUtils.formatStakeStr(amount)));
	}

	@Override
	public void depositFunds(double amount) {
		Economy economy = CheckersPlugin.getInstance().getEconomy();
		economy.depositPlayer(getName(), amount);
	}

//	@Override
//	public void summonToGame() {
//		Player p = getBukkitPlayer();
//		if (p != null) {
//			getGame().getView().summonPlayer(p);
//		}
//	}

	@Override
	public void cancelOffers() {
		// TODO Auto-generated method stub
		Player p = getBukkitPlayer();
		if (p != null) {
			// making a move after a draw/swap/undo offer has been made is equivalent to declining the offer
			YesNoResponse.handleYesNoResponse(p, false);
		}
	}

	@Override
	public double getPayoutMultiplier() {
		return 2.0;
	}

	@Override
	public void drawOffered() {
		String offerer = getGame().getPlayer(getColour().getOtherColour()).getName();
		CheckersPlugin.getInstance().getResponseHandler().expect(getName(), new DrawResponse(getGame(), offerer));
		alert(Messages.getString("Game.drawOfferedOther", offerer));
		alert(Messages.getString("Misc.typeYesOrNo"));
	}

	@Override
	public void swapOffered() {
		String offerer = getGame().getPlayer(getColour().getOtherColour()).getName();
		CheckersPlugin.getInstance().getResponseHandler().expect(getName(), new SwapResponse(getGame(), offerer));
		alert(Messages.getString("Game.swapOfferedOther", offerer));
		alert(Messages.getString("Misc.typeYesOrNo"));
	}

	@Override
	public void undoLastMove() {
		// nothing to do here
	}

	@Override
	public void checkPendingAction() {
		// nothing to do here

	}

	@Override
	public void playEffect(String effect) {
		// TODO Auto-generated method stub

	}

}
