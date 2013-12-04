package me.desht.checkers;

import me.desht.checkers.event.CheckersBoardCreatedEvent;
import me.desht.checkers.event.CheckersBoardDeletedEvent;
import me.desht.checkers.event.CheckersGameCreatedEvent;
import me.desht.checkers.event.CheckersGameDeletedEvent;
import me.desht.checkers.game.CheckersGame;
import me.desht.checkers.view.BoardView;
import me.desht.checkers.view.BoardViewManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SMSIntegration implements Listener {

	private static final String MENU_PREFIX = "chc_";

	// menu names
	private static final String TP_GAME = MENU_PREFIX + "tp-game";
	private static final String CREATE_GAME = MENU_PREFIX + "create-game";
	private static final String BOARD_INFO = MENU_PREFIX + "board-info";
	private static final String GAME_INFO = MENU_PREFIX + "game-info";
	private static final String DEL_GAME = MENU_PREFIX + "delete-game";
	private static final String TP_BOARD = MENU_PREFIX + "tp-board";

	private final SMSHandler smsHandler;

	public SMSIntegration(CheckersPlugin plugin, ScrollingMenuSign sms) {
		smsHandler = sms.getHandler();
		Bukkit.getPluginManager().registerEvents(this, plugin);
		createMenus();
	}

	public void setAutosave(boolean autosave) {
		for (SMSMenu menu : smsHandler.listMenus()) {
			if (menu.getName().startsWith(MENU_PREFIX)) {
				menu.setAutosave(autosave);
			}
		}
	}

	@EventHandler
	public void boardCreated(CheckersBoardCreatedEvent event) {
		BoardView bv = event.getBoardView();

		addItem(TP_BOARD, bv.getName(), "/checkers tp -b " + bv.getName());
		addItem(BOARD_INFO, bv.getName(), "/checkers list board " + bv.getName());
		addItem(CREATE_GAME, bv.getName(), "/checkers create game - " + bv.getName());
	}

	@EventHandler
	public void boardDeleted(CheckersBoardDeletedEvent event) {
		BoardView bv = event.getBoardView();

		removeItem(TP_BOARD, bv.getName());
		removeItem(BOARD_INFO, bv.getName());
		removeItem(CREATE_GAME, bv.getName());
	}

	@EventHandler
	public void gameCreated(CheckersGameCreatedEvent event) {
		CheckersGame game = event.getGame();

		addItem(GAME_INFO, game.getName(), "/checkers list game " + game.getName());
		addItem(TP_GAME, game.getName(), "/checkers tp " + game.getName());
		addItem(DEL_GAME, game.getName(), "/checkers delete game " + game.getName());

		BoardView bv = BoardViewManager.getManager().findBoardForGame(game);
		if (bv != null) {
			removeItem(CREATE_GAME, bv.getName());
		}
	}

	@EventHandler
	public void gameDeleted(CheckersGameDeletedEvent event) {
		CheckersGame game = event.getGame();

		removeItem(GAME_INFO, game.getName());
		removeItem(TP_GAME, game.getName());
		removeItem(DEL_GAME, game.getName());

		BoardView bv = BoardViewManager.getManager().findBoardForGame(game);
		if (bv != null) {
			addItem(CREATE_GAME, bv.getName(), "/checkers create game - " + bv.getName());
		}
	}

	private void createMenus() {
		createMenu(BOARD_INFO, Messages.getString("SMSIntegration.boardInfo"));
		createMenu(CREATE_GAME, Messages.getString("SMSIntegration.createGame"));
		createMenu(TP_GAME, Messages.getString("SMSIntegration.gotoGame"));
		createMenu(GAME_INFO, Messages.getString("SMSIntegration.gameInfo"));
		createMenu(DEL_GAME, Messages.getString("SMSIntegration.deleteGame"));
		createMenu(TP_BOARD, Messages.getString("SMSIntegration.gotoBoard"));

		setAutosave(false);
	}

	private void createMenu(String name, String title) {
		SMSMenu menu;
		if (!smsHandler.checkMenu(name)) {
			menu = smsHandler.createMenu(name, title, "*Checkers");
			menu.setAutosort(true);
		} else {
			try {
				// clear all menu items - setActive with a clean slate
				menu = smsHandler.getMenu(name);
				menu.setTitle(MiscUtil.parseColourSpec(title));
				menu.removeAllItems();
			} catch (SMSException e) {
				// shouldn't get here - we already checked that the menu exists
				LogUtils.warning(null, e);
			}
		}
	}

	private void addItem(String menuName, String label, String command) {
		if (smsHandler.checkMenu(menuName)) {
			try {
				SMSMenu menu = smsHandler.getMenu(menuName);
				menu.addItem(label, command, "");
				menu.notifyObservers(SMSMenuAction.REPAINT);
			} catch (SMSException e) {
				// shouldn't get here
				LogUtils.warning(null, e);
			}
		}
	}

	private void removeItem(String menuName, String label) {
		if (smsHandler.checkMenu(menuName)) {
			try {
				SMSMenu menu = smsHandler.getMenu(menuName);
				menu.removeItem(label);
				menu.notifyObservers(SMSMenuAction.REPAINT);
			} catch (SMSException e) {
				LogUtils.warning(null, e);
			}
		}
	}
}
