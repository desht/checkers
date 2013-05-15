package me.desht.checkers.model;

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

	public String getDisplayColour() {
		switch (this) {
		case WHITE: return ChatColor.WHITE.toString();
		case BLACK: return ChatColor.BLACK.toString();
		default: throw new IllegalArgumentException("unexpected colour");
		}
	}
}
