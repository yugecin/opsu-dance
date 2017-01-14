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

import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.ResolutionChangeListener;

public abstract class BaseOpsuState implements OpsuState, ResolutionChangeListener {

	protected final DisplayContainer displayContainer;

	/**
	 * state is dirty when resolution or skin changed but hasn't rendered yet
	 */
	private boolean isDirty;
	private boolean isCurrentState;

	public BaseOpsuState(DisplayContainer displayContainer) {
		this.displayContainer = displayContainer;
		displayContainer.addResolutionChangeListener(this);
	}

	protected void revalidate() {
	}

	@Override
	public void onDisplayResolutionChanged(int width, int height) {
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

}
