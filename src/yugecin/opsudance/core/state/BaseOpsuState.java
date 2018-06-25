/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017-2018 yugecin
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
import yugecin.opsudance.events.ResolutionChangedListener;

import java.io.StringWriter;

import static yugecin.opsudance.core.InstanceContainer.*;

public abstract class BaseOpsuState implements OpsuState, ResolutionChangedListener {

	/**
	 * state is dirty when resolution or skin changed but hasn't rendered yet
	 */
	private boolean isDirty;
	private boolean isCurrentState;

	public BaseOpsuState() {
		displayContainer.addResolutionChangedListener(this);
	}

	protected void revalidate() {
	}

	@Override
	public void update() {
	}

	@Override
	public void preRenderUpdate() {
	}

	@Override
	public void render(Graphics g) {
	}

	@Override
	public void onResolutionChanged(int w, int h) {
		if (isCurrentState) {
			revalidate();
			return;
		}
		isDirty = true;
	}

	@Override
	public void enter() {
		isCurrentState = true;
		if (isDirty) {
			revalidate();
			isDirty = false;
		}
	}

	@Override
	public void leave() {
		isCurrentState = false;
	}

	@Override
	public boolean onCloseRequest() {
		return true;
	}

	@Override
	public boolean keyPressed(int key, char c) {
		return false;
	}

	@Override
	public boolean keyReleased(int key, char c) {
		return false;
	}

	@Override
	public boolean mouseWheelMoved(int delta) {
		return false;
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		return false;
	}

	@Override
	public boolean mouseReleased(int button, int x, int y) {
		return false;
	}

	@Override
	public boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		return false;
	}

	@Override
	public void writeErrorDump(StringWriter dump) {
		dump.append("> BaseOpsuState dump\n");
		dump.append("isDirty: ").append(String.valueOf(isDirty)).append('\n');
	}

}
