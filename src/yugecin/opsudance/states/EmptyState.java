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
import yugecin.opsudance.core.Demux;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.kernel.InstanceContainer;

public class EmptyState implements OpsuState {

	private int counter;

	private final InstanceContainer instanceContainer;
	private final Demux demux;

	@Inject
	public EmptyState(InstanceContainer instanceContainer, Demux demux) {
		this.instanceContainer = instanceContainer;
		this.demux = demux;
	}

	@Override
	public void update(int delta) {
		counter -= delta;
		if (counter < 0) {
			demux.switchState(instanceContainer.provide(EmptyRedState.class));
		}
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

}
