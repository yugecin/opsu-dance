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
import org.newdawn.slick.Graphics;
import yugecin.opsudance.kernel.InstanceContainer;
import yugecin.opsudance.states.EmptyState;
import yugecin.opsudance.states.GameState;
import yugecin.opsudance.states.transitions.FadeInTransitionState;
import yugecin.opsudance.states.transitions.FadeOutTransitionState;
import yugecin.opsudance.states.transitions.TransitionState;

/**
 * state demultiplexer, sends events to current state
 */
public class Demux {

	private final InstanceContainer instanceContainer;

	private TransitionState fadeOutTransitionState;
	private TransitionState fadeInTransitionState;

	private GameState state;

	@Inject
	public Demux(InstanceContainer instanceContainer) {
		this.instanceContainer = instanceContainer;
	}

	// cannot do this in constructor, would cause circular dependency
	public void init() {
		state = instanceContainer.provide(EmptyState.class);
		fadeOutTransitionState = instanceContainer.provide(FadeOutTransitionState.class);
		fadeInTransitionState = instanceContainer.provide(FadeInTransitionState.class);
	}

	public void update(int delta) {
		state.update(delta);
	}

	public void render(Graphics g) {
		state.render(g);
	}

	public boolean onCloseRequest() {
		return !isTransitioning();
	}

	public boolean isTransitioning() {
		return state == fadeInTransitionState || state == fadeOutTransitionState;
	}

	public void switchState(GameState newState) {
		if (isTransitioning()) {
			return;
		}
		fadeOutTransitionState.setApplicableState(state);
		fadeInTransitionState.setApplicableState(newState);
		state = fadeOutTransitionState;
		state.enter();
	}

	public void switchStateNow(GameState newState) {
		if (!isTransitioning()) {
			return;
		}
		state = newState;
	}

}
