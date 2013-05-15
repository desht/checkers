package me.desht.checkers;

import java.io.File;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

public interface CheckersPersistable extends ConfigurationSerializable {
	public String getName();			// for determining save file names
	public File getSaveDirectory();		// directory where save files are placed
}
