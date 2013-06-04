package me.desht.checkers.model;

import me.desht.checkers.Messages;

import org.bukkit.ChatColor;

public enum PlayerColour {
	NONE, BLACK, WHITE;

	public PlayerColour getOtherColour() {
		switch (this) {
		case WHITE: return BLACK;
		case BLACK: return WHITE;
		default: throw new IllegalArgumentException("unexpected colour");
		}
	}

	public int getIndex() {
		switch (this) {
		case WHITE: return 0;
		case BLACK: return 1;
		default: throw new IllegalArgumentException("unexpected colour");
		}
	}

	public String getColour() {
		switch (this) {
		case WHITE: return Messages.getString("Game.white");
		case BLACK: return Messages.getString("Game.black");
		default: throw new IllegalArgumentException("unexpected colour");
		}
	}

	public String getDisplayColour() {
		switch (this) {
		case WHITE: return ChatColor.WHITE + this.getColour();
		case BLACK: return ChatColor.DARK_GRAY + this.getColour();  // BLACK is just too dark for chat
		default: throw new IllegalArgumentException("unexpected colour");
		}
	}
}
