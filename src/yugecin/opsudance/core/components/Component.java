/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
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
package yugecin.opsudance.core.components;

import org.newdawn.slick.Graphics;

public abstract class Component {

	public int width;
	public int height;
	public int x;
	public int y;

	protected boolean focused;
	protected boolean hovered;

	public abstract boolean isFocusable();

	public boolean isHovered() {
		return hovered;
	}

	public void updateHover(int x, int y) {
		this.hovered = this.x <= x && x <= this.x + width && this.y <= y && y <= this.y + height;
	}

	public void mouseReleased(int button) {
	}

	public void preRenderUpdate() {
	}

	public abstract void render(Graphics g);

	public void keyPressed(int key, char c) {
	}

	public void keyReleased(int key, char c) {
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

}
