/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
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
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */

package yugecin.opsudance.ui;

import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.state.StateBasedGame;

import java.util.Observer;

public class ItemList {

	private int x;
	private int y;
	private int width;
	private int height;

	private int start;

	private Object[] items;
	private Observer observer;

	private boolean visible;

	public void init(GameContainer container) {
		x = container.getWidth() / 6;
		y = 0;
		width = x * 4;
		height = container.getHeight();
	}

	public void setItems(Object[] items) {
		this.items = items;
	}

	public void setClickListener(Observer o) {
		observer = o;
	}

	public boolean isVisible() {
		return visible;
	}

	public void show() {
		visible = true;
	}

	public void render(GameContainer container, StateBasedGame game, Graphics g) {
		g.setColor(Color.black);
		g.fillRect(x, y, width, height);
		int y = this.y + 5;
		for (int i = start, m = items.length; i < m; i++) {
			if (container.getInput().getMouseX() >= x && container.getInput().getMouseX() < x + width) {
				if (container.getInput().getMouseY() >= y && container.getInput().getMouseY() < y + Fonts.MEDIUM.getLineHeight() + 5) {
					g.setColor(new Color(0x11A9FF));
					g.fillRect(x, y, width, Fonts.MEDIUM.getLineHeight() + 5);
				}
			}
			Fonts.MEDIUM.drawString(x + 5, y + 2, items[i].toString(), Color.white);
			y += Fonts.MEDIUM.getLineHeight() + 5;
			if (y >= height - Fonts.MEDIUM.getLineHeight()) {
				break;
			}
		}
	}

	public void mousePressed(int button, int x, int y) {
		if (UI.getBackButton().contains(x, y)) {
			visible = false;
		}
		if (x < this.x || x > this.x + width) {
			visible = false;
			return;
		}
		if (button != 0) {
			return;
		}
		if (y < 5) {
			return;
		}
		y -= 5;
		int index = y / (Fonts.MEDIUM.getLineHeight() + 5);
		index -= start;
		if (index >= 0 && index < items.length) {
			observer.update(null, index);
			visible = false;
		}
	}

	public void mouseDragged(int oldx, int oldy, int x, int y) {

	}

	public void mouseWheelMoved(int delta) {
		if (delta > 0) {
			start = Math.max(0, start - 1);
		} else {
			start = Math.min(items.length - 1, start + 1);
		}
	}

	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			visible = false;
		}
	}

}
