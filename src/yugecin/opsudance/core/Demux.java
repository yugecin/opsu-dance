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
package yugecin.opsudance.core;

import com.google.inject.Inject;
import org.newdawn.slick.Game;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import yugecin.opsudance.kernel.InstanceContainer;
import yugecin.opsudance.states.EmptyState;
import yugecin.opsudance.states.GameState;

public class Demux implements Game {

	private final InstanceContainer instanceContainer;

	private GameState currentState;

	@Inject
	public Demux(InstanceContainer instanceContainer) {
		this.instanceContainer = instanceContainer;
	}

	@Override
	public void init(GameContainer container) throws SlickException {
		currentState = instanceContainer.provide(EmptyState.class);
	}

	@Override
	public void update(GameContainer container, int delta) throws SlickException {
		currentState.update(delta);
	}

	@Override
	public void render(GameContainer container, Graphics g) throws SlickException {
		currentState.render(g);
	}

	@Override
	public boolean closeRequested() {
		return false;
	}

	@Override
	public String getTitle() {
		return "opsu!dance";
	}

}
