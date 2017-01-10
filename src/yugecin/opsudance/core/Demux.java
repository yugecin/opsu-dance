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
import org.newdawn.slick.Input;
import org.newdawn.slick.KeyListener;
import org.newdawn.slick.MouseListener;
import yugecin.opsudance.kernel.InstanceContainer;
import yugecin.opsudance.states.EmptyState;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.core.state.transitions.FadeInTransitionState;
import yugecin.opsudance.core.state.transitions.FadeOutTransitionState;
import yugecin.opsudance.core.state.transitions.TransitionState;

/**
 * state demultiplexer, sends events to current state
 */
public class Demux implements KeyListener, MouseListener {

	private final InstanceContainer instanceContainer;

	private TransitionState fadeOutTransitionState;
	private TransitionState fadeInTransitionState;

	private OpsuState state;

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
	public boolean isTransitioning() {
		return state == fadeInTransitionState || state == fadeOutTransitionState;
	}

	public void switchState(Class<? extends OpsuState> newState) {
		switchState(instanceContainer.provide(newState));
	}

	public void switchState(OpsuState newState) {
		if (isTransitioning()) {
			return;
		}
		fadeOutTransitionState.setApplicableState(state);
		fadeInTransitionState.setApplicableState(newState);
		state = fadeOutTransitionState;
		state.enter();
	}

	public void switchStateNow(OpsuState newState) {
		if (!isTransitioning()) {
			return;
		}
		state = newState;
	}

	/*
	 * demux stuff below
	 */

	public void update(int delta) {
		state.update(delta);
	}

	public void preRenderUpdate(int delta) {
		state.preRenderUpdate(delta);
	}

	public void render(Graphics g) {
		state.render(g);
	}

	public boolean onCloseRequest() {
		return !isTransitioning();
	}

	/*
	 * input events below, see org.newdawn.slick.KeyListener & org.newdawn.slick.MouseListener
	 */

	@Override
	public void keyPressed(int key, char c) {
		state.keyPressed(key, c);
	}

	@Override
	public void keyReleased(int key, char c) {
		state.keyReleased(key, c);
	}

	@Override
	public void mouseWheelMoved(int change) {
		state.mouseWheelMoved(change);
	}

	@Override
	public void mouseClicked(int button, int x, int y, int clickCount) { }

	@Override
	public void mousePressed(int button, int x, int y) {
		state.mousePressed(button, x, y);
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		state.mouseReleased(button, x, y);
	}

	@Override
	public void mouseMoved(int oldx, int oldy, int newx, int newy) { }

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) { }

	@Override
	public void setInput(Input input) { }

	@Override
	public boolean isAcceptingInput() {
		return true;
	}

	@Override
	public void inputEnded() { }

	@Override
	public void inputStarted() { }

}
