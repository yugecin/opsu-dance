/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.ui;

import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Options.GameOption;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import yugecin.opsudance.ObjectColorOverrides;
import yugecin.opsudance.ui.OptionsOverlay.OptionTab;

import java.util.*;

@SuppressWarnings("unchecked")
public class SBOverlay implements OptionsOverlay.Parent {

	private static final OptionTab[] options = new OptionsOverlay.OptionTab[]{
		new OptionTab("Gameplay", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.DANCE_REMOVE_BG,
			GameOption.SNAKING_SLIDERS,
			GameOption.SHRINKING_SLIDERS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_HIT_ANIMATIONS,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
		}),
		new OptionTab("Input", new GameOption[] {
			GameOption.CURSOR_SIZE,
			GameOption.NEW_CURSOR,
			GameOption.DISABLE_CURSOR
		}),
		new OptionTab("Dance", new GameOption[] {
			GameOption.DANCE_MOVER,
			GameOption.DANCE_QUAD_BEZ_AGGRESSIVENESS,
			GameOption.DANCE_QUAD_BEZ_SLIDER_AGGRESSIVENESS_FACTOR,
			GameOption.DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS,
			GameOption.DANCE_QUAD_BEZ_CUBIC_AGGRESSIVENESS_FACTOR,
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
		new OptionTab("Dance display", new GameOption[] {
			GameOption.DANCE_DRAW_APPROACH,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED,
			GameOption.DANCE_RGB_OBJECT_INC,
			GameOption.DANCE_CURSOR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_MIRROR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_ONLY_COLOR_TRAIL,
			GameOption.DANCE_RGB_CURSOR_INC,
			GameOption.DANCE_CURSOR_TRAIL_OVERRIDE,
			GameOption.DANCE_HIDE_OBJECTS,
			GameOption.DANCE_HIDE_UI,
			GameOption.DANCE_HIDE_WATERMARK,
		}),
		new OptionTab ("Pippi", new GameOption[] {
			GameOption.PIPPI_ENABLE,
			GameOption.PIPPI_RADIUS_PERCENT,
			GameOption.PIPPI_ANGLE_INC_MUL,
			GameOption.PIPPI_ANGLE_INC_MUL_SLIDER,
			GameOption.PIPPI_SLIDER_FOLLOW_EXPAND,
			GameOption.PIPPI_PREVENT_WOBBLY_STREAMS,
		})
	};

	private final static List<GameOption> optionList = new ArrayList<>();

	private boolean hide;
	private boolean menu;

	private int width;
	private int height;

	private int speed;
	private GameObject[] gameObjects;
	private HashMap[] optionsMap;
	private HashMap<Options.GameOption, String> initialOptions;

	private int index;

	private final Game game;
	private final OptionsOverlay overlay;

	static {
		for (OptionTab tab : options) {
			optionList.addAll(Arrays.asList(tab.options));
		}
	}

	public SBOverlay(Game game, GameContainer container) {
		this.game = game;
		initialOptions = new HashMap<>();
		overlay = new OptionsOverlay(this, options, 2, container);
		this.width = container.getWidth();
		this.height = container.getHeight();
		speed = 10;
		gameObjects = new GameObject[0];
	}

	public void render(GameContainer container, Graphics g) {
		if (!Options.isEnableSB() || hide) {
			return;
		}
		int lh = Fonts.SMALL.getLineHeight();
		Fonts.SMALL.drawString(10, height - 50 + lh, "save position: ctrl+s, load position: ctrl+l", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50, "speed: C " + (speed / 10f) + " V", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh, "Menu: N", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh * 2, "HIDE: H", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh * 3, "obj: J " + index + " K", Color.cyan);
		g.setColor(Color.red);
		if (index < optionsMap.length && optionsMap[index] != null) {
			int i = 0;
			for (Object o : optionsMap[index].entrySet()) {
				Map.Entry<Options.GameOption, String> option = (Map.Entry<Options.GameOption, String>) o;
				Fonts.SMALL.drawString(10, 50 + i * lh, option.getKey().getName(), Color.cyan);
				Fonts.SMALL.drawString(width / 5, 50 + i * lh, option.getKey().getValueString(), Color.cyan);
				g.fillRect(0, 50 + i * lh + lh / 4, 10, 10);
				i++;
			}
		}
		if (gameObjects.length > 0) {
			int start = gameObjects[0].getTime();
			int end = gameObjects[gameObjects.length - 1].getEndTime();
			float curtime = (float) (MusicController.getPosition() - start) / (end - start);
			g.fillRect(curtime * width, height - 10f, 10f, 10f);
		}
		if (menu) {
			overlay.render(g, container.getInput().getMouseX(), container.getInput().getMouseY());
		}
	}

	public void update(int delta, int mouseX, int mouseY) {
		if (Options.isEnableSB() && menu) {
			overlay.update(delta, mouseX, mouseY);
		}
	}

	public boolean keyPressed(int key, char c) {
		if (!Options.isEnableSB()) {
			return false;
		}
		if (menu && overlay.keyPressed(key, c)) {
			return true;
		}
		if (key == Input.KEY_C) {
			if (speed > 0) {
				speed -= 1;
			}
			if (speed == 0) {
				MusicController.pause();
			} else {
				MusicController.setPitch(speed / 10f);
			}
		} else if (key == Input.KEY_V && speed < 21) {
			if (speed == 0) {
				MusicController.resume();
			}
			speed += 1;
			MusicController.setPitch(speed / 10f);
		} else if (key == Input.KEY_H) {
			hide = !hide;
		} else if (key == Input.KEY_N) {
			menu = !menu;
			if (menu && speed != 0) {
				MusicController.pause();
			} else if (!menu && speed != 0) {
				MusicController.resume();
			}
		} else if (key == Input.KEY_J && index > 0) {
			index--;
			setMusicPosition();
			goBackOneSBIndex();
		} else if (key == Input.KEY_K && index < gameObjects.length - 1) {
			index++;
			setMusicPosition();
			updateIndex(index);
		}
		return false;
	}

	private void goBackOneSBIndex() {
		if (index + 1 < optionsMap.length && optionsMap[index + 1] != null) {
			// new options on previous index, so to revert then we have to reload them all to this point..
			reloadSBsettingsToIndex(index);
		}
	}

	private void reloadSBsettingsToIndex(final int index) {
		for (int i = 0; i <= index; i++) {
			updateIndex(i);
		}
	}

	private void setMusicPosition() {
		game.loadCheckpoint(gameObjects[index].getTime());
		if (speed != 0) {
			MusicController.setPitch(speed / 10f);
			MusicController.resume();
		} else {
			MusicController.pause();
		}
	}

	public void setGameObjects(GameObject[] gameObjects) {
		if (this.gameObjects.length != gameObjects.length) {
			optionsMap = new HashMap[gameObjects.length];
		}
		if (optionsMap.length > 0) {
			// copy all current settings in first obj map
			optionsMap[0] = new HashMap<>();
			for (Options.GameOption o : optionList) {
				optionsMap[0].put(o, o.write());
			}
		}
		this.gameObjects = gameObjects;
	}

	public boolean mousePressed(int button, int x, int y) {
		if (!menu) {
			return false;
		}
		overlay.mousePressed(button, x, y);
		return true;
	}

	public boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (!menu) {
			return false;
		}
		overlay.mouseDragged(oldx, oldy, newx, newy);
		return true;
	}

	public void updateIndex(int index) {
		if (index < this.index) {
			this.index = index;
			reloadSBsettingsToIndex(index);
			return;
		}
		if (index >= optionsMap.length) {
			return;
		}
		for (; this.index < index; this.index++) {
			HashMap options = optionsMap[this.index];
			if (options == null) {
				continue;
			}
			for (Object o : options.entrySet()) {
				Map.Entry<Options.GameOption, String> next = (Map.Entry<Options.GameOption, String>) o;
				next.getKey().read(next.getValue());
				readOption(next.getKey());
			}
		}
	}

	public boolean mouseReleased(int button, int x, int y) {
		if (!menu) {
			return false;
		}
		overlay.mouseReleased(button, x, y);
		return true;
	}

	public boolean mouseWheelMoved(int delta) {
		if (!menu) {
			return false;
		}
		overlay.mouseWheelMoved(delta);
		return true;
	}

	public void enter() {
		// enter, save current settings
		for (Options.GameOption o : optionList) {
			initialOptions.put(o, o.write());
		}
		speed = 10;
	}

	public void leave() {
		// leave, revert the settings saved before entering
		for (Options.GameOption o : optionList) {
			if (initialOptions.containsKey(o)) {
				o.read(initialOptions.get(o));
				readOption(o);
			}
		}
	}

	// needed for object color overrides...
	private void readOption(Options.GameOption o) {
		if (o == Options.GameOption.DANCE_OBJECT_COLOR_OVERRIDE
			|| o == Options.GameOption.DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED
			|| o == Options.GameOption.DANCE_RGB_OBJECT_INC) {
			if (index < gameObjects.length) {
				ObjectColorOverrides.hue = gameObjects[index].getHue();
			}
			for (int i = index; i < gameObjects.length; i++) {
				gameObjects[i].updateColor();
			}
		}
	}

	@Override
	public void onLeave() {
		menu = false;
		if (speed != 0) {
			MusicController.resume();
		}
	}

	@Override
	public void onSaveOption(GameOption option) {
		if (optionsMap[index] == null) {
			optionsMap[index] = new HashMap<>();
		}
		optionsMap[index].put(option, option.write());
		readOption(option);
	}

}
