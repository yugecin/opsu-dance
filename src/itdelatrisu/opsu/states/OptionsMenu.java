/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Options.GameOption;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;

import java.util.*;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.EmptyTransition;
import yugecin.opsudance.ui.ItemList;
import yugecin.opsudance.ui.RWM;

/**
 * "Game Options" state.
 * <p>
 * Players are able to view and change various game settings in this state.
 */
public class OptionsMenu extends BasicGameState {
	/** Option tabs. */
	private enum OptionTab {
		DISPLAY ("Display", new GameOption[] {
			GameOption.SCREEN_RESOLUTION,
			GameOption.FULLSCREEN,
			GameOption.ALLOW_LARGER_RESOLUTIONS,
			GameOption.SKIN,
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
			GameOption.SHOW_UNICODE,
			GameOption.SCREENSHOT_FORMAT,
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.LOAD_HD_IMAGES,
			GameOption.LOAD_VERBOSE
		}),
		MUSIC ("Music", new GameOption[] {
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.SAMPLE_VOLUME_OVERRIDE,
			GameOption.MUSIC_OFFSET,
			GameOption.DISABLE_SOUNDS,
			GameOption.ENABLE_THEME_SONG
		}),
		GAMEPLAY ("Gameplay", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.SNAKING_SLIDERS,
			GameOption.FALLBACK_SLIDERS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
			GameOption.SHOW_HIT_ERROR_BAR
		}),
		INPUT ("Input", new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
			GameOption.CURSOR_SIZE,
			GameOption.NEW_CURSOR,
			GameOption.DISABLE_CURSOR
		}),
		CUSTOM ("Custom", new GameOption[] {
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
			GameOption.DISABLE_UPDATER,
			GameOption.ENABLE_WATCH_SERVICE
		}),
		DANCE ("Dance", new GameOption[] {
			GameOption.DANCE_MOVER,
			GameOption.DANCE_MOVER_DIRECTION,
			GameOption.DANCE_SLIDER_MOVER_TYPE,
			GameOption.DANCE_SPINNER,
			GameOption.DANCE_SPINNER_DELAY,
			GameOption.DANCE_LAZY_SLIDERS,
			GameOption.DANCE_CIRCLE_STREAMS,
			GameOption.DANCE_ONLY_CIRCLE_STACKS,
			GameOption.DANCE_CIRLCE_IN_SLOW_SLIDERS,
			GameOption.DANCE_CIRLCE_IN_LAZY_SLIDERS,
			GameOption.DANCE_MIRROR,
		}),
		DANCEDISP ("Dance display", new GameOption[] {
			GameOption.DANCE_DRAW_APPROACH,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED,
			GameOption.DANCE_RGB_OBJECT_INC,
			GameOption.DANCE_CURSOR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_MIRROR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_ONLY_COLOR_TRAIL,
			GameOption.DANCE_RGB_CURSOR_INC,
			GameOption.DANCE_CURSOR_TRAIL_OVERRIDE,
			GameOption.DANCE_REMOVE_BG,
			GameOption.DANCE_HIDE_OBJECTS,
			GameOption.DANCE_HIDE_UI,
			GameOption.DANCE_ENABLE_SB,
			GameOption.DANCE_HIDE_WATERMARK,
		}),
		PIPPI ("Pippi", new GameOption[] {
			GameOption.PIPPI_ENABLE,
			GameOption.PIPPI_ANGLE_INC_MUL,
			GameOption.PIPPI_ANGLE_INC_MUL_SLIDER,
			GameOption.PIPPI_SLIDER_FOLLOW_EXPAND,
			GameOption.PIPPI_PREVENT_WOBBLY_STREAMS,
		});

		/** Total number of tabs. */
		public static final int SIZE = values().length;

		/** Array of OptionTab objects in reverse order. */
		public static final OptionTab[] VALUES_REVERSED;
		static {
			VALUES_REVERSED = values();
			Collections.reverse(Arrays.asList(VALUES_REVERSED));
		}

		/** Enum values. */
		private static OptionTab[] values = values();

		/** Tab name. */
		private final String name;

		/** Options array. */
		public final GameOption[] options;

		/** Associated tab button. */
		public MenuButton button;

		/**
		 * Constructor.
		 * @param name the tab name
		 * @param options the options to display under the tab
		 */
		OptionTab(String name, GameOption[] options) {
			this.name = name;
			this.options = options;
		}

		/**
		 * Returns the tab name.
		 */
		public String getName() { return name; }

		/**
		 * Returns the next tab.
		 */
		public OptionTab next() { return values[(this.ordinal() + 1) % values.length]; }

		/**
		 * Returns the previous tab.
		 */
		public OptionTab prev() { return values[(this.ordinal() + (SIZE - 1)) % values.length]; }
	}

	/** Current tab. */
	private OptionTab currentTab;

	/** Key entry states. */
	private boolean keyEntryLeft = false, keyEntryRight = false;

	/** Game option coordinate modifiers (for drawing). */
	private int textY, offsetY;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private Graphics g;
	private final int state;

	private final ItemList list;
	private final RWM rwm;

	public OptionsMenu(int state) {
		this.state = state;
		list = new ItemList();
		rwm = new RWM();
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		list.init(container);
		rwm.init(container);

		this.container = container;
		this.game = game;
		this.input = container.getInput();
		this.g = container.getGraphics();

		int width = container.getWidth();
		int height = container.getHeight();

		// option tabs
		Image tabImage = GameImage.MENU_TAB.getImage();
		float tabX = width * 0.032f + (tabImage.getWidth() / 3);
		float tabY = Fonts.XLARGE.getLineHeight() + Fonts.DEFAULT.getLineHeight() +
				height * 0.015f - (tabImage.getHeight() / 2f);
		int tabOffset = Math.min(tabImage.getWidth(), width / OptionTab.SIZE);
		for (OptionTab tab : OptionTab.values())
			tab.button = new MenuButton(tabImage, tabX + (tab.ordinal() * tabOffset), tabY);

		// game option coordinate modifiers
		textY = (int) (tabY + tabImage.getHeight());
		//int backHeight = GameImage.MENU_BACK.getAnimation(1).getHeight();
		//offsetY = (height - textY - (backHeight * 4 / 5)) / maxOptionsScreen;
		offsetY = (int) ((Fonts.MEDIUM.getLineHeight() + Fonts.SMALL.getLineHeight()) * 1.1f);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		int width = container.getWidth();
		int height = container.getHeight();
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();

		// background
		GameImage.OPTIONS_BG.getImage().draw();

		// title
		float marginX = width * 0.015f, marginY = height * 0.01f;
		Fonts.XLARGE.drawString(marginX, marginY, "Options", Color.white);
		Fonts.DEFAULT.drawString(marginX + Fonts.XLARGE.getWidth("Options") * 1.2f, marginY + Fonts.XLARGE.getLineHeight() * 0.9f - Fonts.DEFAULT.getLineHeight(),
				"Change the way opsu! behaves", Color.white);

		// game options
		g.setLineWidth(1f);
		GameOption hoverOption = (keyEntryLeft)  ? GameOption.KEY_LEFT :
		                         (keyEntryRight) ? GameOption.KEY_RIGHT :
		                                           getOptionAt(mouseY);
		for (int i = 0; i < currentTab.options.length; i++) {
			GameOption option = currentTab.options[i];
			drawOption(option, i, hoverOption == option);
		}

		// option tabs
		OptionTab hoverTab = null;
		for (OptionTab tab : OptionTab.values()) {
			if (tab.button.contains(mouseX, mouseY)) {
				hoverTab = tab;
				break;
			}
		}
		for (OptionTab tab : OptionTab.VALUES_REVERSED) {
			if (tab != currentTab)
				UI.drawTab(tab.button.getX(), tab.button.getY(),
						tab.getName(), false, tab == hoverTab && !list.isVisible());
		}
		UI.drawTab(currentTab.button.getX(), currentTab.button.getY(),
				currentTab.getName(), true, false);
		g.setColor(Color.white);
		g.setLineWidth(2f);
		float lineY = OptionTab.DISPLAY.button.getY() + (GameImage.MENU_TAB.getImage().getHeight() / 2f);
		g.drawLine(0, lineY, width, lineY);
		g.resetLineWidth();

		if (list.isVisible()) {
			list.render(container, game, g);
		}
		if (rwm.isVisible()) {
			rwm.render(container, game, g);
		}

		UI.getBackButton().draw();

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.white);
			String prompt = (keyEntryLeft) ?
					"Please press the new left-click key." :
					"Please press the new right-click key.";
			Fonts.LARGE.drawString(
					(width / 2) - (Fonts.LARGE.getWidth(prompt) / 2),
					(height / 2) - Fonts.LARGE.getLineHeight(), prompt
			);
		}

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		rwm.update(delta);
		UI.update(delta);
		MusicController.loopTrackIfEnded(false);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		UI.getBackButton().hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void mouseReleased(int button, int x, int y) {
		if (list.isVisible()) {
			list.mouseReleased(button, x, y);
		}
		if (rwm.isVisible()) {
			rwm.mouseReleased(button, x, y);
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (list.isVisible()) {
			list.mousePressed(button, x, y);
			return;
		}
		if (rwm.isVisible()) {
			return;
		}

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		// back
		if (UI.getBackButton().contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			return;
		}

		// option tabs
		for (OptionTab tab : OptionTab.values()) {
			if (tab.button.contains(x, y)) {
				if (tab != currentTab) {
					currentTab = tab;
					SoundController.playSound(SoundEffect.MENUCLICK);
				}
				return;
			}
		}

		// options (click only)
		final GameOption option = getOptionAt(y);
		if (option != null) {
			Object[] listItems = option.getListItems();
			if (listItems == null) {
				if (option.showRWM()) {
					rwm.show();
				} else {
					option.click(container);
				}
			} else {
				list.setItems(listItems);
				list.setClickListener(new Observer() {
					@Override
					public void update(Observable o, Object arg) {
						option.clickListItem((int) arg);
					}
				});
				list.show();
			}
		}

		// special key entry states
		if (option == GameOption.KEY_LEFT) {
			keyEntryLeft = true;
			keyEntryRight = false;
		} else if (option == GameOption.KEY_RIGHT) {
			keyEntryLeft = false;
			keyEntryRight = true;
		}
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (list.isVisible()) {
			list.mouseDragged(oldx, oldy, newx, newy);
			return;
		}
		if (rwm.isVisible()) {
			return;
		}

		// key entry state
		if (keyEntryLeft || keyEntryRight)
			return;

		// check mouse button (right click scrolls faster)
		int multiplier;
		if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON))
			multiplier = 4;
		else if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			multiplier = 1;
		else
			return;

		// get direction
		int diff = newx - oldx;
		if (diff == 0)
			return;
		diff = ((diff > 0) ? 1 : -1) * multiplier;

		// options (drag only)
		GameOption option = getOptionAt(oldy);
		if (option != null)
			option.drag(container, diff);
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		if (list.isVisible()) {
			list.mouseWheelMoved(newValue);
		}
		if (input.isKeyDown(Input.KEY_LALT) || input.isKeyDown(Input.KEY_RALT))
			UI.changeVolume((newValue < 0) ? -1 : 1);
	}

	@Override
	public void keyPressed(int key, char c) {
		if (list.isVisible()) {
			list.keyPressed(key, c);
			return;
		}
		if (rwm.isVisible()) {
			rwm.keyPressed(key, c);
			return;
		}

		// key entry state
		if (keyEntryLeft || keyEntryRight) {
			if (keyEntryLeft)
				Options.setGameKeyLeft(key);
			else
				Options.setGameKeyRight(key);
			keyEntryLeft = keyEntryRight = false;
			return;
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			SoundController.playSound(SoundEffect.MENUBACK);
			game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
			break;
		case Input.KEY_F5:
			// restart application
			if ((input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) &&
				(input.isKeyDown(Input.KEY_RSHIFT) || input.isKeyDown(Input.KEY_LSHIFT))) {
				container.setForceExit(false);
				container.exit();
			}
			break;
		case Input.KEY_F7:
			Options.setNextFPS(container);
			break;
		case Input.KEY_F10:
			Options.toggleMouseDisabled();
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		case Input.KEY_TAB:
			// change tabs
			if (input.isKeyDown(Input.KEY_LSHIFT) || input.isKeyDown(Input.KEY_RSHIFT))
				currentTab = currentTab.prev();
			else
				currentTab = currentTab.next();
			SoundController.playSound(SoundEffect.MENUCLICK);
			break;
		}
	}

	private String resolutionOptions;

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		currentTab = OptionTab.DANCE;
		resolutionOptions = "" + Options.getResolutionIdx() + Options.isFullscreen() + Options.allowLargeResolutions();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game) throws SlickException {
		if (!("" + Options.getResolutionIdx() + Options.isFullscreen() + Options.allowLargeResolutions()).equals(resolutionOptions)) {
			container.setForceExit(false);
			container.exit();
		}
	}

	/**
	 * Draws a game option.
	 * @param option the option
	 * @param pos the position to draw at
	 * @param focus whether the option is currently focused
	 */
	private void drawOption(GameOption option, int pos, boolean focus) {
		int width = container.getWidth();
		int textHeight = Fonts.MEDIUM.getLineHeight();
		float y = textY + (pos * offsetY);
		Color color = (focus) ? Color.cyan : Color.white;

		Fonts.MEDIUM.drawString(width / 30, y, option.getName(), color);
		Fonts.MEDIUM.drawString(width / 2, y, option.getValueString(), color);
		Fonts.SMALL.drawString(width / 30, y + textHeight, option.getDescription(), color);
		g.setColor(Colors.WHITE_ALPHA);
		g.drawLine(0, y + textHeight, width, y + textHeight);
	}

	/**
	 * Returns the option at the given y coordinate.
	 * @param y the y coordinate
	 * @return the option, or GameOption.NULL if no such option exists
	 */
	private GameOption getOptionAt(int y) {
		if (y < textY)
			return null;

		int index = (y - textY) / offsetY;
		if (index >= currentTab.options.length)
			return null;

		return currentTab.options[index];
	}
}
