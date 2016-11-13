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
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.state.StateBasedGame;
import yugecin.opsudance.ObjectColorOverrides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class SBOverlay {

	public static boolean isActive = false;

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
	private final OptionsOverlay options;

	public SBOverlay(Game game) {
		this.game = game;
		options = new OptionsOverlay(this);
		initialOptions = new HashMap<>();
	}

	public void init(GameContainer container, Input input, int width, int height) {
		this.width = width;
		this.height = height;
		speed = 10;
		gameObjects = new GameObject[0];
		options.init(container, input, width, height);
	}

	public void render(GameContainer container, StateBasedGame game, Graphics g) {
		if (!isActive || hide) {
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
				Fonts.SMALL.drawString(250, 50 + i * lh, option.getKey().getValueString(), Color.cyan);
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
			options.render(container, game, g);
		}
	}

	public void update(int mouseX, int mouseY) {
		if (!isActive) {
			return;
		}
		if (menu) {
			options.update(mouseX, mouseY);
		}
	}

	public boolean keyPressed(int key, char c) {
		if (!isActive) {
			return false;
		}
		if (options.keyPressed(key, c)) {
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
		} else if (key == Input.KEY_ESCAPE && menu) {
			menu = false;
			if (speed != 0) {
				MusicController.resume();
			}
			return true;
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
		game.setObjectIndex(index);
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
			// copy all current settings in first obj map
			optionsMap[0] = new HashMap<>();
			for (Options.GameOption o : options.getSavedOptionList()) {
				optionsMap[0].put(o, o.write());
			}
		}
		this.gameObjects = gameObjects;
	}

	public void saveOption(Options.GameOption option) {
		if (optionsMap[index] == null) {
			optionsMap[index] = new HashMap<>();
		}
		optionsMap[index].put(option, option.write());
		readOption(option);
	}

	public boolean mousePressed(int button, int x, int y) {
		return menu && options.mousePressed(button, x, y);
	}

	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (menu) options.mouseDragged(oldx, oldy, newx, newy);
	}

	public void updateIndex(int index) {
		this.index = index;
		if (index >= optionsMap.length) {
			return;
		}
		HashMap options = optionsMap[index];
		if (options != null) {
			for (Object o : options.entrySet()) {
				Map.Entry<Options.GameOption, String> next = (Map.Entry<Options.GameOption, String>) o;
				next.getKey().read(next.getValue());
				readOption(next.getKey());
			}
		}
	}

	public boolean mouseReleased(int button, int x, int y) {
		if (menu) {
			return options.mouseReleased(button, x, y);
		}
		if (x > 10 || index >= optionsMap.length || optionsMap[index] == null) {
			return false;
		}
		int lh = Fonts.SMALL.getLineHeight();
		int ypos = 50 + lh / 4;
		for (Object o : optionsMap[index].entrySet()) {
			if (y >= ypos && y <= ypos + 10) {
				optionsMap[index].remove(((Map.Entry<Options.GameOption, String>) o).getKey());
				if (optionsMap[index].size() == 0) {
					optionsMap[index] = null;
				}
				reloadSBsettingsToIndex(index);
				return true;
			}
			ypos += lh;
		}
		return false;
	}

	public boolean mouseWheelMoved(int newValue) {
		return menu && options.mouseWheenMoved(newValue);
	}

	public void enter() {
		// enter, save current settings
		for (Options.GameOption o : options.getSavedOptionList()) {
			initialOptions.put(o, o.write());
		}
	}

	public void leave() {
		// leave, revert the settings saved before entering
		for (Options.GameOption o : options.getSavedOptionList()) {
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

}
