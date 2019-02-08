// Copyright 2016-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui;

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.states.game.Game;
import yugecin.opsudance.options.Option;
import yugecin.opsudance.options.OptionGroups;
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import yugecin.opsudance.ObjectColorOverrides;
import yugecin.opsudance.core.input.*;
import yugecin.opsudance.core.state.OverlayOpsuState;
import yugecin.opsudance.options.OptionTab;
import yugecin.opsudance.sbv2.MoveStoryboard;

import java.util.*;

import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class StoryboardOverlay extends OverlayOpsuState implements OptionsOverlay.Listener {

	private final static List<Option> optionList = new ArrayList<>();

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
			if (tab.options != null) {
				optionList.addAll(Arrays.asList(tab.options));
			}
		}
	}

	public StoryboardOverlay(MoveStoryboard msb, OptionsOverlay optionsOverlay, Game game) {
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
		Fonts.SMALL.drawString(10, height - 50 + lh, "save position: ctrl+s, load position: ctrl+l", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50, "speed: C " + (speed / 10f) + " V", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh, "Menu: N", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh * 2, "HIDE: H", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh * 3, "obj: J " + index + " K", Color.cyan);
		g.setColor(Color.red);
		if (index < optionsMap.length && optionsMap[index] != null) {
			int i = 0;
			for (Object o : optionsMap[index].entrySet()) {
				Map.Entry<Option, String> option = (Map.Entry<Option, String>) o;
				Fonts.SMALL.drawString(10, 50 + i * lh, option.getKey().name, Color.cyan);
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
	}

	@Override
	public void onPreRenderUpdate() {
	}

	@Override
	public void onKeyPressed(KeyEvent e)
	{
		e.consume();
		switch (e.keyCode) {
		case KEY_C:
			if (speed > 0) {
				speed -= 1;
			}
			if (speed == 0) {
				MusicController.pause();
			} else {
				MusicController.setPitch(speed / 10f);
			}
			break;
		case KEY_V:
			if (speed < 21) {
				if (speed == 0) {
					MusicController.resume();
				}
				speed += 1;
				MusicController.setPitch(speed / 10f);
			}
			break;
		case KEY_H:
			hide = !hide;
			break;
		case KEY_N:
			optionsOverlay.show();
			if (speed != 0) {
				MusicController.pause();
			}
			break;
		case KEY_J:
			if (index > 0) {
				index--;
				goBackOneSBIndex();
				setMusicPosition();
			}
			break;
		case KEY_K:
			if (index < gameObjects.length - 1) {
				index++;
				updateIndex(index);
				setMusicPosition();
			}
			break;
		}
	}

	@Override
	protected void onKeyReleased(KeyEvent e)
	{
		e.consume();
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
	public void onMousePressed(MouseEvent e)
	{
		e.consume();
	}

	@Override
	public void onMouseDragged(MouseDragEvent e)
	{
		e.consume();
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
	public void onMouseReleased(MouseEvent e)
	{
		e.consume();
		if (e.x > 10 || index >= optionsMap.length || optionsMap[index] == null) {
			return;
		}
		int lh = Fonts.SMALL.getLineHeight();
		int ypos = 50 + lh / 4;
		for (Object o : optionsMap[index].entrySet()) {
			if (e.y >= ypos && e.y <= ypos + 10) {
				optionsMap[index].remove(((Map.Entry<Option, String>) o).getKey());
				if (optionsMap[index].size() == 0) {
					optionsMap[index] = null;
				}
				reloadSBsettingsToIndex(index);
				return;
			}
			ypos += lh;
		}
	}

	@Override
	public void onMouseWheelMoved(MouseWheelEvent e)
	{
		e.consume();
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
