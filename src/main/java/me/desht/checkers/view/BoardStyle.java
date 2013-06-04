package me.desht.checkers.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import me.desht.checkers.CheckersException;
import me.desht.checkers.CheckersValidate;
import me.desht.checkers.DirectoryStructure;
import me.desht.checkers.PersistenceHandler;
import me.desht.dhutils.AttributeCollection;
import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.block.MaterialWithData;

import org.bukkit.configuration.Configuration;

public class BoardStyle implements Comparable<BoardStyle>, ConfigurationListener {
	public static final String DEFAULT_BOARD_STYLE = "greenwood";

	private static final String WHITE_SQUARE = "white_square";
	private static final String BLACK_SQUARE = "black_square";
	private static final String FRAME = "frame";
	private static final String ENCLOSURE = "enclosure";
	private static final String STRUTS = "struts";
	private static final String PANEL = "panel";
	private static final String HIGHLIGHT_SELECTED = "highlight_selected";
	private static final String HIGHLIGHT_LASTMOVE = "highlight_lastmove";
	private static final String LIGHT_LEVEL = "light_level";
	private static final String WHITE_PIECE = "white_piece";
	private static final String BLACK_PIECE = "black_piece";
	private static final String PIECE_HEIGHT = "piece_height";

	private final boolean isCustom;
	private final int frameWidth, squareSize, height;
	private final String styleName;
	private final AttributeCollection attributes;

	private BoardStyle(String styleName, Configuration c, boolean isCustom) {
		for (String k : new String[] {
				"square_size", "frame_width", "height",
				"black_square", "white_square", "frame", "enclosure"}) {
			PersistenceHandler.requireSection(c, k);
		}
		this.attributes = new AttributeCollection(this);
		registerAttributes();
		this.styleName = styleName;
		this.isCustom = isCustom;
		this.squareSize = c.getInt("square_size");
		this.frameWidth = c.getInt("frame_width");
		this.height = c.getInt("height");

		for (String k : c.getKeys(false)) {
			if (attributes.contains(k)) {
				attributes.set(k, c.getString(k));
			}
		}
	}

	private void registerAttributes() {
		attributes.registerAttribute(LIGHT_LEVEL, 15, "Lighting level (0-15) for the board");
		attributes.registerAttribute(PIECE_HEIGHT, 1, "Height in blocks of a checkers piece");
		attributes.registerAttribute(WHITE_PIECE, MaterialWithData.get("wool:white"), "Material for white checkers pieces");
		attributes.registerAttribute(BLACK_PIECE, MaterialWithData.get("wool:red"), "Material for black checkers pieces");
		attributes.registerAttribute(WHITE_SQUARE, MaterialWithData.get("wool:white"), "Block for white board square");
		attributes.registerAttribute(BLACK_SQUARE, MaterialWithData.get("wool:grey"), "Block for black board square");
		attributes.registerAttribute(FRAME, MaterialWithData.get("wood"), "Block for outer board frame");
		attributes.registerAttribute(ENCLOSURE, MaterialWithData.get("air"), "Block for board enclosure");
		attributes.registerAttribute(STRUTS, MaterialWithData.get("wood"), "Block for board edge struts");
		attributes.registerAttribute(PANEL, MaterialWithData.get("wood"), "Block for control panel");
		attributes.registerAttribute(HIGHLIGHT_SELECTED, MaterialWithData.get("wool:yellow"), "Block for selected square highlight");
		attributes.registerAttribute(HIGHLIGHT_LASTMOVE, MaterialWithData.get("wool:cyan"), "Block for legal move highlight");
	}

	public static BoardStyle loadStyle(String styleName) {
		if (styleName == null) styleName = DEFAULT_BOARD_STYLE;

		try {
			File f = DirectoryStructure.getResourceFileForLoad(DirectoryStructure.getBoardStyleDirectory(), styleName);
			Configuration conf = MiscUtil.loadYamlUTF8(f);
			return new BoardStyle(styleName, conf, DirectoryStructure.isCustom(f));
		} catch (Exception e) {
			throw new CheckersException(e.getMessage());
		}
	}

	public BoardStyle saveStyle(String newStyleName) {
		File f = DirectoryStructure.getResourceFileForSave(DirectoryStructure.getBoardStyleDirectory(), newStyleName);

		// It would be nice to use the configuration API to save this, but I want comments!
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("# Checkers board style definition\n\n");
			out.write("# NOTE: all materials must be quoted, even if they're just integers, or\n");
			out.write("# you will get a java.lang.ClassCastException when the style is loaded.\n\n");
			out.write("# width/length of the board squares, in blocks\n");
			out.write("square_size: " + getSquareSize() + "\n");
			out.write("# width in blocks of the frame surrounding the board\n");
			out.write("frame_width: " + getFrameWidth() + "\n");
			out.write("# height of the board - number of squares of clear air between board and enclosure roof\n");
			out.write("height: " + getHeight() + "\n");
			out.write("# material/data for the white squares\n");
			out.write("white_square: '" + getWhiteSquareMaterial() + "'\n");
			out.write("# material/data for the black squares\n");
			out.write("black_square: '" + getBlackSquareMaterial() + "'\n");
			out.write("# material/data for the frame\n");
			out.write("frame: '" + getFrameMaterial() + "'\n");
			out.write("# material/data for the enclosure\n");
			out.write("enclosure: '" + getEnclosureMaterial() + "'\n");
			out.write("# material/data for the enclosure struts (default: 'enclosure' setting)\n");
			out.write("struts: '" + getStrutsMaterial() + "'\n");
			out.write("# board lighting level (0-15)\n");
			out.write("light_level: " + getLightLevel() + "\n");
			out.write("# material/data for the white pieces\n");
			out.write("white_piece: " + getWhitePieceMaterial() + "\n");
			out.write("# material/data for the black pieces\n");
			out.write("black_piece: " + getBlackPieceMaterial() + "\n");
			out.write("# material/data for the control panel (default: 'frame' setting)\n");
			out.write("panel: '" + getControlPanelMaterial() + "'\n");
			out.write("# highlighting material for selected piece\n");
			out.write("highlight_selected: '" + getSelectedHighlightMaterial() + "'\n");
			out.write("# highlighting material for the last move made\n");
			out.write("highlight_lastmove: '" + getLastMoveHighlightMaterial() + "'\n");
			out.close();

			return loadStyle(newStyleName);
		} catch (IOException e) {
			throw new CheckersException(e.getMessage());
		}
	}

	public String getName() {
		return styleName;
	}

	public int getSquareSize() {
		return squareSize;
	}

	public boolean isCustom() {
		return isCustom;
	}

	public int getHeight() {
		return height;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getLightLevel() {
		return (Integer) attributes.get(LIGHT_LEVEL);
	}

	public MaterialWithData getBlackSquareMaterial() {
		return (MaterialWithData) attributes.get(BLACK_SQUARE);
	}

	public MaterialWithData getWhiteSquareMaterial() {
		return (MaterialWithData) attributes.get(WHITE_SQUARE);
	}

	public MaterialWithData getControlPanelMaterial() {
		return (MaterialWithData) attributes.get(PANEL);
	}

	public MaterialWithData getEnclosureMaterial() {
		return (MaterialWithData) attributes.get(ENCLOSURE);
	}

	public MaterialWithData getFrameMaterial() {
		return (MaterialWithData) attributes.get(FRAME);
	}

	public MaterialWithData getStrutsMaterial() {
		return (MaterialWithData) attributes.get(STRUTS);
	}

	public MaterialWithData getLastMoveHighlightMaterial() {
		return (MaterialWithData) attributes.get(HIGHLIGHT_LASTMOVE);
	}

	public MaterialWithData getSelectedHighlightMaterial() {
		return (MaterialWithData) attributes.get(HIGHLIGHT_SELECTED);
	}

	public MaterialWithData getBlackPieceMaterial() {
		return (MaterialWithData) attributes.get(BLACK_PIECE);
	}

	public MaterialWithData getWhitePieceMaterial() {
		return (MaterialWithData) attributes.get(WHITE_PIECE);
	}

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		if (key.equals(LIGHT_LEVEL)) {
			int level = (Integer) newVal;
			CheckersValidate.isTrue(level >= 0 && level <= 15, "Light level must be in range 0-15");
		} else if (newVal instanceof MaterialWithData) {
			MaterialWithData mat = (MaterialWithData) newVal;
			CheckersValidate.isTrue(mat.getBukkitMaterial().isBlock(), key + ": " + mat + " is not a block material!");
		}
	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		// nothing to do here
	}

	@Override
	public int compareTo(BoardStyle o) {
		return getName().compareTo(o.getName());
	}

	public AttributeCollection getAttributes() {
		return attributes;
	}

}
