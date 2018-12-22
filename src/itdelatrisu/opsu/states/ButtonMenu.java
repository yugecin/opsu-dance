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
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.ScoreData;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapSetNode;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import yugecin.opsudance.core.state.BaseOpsuState;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Generic button menu state.
 * <p>
 * Displays a header and a set of defined options to the player.
 */
public class ButtonMenu extends BaseOpsuState {

	/** Menu states. */
	public enum MenuState {
		/** The exit confirmation screen. */
		EXIT (new Button[] { Button.YES, Button.NO }) {
			@Override
			public String[] getTitle() {
				return new String[] { "Are you sure you want to exit opsu!?" };
			}

			@Override
			public void leave() {
				Button.NO.click();
			}
		},
		/** The initial beatmap management screen (for a non-"favorite" beatmap). */
		BEATMAP (new Button[] { Button.CLEAR_SCORES, Button.FAVORITE_ADD, Button.DELETE, Button.CANCEL }) {
			@Override
			public String[] getTitle() {
				BeatmapSetNode node = buttonState.getNode();
				String beatmapString = (node != null) ? BeatmapSetList.get().getBaseNode(node.index).toString() : "";
				return new String[] { beatmapString, "What do you want to do with this beatmap?" };
			}

			@Override
			public void leave() {
				Button.CANCEL.click();
			}
		},
		/** The initial beatmap management screen (for a "favorite" beatmap). */
		BEATMAP_FAVORITE (new Button[] { Button.CLEAR_SCORES, Button.FAVORITE_REMOVE, Button.DELETE, Button.CANCEL }) {
			@Override
			public String[] getTitle() {
				return BEATMAP.getTitle();
			}

			@Override
			public void leave() {
				BEATMAP.leave();
			}

			@Override
			public void mouseWheelMoved(int newValue) {
				BEATMAP.mouseWheelMoved(newValue);
			}
		},
		/** The beatmap deletion screen for a beatmap set with multiple beatmaps. */
		BEATMAP_DELETE_SELECT (new Button[] { Button.DELETE_GROUP, Button.DELETE_SONG, Button.CANCEL_DELETE }) {
			@Override
			public String[] getTitle() {
				BeatmapSetNode node = buttonState.getNode();
				String beatmapString = (node != null) ? node.toString() : "";
				return new String[] { String.format("Are you sure you wish to delete '%s' from disk?", beatmapString) };
			}

			@Override
			public void leave() {
				Button.CANCEL_DELETE.click();
			}

			@Override
			public void mouseWheelMoved(int newValue) {
				MenuState.BEATMAP.mouseWheelMoved(newValue);
			}
		},
		/** The beatmap deletion screen for a single beatmap. */
		BEATMAP_DELETE_CONFIRM (new Button[] { Button.DELETE_CONFIRM, Button.CANCEL_DELETE }) {
			@Override
			public String[] getTitle() {
				return BEATMAP_DELETE_SELECT.getTitle();
			}

			@Override
			public void leave() {
				Button.CANCEL_DELETE.click();
			}

			@Override
			public void mouseWheelMoved(int newValue) {
				MenuState.BEATMAP.mouseWheelMoved(newValue);
			}
		},
		/** The beatmap reloading confirmation screen. */
		RELOAD (new Button[] { Button.RELOAD_CONFIRM, Button.RELOAD_CANCEL }) {
			@Override
			public String[] getTitle() {
				return new String[] {
						"You have requested a full process of your beatmaps.",
						"This could take a few minutes.",
						"Are you sure you wish to continue?"
				};
			}

			@Override
			public void leave() {
				Button.RELOAD_CANCEL.click();
			}

			@Override
			public void mouseWheelMoved(int newValue) {
				MenuState.BEATMAP.mouseWheelMoved(newValue);
			}
		},
		/** The score management screen. */
		SCORE (new Button[] { Button.DELETE_SCORE, Button.CLOSE }) {
			@Override
			public String[] getTitle() {
				return new String[] { "Score Management" };
			}

			@Override
			public void leave() {
				Button.CLOSE.click();
			}

			@Override
			public void mouseWheelMoved(int newValue) {
				MenuState.BEATMAP.mouseWheelMoved(newValue);
			}
		},
		/** The game mod selection screen. */
		MODS (new Button[] { Button.RESET_MODS, Button.CLOSE }) {
			@Override
			public String[] getTitle() {
				return new String[] {
					"Mods provide different ways to enjoy gameplay. Some have an effect on the score you can achieve during ranked play. Others are just for fun."
				};
			}

			@Override
			protected float getBaseY() {
				return height * 2f / 3;
			}

			@Override
			public void enter() {
				super.enter();
				for (GameMod mod : GameMod.values()) {
					mod.resetHover();
				}
			}

			@Override
			public void leave() {
				Button.CLOSE.click();
			}

			@Override
			public void render(Graphics g) {
				// score multiplier (TODO: fade in color changes)
				float mult = GameMod.getScoreMultiplier();
				String multString = String.format("Score Multiplier: %.2fx", mult);
				Color multColor = (mult == 1f) ? Color.white : (mult > 1f) ? Color.green : Color.red;
				float multY = Fonts.LARGE.getLineHeight() * 2 + height * 0.06f;
				final float multX = width2 - Fonts.LARGE.getWidth(multString) / 2f;
				Fonts.LARGE.drawString(multX, multY, multString, multColor);

				// category text
				for (GameMod.Category category : GameMod.Category.values()) {
					Fonts.LARGE.drawString(category.getX(),
							category.getY() - Fonts.LARGE.getLineHeight() / 2f,
							category.getName(), category.getColor());
				}

				// buttons
				for (GameMod mod : GameMod.values())
					mod.draw();

				super.render(g);
			}

			@Override
			public void preRenderUpdate() {
				super.preRenderUpdate();
				GameMod hoverMod = null;
				for (GameMod mod : GameMod.values()) {
					mod.hoverUpdate(renderDelta, mod.isActive());
					if (hoverMod == null && mod.contains(mouseX, mouseY))
						hoverMod = mod;
				}

				// tooltips
				if (hoverMod != null) {
					UI.updateTooltip(renderDelta, hoverMod.getDescription(), true);
				}
			}

			@Override
			public void keyPressed(int key, char c) {
				super.keyPressed(key, c);
				for (GameMod mod : GameMod.values()) {
					if (key == mod.getKey()) {
						mod.toggle(true);
						break;
					}
				}
			}

			@Override
			public void mousePressed(int cx, int cy) {
				super.mousePressed(cx, cy);
				for (GameMod mod : GameMod.values()) {
					if (mod.contains(cx, cy)) {
						boolean prevState = mod.isActive();
						mod.toggle(true);
						if (mod.isActive() != prevState)
							SoundController.playSound(SoundEffect.MENUCLICK);
						return;
					}
				}
			}

			@Override
			public void mouseWheelMoved(int newValue) {
				MenuState.BEATMAP.mouseWheelMoved(newValue);
			}
		};

		/** The buttons in the state. */
		private final Button[] buttons;

		/** The associated MenuButton objects. */
		private MenuButton[] menuButtons;

		/** The actual title string list, generated upon entering the state. */
		private List<String> actualTitle;

		/** The horizontal center offset, used for the initial button animation. */
		private AnimatedValue centerOffset;

		/** Initial x coordinate offsets left/right of center (for shifting animation), times width. (TODO) */
		private static final float OFFSET_WIDTH_RATIO = 1 / 25f;

		/**
		 * Constructor.
		 * @param buttons the ordered list of buttons in the state
		 */
		MenuState(Button[] buttons) {
			this.buttons = buttons;
		}

		/**
		 * Initializes the menu state.
		 */
		public void revalidate(Image button, Image buttonL, Image buttonR) {
			float baseY = getBaseY();
			float offsetY = button.getHeight() * 1.25f;

			menuButtons = new MenuButton[buttons.length];
			for (int i = 0; i < buttons.length; i++) {
				MenuButton b = new MenuButton(button, buttonL, buttonR, width2, baseY + (i * offsetY));
				b.setText(String.format("%d. %s", i + 1, buttons[i].getText()), Fonts.XLARGE, Color.white);
				b.setHoverFade();
				menuButtons[i] = b;
			}
		}

		/**
		 * Returns the base Y coordinate for the buttons.
		 */
		protected float getBaseY() {
			float baseY = height * 0.2f;
			baseY += ((getTitle().length - 1) * Fonts.LARGE.getLineHeight());
			return baseY;
		}

		/**
		 * Draws the title and buttons to the graphics context.
		 * @param g the graphics context
		 */
		public void render(Graphics g) {
			// draw title
			if (actualTitle != null) {
				float marginX = width * 0.015f, marginY = height * 0.01f;
				int lineHeight = Fonts.LARGE.getLineHeight();
				for (int i = 0, size = actualTitle.size(); i < size; i++)
					Fonts.LARGE.drawString(marginX, marginY + (i * lineHeight), actualTitle.get(i), Color.white);
			}

			// draw buttons
			for (int i = 0; i < buttons.length; i++)
				menuButtons[i].draw(buttons[i].getColor());
		}

		/**
		 * Updates the menu state.
		 */
		public void preRenderUpdate() {
			boolean centerOffsetUpdated = centerOffset.update(renderDelta);
			float centerOffsetX = centerOffset.getValue();
			final float[] offsets = { centerOffsetX, - centerOffsetX };
			for (int i = 0; i < buttons.length; i++) {
				menuButtons[i].hoverUpdate(renderDelta, mouseX, mouseY);

				// move button to center
				if (centerOffsetUpdated) {
					menuButtons[i].setX(width2 + offsets[i & 1]);
				}
			}
		}

		/**
		 * Processes a mouse click action.
		 */
		public void mousePressed(int x, int y) {
			for (int i = 0; i < buttons.length; i++) {
				if (menuButtons[i].contains(x, y)) {
					buttons[i].click();
					break;
				}
			}
		}

		/**
		 * Processes a key press action.
		 * @param key the key code that was pressed (see {@link org.newdawn.slick.Input})
		 * @param c the character of the key that was pressed
		 */
		public void keyPressed(int key, char c) {
			int index = Character.getNumericValue(c) - 1;
			if (index >= 0 && index < buttons.length)
				buttons[index].click();
		}

		/**
		 * Retrieves the title strings for the menu state (via override).
		 */
		public String[] getTitle() { return new String[0]; }

		/**
		 * Processes a mouse wheel movement.
		 * @param newValue the amount that the mouse wheel moved
		 */
		public void mouseWheelMoved(int newValue)
		{
			volumeControl.changeVolume(newValue);
		}

		/**
		 * Processes a state enter request.
		 */
		public void enter() {
			float centerOffsetX = width * OFFSET_WIDTH_RATIO;
			centerOffset = new AnimatedValue(700, centerOffsetX, 0, AnimationEquation.OUT_BOUNCE);
			for (int i = 0; i < buttons.length; i++) {
				menuButtons[i].setX(width2 + ((i % 2 == 0) ? centerOffsetX : centerOffsetX * -1));
				menuButtons[i].resetHover();
			}

			// create title string list
			actualTitle = new ArrayList<>();
			String[] title = getTitle();
			int maxLineWidth = (int) (width * 0.96f);
			for (String aTitle : title) {
				// wrap text if too long
				if (Fonts.LARGE.getWidth(aTitle) > maxLineWidth) {
					List<String> list = Fonts.wrap(Fonts.LARGE, aTitle, maxLineWidth, false);
					actualTitle.addAll(list);
				} else {
					actualTitle.add(aTitle);
				}
			}
		}

		/**
		 * Processes a state exit request (via override).
		 */
		public void leave() {}
	}

	/** Button types. */
	private enum Button {
		YES ("Yes", Color.green) {
			@Override
			public void click() {
				displayContainer.exitRequested = true;
			}
		},
		NO ("No", Color.red) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUBACK);
				displayContainer.switchState(mainmenuState);
			}
		},
		CLEAR_SCORES ("Clear local scores", Color.magenta) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = buttonState.getNode();
				songMenuState.doStateActionOnLoad(MenuState.BEATMAP, node);
				displayContainer.switchState(songMenuState);
			}
		},
		FAVORITE_ADD ("Add to Favorites", Color.blue) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = buttonState.getNode();
				node.getBeatmapSet().setFavorite(true);
				displayContainer.switchState(songMenuState);
			}
		},
		FAVORITE_REMOVE ("Remove from Favorites", Color.blue) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = buttonState.getNode();
				node.getBeatmapSet().setFavorite(false);
				songMenuState.doStateActionOnLoad(MenuState.BEATMAP_FAVORITE);
				displayContainer.switchState(songMenuState);
			}
		},
		DELETE ("Delete...", Color.red) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = buttonState.getNode();
				MenuState ms = (node.beatmapIndex == -1 || node.getBeatmapSet().size() == 1) ?
						MenuState.BEATMAP_DELETE_CONFIRM : MenuState.BEATMAP_DELETE_SELECT;
				buttonState.setMenuState(ms, node);
				displayContainer.switchState(buttonState);
			}
		},
		CANCEL ("Cancel", Color.gray) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUBACK);
				displayContainer.switchState(songMenuState);
			}
		},
		DELETE_CONFIRM ("Yes, delete this beatmap!", Color.red) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = buttonState.getNode();
				songMenuState.doStateActionOnLoad(MenuState.BEATMAP_DELETE_CONFIRM, node);
				displayContainer.switchState(songMenuState);
			}
		},
		DELETE_GROUP ("Yes, delete all difficulties!", Color.red) {
			@Override
			public void click() {
				DELETE_CONFIRM.click();
			}
		},
		DELETE_SONG ("Yes, but only this difficulty", Color.red) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				BeatmapSetNode node = buttonState.getNode();
				songMenuState.doStateActionOnLoad(MenuState.BEATMAP_DELETE_SELECT, node);
				displayContainer.switchState(songMenuState);
			}
		},
		CANCEL_DELETE ("Nooooo! I didn't mean to!", Color.gray) {
			@Override
			public void click() {
				CANCEL.click();
			}
		},
		RELOAD_CONFIRM ("Let's do it!", Color.green) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				songMenuState.doStateActionOnLoad(MenuState.RELOAD);
				displayContainer.switchState(songMenuState);
			}
		},
		RELOAD_CANCEL ("Cancel", Color.red) {
			@Override
			public void click() {
				CANCEL.click();
			}
		},
		DELETE_SCORE ("Delete score", Color.green) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUHIT);
				ScoreData scoreData = buttonState.getScoreData();
				songMenuState.doStateActionOnLoad(MenuState.SCORE, scoreData);
				displayContainer.switchState(songMenuState);
			}
		},
		CLOSE ("Close", Color.gray) {
			@Override
			public void click() {
				CANCEL.click();
			}
		},
		RESET_MODS ("Reset All Mods", Color.red) {
			@Override
			public void click() {
				SoundController.playSound(SoundEffect.MENUCLICK);
				for (GameMod mod : GameMod.values()) {
					if (mod.isActive())
						mod.toggle(false);
				}
			}
		};

		/** The text to show on the button. */
		private final String text;

		/** The button color. */
		private final Color color;

		/**
		 * Constructor.
		 * @param text the text to show on the button
		 * @param color the button color
		 */
		Button(String text, Color color) {
			this.text = text;
			this.color = color;
		}

		/**
		 * Returns the button text.
		 */
		public String getText() { return text; }

		/**
		 * Returns the button color.
		 */
		public Color getColor() { return color; }

		/**
		 * Processes a mouse click action (via override).
		 */
		public void click() {}
	}

	/** The current menu state. */
	private MenuState menuState;

	/** The song node to process in the state. */
	private BeatmapSetNode node;

	/** The score data to process in the state. */
	private ScoreData scoreData;

	@Override
	public void revalidate() {
		super.revalidate();

		// initialize buttons
		Image button = GameImage.MENU_BUTTON_MID.getImage();
		button = button.getScaledCopy(width2, button.getHeight());
		Image buttonL = GameImage.MENU_BUTTON_LEFT.getImage();
		Image buttonR = GameImage.MENU_BUTTON_RIGHT.getImage();
		for (MenuState ms : MenuState.values()) {
			ms.revalidate(button, buttonL, buttonR);
		}
	}

	@Override
	public void render(Graphics g) {
		super.render(g);

		g.setBackground(Color.black);
		if (menuState == null) {
			return;
		}
		menuState.render(g);
	}

	@Override
	public void preRenderUpdate() {
		super.preRenderUpdate();

		UI.update(renderDelta);
		MusicController.loopTrackIfEnded(false);
		menuState.preRenderUpdate();
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		if (button == Input.MMB) {
			return false;
		}

		menuState.mousePressed(x, y);
		return true;
	}

	@Override
	public boolean mouseWheelMoved(int newValue) {
		menuState.mouseWheelMoved(newValue);
		return true;
	}

	@Override
	public boolean keyPressed(int key, char c) {
		if (super.keyPressed(key, c)) {
			return true;
		}

		if (key == Keyboard.KEY_ESCAPE) {
			menuState.leave();
			return true;
		}

		menuState.keyPressed(key, c);
		return true;
	}

	@Override
	public void enter() {
		super.enter();

		UI.enter();
		menuState.enter();
	}

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 */
	public void setMenuState(MenuState menuState) { setMenuState(menuState, null, null); }

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 * @param node the song node to process in the state
	 */
	public void setMenuState(MenuState menuState, BeatmapSetNode node) { setMenuState(menuState, node, null); }

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 * @param scoreData the score scoreData
	 */
	public void setMenuState(MenuState menuState, ScoreData scoreData) { setMenuState(menuState, null, scoreData); }

	/**
	 * Changes the menu state.
	 * @param menuState the new menu state
	 * @param node the song node to process in the state
	 * @param scoreData the score scoreData
	 */
	private void setMenuState(MenuState menuState, BeatmapSetNode node, ScoreData scoreData) {
		this.menuState = menuState;
		this.node = node;
		this.scoreData = scoreData;
	}

	/**
	 * Returns the song node being processed, or null if none.
	 */
	private BeatmapSetNode getNode() { return node; }

	/**
	 * Returns the score data being processed, or null if none.
	 */
	private ScoreData getScoreData() { return scoreData; }
}
