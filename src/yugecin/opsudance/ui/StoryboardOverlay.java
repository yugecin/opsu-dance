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

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.states.Game;
import yugecin.opsudance.options.Option;
import yugecin.opsudance.options.OptionGroups;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import yugecin.opsudance.ObjectColorOverrides;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.state.OverlayOpsuState;
import yugecin.opsudance.options.OptionTab;
import yugecin.opsudance.sbv2.MoveStoryboard;

import java.util.*;

import static yugecin.opsudance.options.Options.*;

@SuppressWarnings("unchecked")
public class StoryboardOverlay extends OverlayOpsuState implements OptionsOverlay.Listener {

	private final static List<Option> optionList = new ArrayList<>();

	private final DisplayContainer displayContainer;

	private boolean hide;

	private int speed;
	private GameObject[] gameObjects;
	private HashMap[] optionsMap;
	private HashMap<Option, String> initialOptions;

	private int index;

	private final Game game;
	private final MoveStoryboard msb;
	private final OptionsOverlay optionsOverlay;

	static {
		for (OptionTab tab : OptionGroups.storyboardOptions) {
			optionList.addAll(Arrays.asList(tab.options));
		}
	}

	public StoryboardOverlay(DisplayContainer displayContainer, MoveStoryboard msb, OptionsOverlay optionsOverlay, Game game) {
		this.displayContainer = displayContainer;
		this.msb = msb;
		this.optionsOverlay = optionsOverlay;
		this.game = game;
		initialOptions = new HashMap<>();
		speed = 10;
		gameObjects = new GameObject[0];
	}

	@Override
	public void onRender(Graphics g) {
		if (!OPTION_DANCE_ENABLE_SB.state || hide) {
			return;
		}
		int lh = Fonts.SMALL.getLineHeight();
		Fonts.SMALL.drawString(10, displayContainer.height - 50 + lh, "save position: ctrl+s, load position: ctrl+l", Color.cyan);
		Fonts.SMALL.drawString(10, displayContainer.height - 50, "speed: C " + (speed / 10f) + " V", Color.cyan);
		Fonts.SMALL.drawString(10, displayContainer.height - 50 - lh, "Menu: N", Color.cyan);
		Fonts.SMALL.drawString(10, displayContainer.height - 50 - lh * 2, "HIDE: H", Color.cyan);
		Fonts.SMALL.drawString(10, displayContainer.height - 50 - lh * 3, "obj: J " + index + " K", Color.cyan);
		g.setColor(Color.red);
		if (index < optionsMap.length && optionsMap[index] != null) {
			int i = 0;
			for (Object o : optionsMap[index].entrySet()) {
				Map.Entry<Option, String> option = (Map.Entry<Option, String>) o;
				Fonts.SMALL.drawString(10, 50 + i * lh, option.getKey().name, Color.cyan);
				Fonts.SMALL.drawString(displayContainer.width / 5, 50 + i * lh, option.getKey().getValueString(), Color.cyan);
				g.fillRect(0, 50 + i * lh + lh / 4, 10, 10);
				i++;
			}
		}
		if (gameObjects.length > 0) {
			int start = gameObjects[0].getTime();
			int end = gameObjects[gameObjects.length - 1].getEndTime();
			float curtime = (float) (MusicController.getPosition() - start) / (end - start);
			g.fillRect(curtime * displayContainer.width, displayContainer.height - 10f, 10f, 10f);
		}
	}

	@Override
	public void onPreRenderUpdate() {
	}

	@Override
	public boolean onKeyPressed(int key, char c) {
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
			optionsOverlay.show();
			if (speed != 0) {
				MusicController.pause();
			}
		} else if (key == Input.KEY_J && index > 0) {
			index--;
			goBackOneSBIndex();
			setMusicPosition();
		} else if (key == Input.KEY_K && index < gameObjects.length - 1) {
			index++;
			updateIndex(index);
			setMusicPosition();
		}
		return false;
	}

	@Override
	protected boolean onKeyReleased(int key, char c) {
		return false;
	}

	private void goBackOneSBIndex() {
		if (index + 1 < optionsMap.length) {
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
			msb.setGameObjects(gameObjects);
		}
		if (optionsMap.length > 0) {
			// copy all current settings in first obj map
			optionsMap[0] = new HashMap<>();
			for (Option o : optionList) {
				optionsMap[0].put(o, o.write());
			}
		}
		this.gameObjects = gameObjects;
	}

	@Override
	public boolean onMousePressed(int button, int x, int y) {
		return true;
	}

	@Override
	public boolean onMouseDragged(int oldx, int oldy, int newx, int newy) {
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
		msb.setIndex(index);
		for (; this.index <= index; this.index++) {
			HashMap options = optionsMap[this.index];
			if (options == null) {
				continue;
			}
			for (Object o : options.entrySet()) {
				Map.Entry<Option, String> next = (Map.Entry<Option, String>) o;
				next.getKey().read(next.getValue());
				readOption(next.getKey());
			}
		}
		this.index--;
	}

	@Override
	public boolean onMouseReleased(int button, int x, int y) {
		if (x > 10 || index >= optionsMap.length || optionsMap[index] == null) {
			return false;
		}
		int lh = Fonts.SMALL.getLineHeight();
		int ypos = 50 + lh / 4;
		for (Object o : optionsMap[index].entrySet()) {
			if (y >= ypos && y <= ypos + 10) {
				optionsMap[index].remove(((Map.Entry<Option, String>) o).getKey());
				if (optionsMap[index].size() == 0) {
					optionsMap[index] = null;
				}
				reloadSBsettingsToIndex(index);
				return true;
			}
			ypos += lh;
		}
		return true;
	}

	@Override
	public boolean onMouseWheelMoved(int delta) {
		return false;
	}

	public void onEnter() {
		// enter, save current settings
		for (Option o : optionList) {
			initialOptions.put(o, o.write());
		}
		speed = 10;
	}

	public void onLeave() {
		// leave, revert the settings saved before entering
		for (Option o : optionList) {
			if (initialOptions.containsKey(o)) {
				o.read(initialOptions.get(o));
				readOption(o);
			}
		}
	}

	// needed for object color overrides...
	private void readOption(Option o) {
		if (o == OPTION_DANCE_OBJECT_COLOR_OVERRIDE
			|| o == OPTION_DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED
			|| o == OPTION_DANCE_RGB_OBJECT_INC) {
			if (index < gameObjects.length) {
				ObjectColorOverrides.hue = gameObjects[index].getHue();
			}
			for (int i = index; i < gameObjects.length; i++) {
				gameObjects[i].updateColor();
			}
		}
	}

	@Override
	public void onLeaveOptionsMenu() {
		if (speed != 0) {
			MusicController.resume();
		}
	}

	@Override
	public void onSaveOption(Option option) {
		if (optionsMap[index] == null) {
			optionsMap[index] = new HashMap<>();
		}
		optionsMap[index].put(option, option.write());
		readOption(option);
	}

}
