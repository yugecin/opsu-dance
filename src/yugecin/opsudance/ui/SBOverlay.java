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
import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.state.StateBasedGame;

import java.util.HashMap;

public class SBOverlay {

	public static boolean isActive = true;

	private boolean hide;
	private boolean menu;

	private int width;
	private int height;

	private int speed;
	private GameObject[] gameObjects;
	private HashMap[] optionsMap;

	private int index;

	private final Game game;
	private final OptionsOverlay options;

	public SBOverlay(Game game) {
		this.game = game;
		options = new OptionsOverlay();
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
		Fonts.SMALL.drawString(10, height - 50, "speed: C " + (speed / 10f) + " V", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh, "Menu: N", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh * 2, "HIDE: H", Color.cyan);
		Fonts.SMALL.drawString(10, height - 50 - lh * 3, "obj: J " + index + " K", Color.cyan);
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

	public boolean keyPressed(int key) {
		if (!isActive) {
			return false;
		}
		if (key == Input.KEY_C && speed > 0) {
			speed -= 1;
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
		} else if (key == Input.KEY_K && index < gameObjects.length - 1) {
			index++;
			setMusicPosition();
		}
		return false;
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
		}
		this.gameObjects = gameObjects;
	}

	public boolean mousePressed(int button, int x, int y) {
		return menu && options.mousePressed(button, x, y);
	}

	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		if (menu) options.mouseDragged(oldx, oldy, newx, newy);
	}

	public void updateIndex(int index) {
		this.index = index;
	}

	public boolean mouseReleased(int button, int x, int y) {
		return menu && options.mouseReleased(button, x, y);
	}

	public boolean mouseWheelMoved(int newValue) {
		return menu && options.mouseWheenMoved(newValue);
	}
}
