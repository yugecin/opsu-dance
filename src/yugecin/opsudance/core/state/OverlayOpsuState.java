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
package yugecin.opsudance.core.state;

import org.newdawn.slick.Graphics;

import java.io.StringWriter;

public abstract class OverlayOpsuState implements OpsuState {

	protected boolean active;
	protected boolean acceptInput;

	public abstract void hide();
	public abstract void show();

	@Override
	public final void update() {
	}

	public void revalidate() {
	}

	protected abstract void onPreRenderUpdate();

	@Override
	public final void preRenderUpdate() {
		if (active) {
			onPreRenderUpdate();
		}
	}

	protected abstract void onRender(Graphics g);

	@Override
	public final void render(Graphics g) {
		if (active) {
			onRender(g);
		}
	}

	@Override
	public final void enter() {
	}

	@Override
	public final void leave() {
	}

	@Override
	public final boolean onCloseRequest() {
		return true;
	}

	protected abstract boolean onKeyPressed(int key, char c);

	@Override
	public final boolean keyPressed(int key, char c) {
		return acceptInput && onKeyPressed(key, c);
	}

	protected abstract boolean onKeyReleased(int key, char c);

	@Override
	public final boolean keyReleased(int key, char c) {
		return acceptInput && onKeyReleased(key, c);
	}

	protected abstract boolean onMouseWheelMoved(int delta);

	@Override
	public final boolean mouseWheelMoved(int delta) {
		return acceptInput && onMouseWheelMoved(delta);
	}

	protected abstract boolean onMousePressed(int button, int x, int y);

	@Override
	public final boolean mousePressed(int button, int x, int y) {
		return acceptInput && onMousePressed(button, x, y);
	}

	protected abstract boolean onMouseReleased(int button, int x, int y);

	@Override
	public final boolean mouseReleased(int button, int x, int y) {
		return acceptInput && onMouseReleased(button, x, y);
	}

	protected abstract boolean onMouseDragged(int oldx, int oldy, int newx, int newy);

	@Override
	public final boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		return acceptInput && onMouseDragged(oldx, oldy, newx, newy);
	}

	@Override
	public void writeErrorDump(StringWriter dump) {
		dump.append("> OverlayOpsuState dump\n");
		dump.append("accepts input: ").append(String.valueOf(acceptInput)).append(" is active: ").append(String.valueOf(active));
	}
}
