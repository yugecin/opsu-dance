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
package yugecin.opsudance.states;

import com.google.inject.Inject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.state.OpsuState;

import java.io.StringWriter;

public class EmptyState implements OpsuState {

	private int counter;

	private final DisplayContainer displayContainer;

	@Inject
	public EmptyState(DisplayContainer displayContainer) {
		this.displayContainer = displayContainer;
	}

	@Override
	public void update(int delta) {
		counter -= delta;
		if (counter < 0) {
			counter = 10000; // to prevent more calls to switch, as this will keep rending until state transitioned
			displayContainer.demux.switchState(EmptyRedState.class);
		}
	}

	@Override
	public void preRenderUpdate(int delta) {
	}

	@Override
	public void render(Graphics g) {
		g.setColor(Color.green);
		g.fillRect(0, 0, 100, 100);
	}

	@Override
	public void enter() {
		counter = 2000;
	}

	@Override
	public void leave() {
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
	public void writeErrorDump(StringWriter dump) {
		dump.append("> EmptyState dump\n");
		dump.append("its green\n");
	}

}
