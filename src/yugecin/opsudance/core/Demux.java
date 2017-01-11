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
import yugecin.opsudance.core.state.BaseOpsuState;
import yugecin.opsudance.core.state.transitions.*;
import yugecin.opsudance.kernel.InstanceContainer;
import yugecin.opsudance.core.state.OpsuState;

/**
 * state demultiplexer, sends events to current state
 */
public class Demux implements KeyListener, MouseListener {

	private final InstanceContainer instanceContainer;

	private TransitionState outTransitionState;
	private TransitionState inTransitionState;

	private final TransitionFinishedListener outTransitionListener;
	private final TransitionFinishedListener inTransitionListener;

	private OpsuState state;

	@Inject
	public Demux(final InstanceContainer instanceContainer) {
		this.instanceContainer = instanceContainer;

		state = new BaseOpsuState() {

			@Override
			public void update(int delta) { }

			@Override
			public void preRenderUpdate(int delta) { }

			@Override
			public void render(Graphics g) { }

			@Override
			public void enter() { }

			@Override
			public void leave() { }

		};

		outTransitionListener = new TransitionFinishedListener() {
			@Override
			public void onFinish() {
				state.leave();
				outTransitionState.getApplicableState().leave();
				state = inTransitionState;
				state.enter();
				inTransitionState.getApplicableState().enter();
			}
		};

		inTransitionListener = new TransitionFinishedListener() {
			@Override
			public void onFinish() {
				state.leave();
				state = inTransitionState.getApplicableState();
			}
		};
	}

	public boolean isTransitioning() {
		return state instanceof TransitionState;
	}

	public void switchState(Class<? extends OpsuState> newState) {
		switchState(newState, FadeOutTransitionState.class, 200, FadeInTransitionState.class, 300);
	}

	public void switchState(Class<? extends OpsuState> newState, Class<? extends TransitionState> outTransition, int outTime, Class<? extends TransitionState> inTransition, int inTime) {
		if (isTransitioning()) {
			return;
		}
		outTransitionState = instanceContainer.provide(outTransition).set(state, outTime, outTransitionListener);
		inTransitionState = instanceContainer.provide(inTransition).set(instanceContainer.provide(newState), inTime, inTransitionListener);
		state = outTransitionState;
		state.enter();
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
		return state.onCloseRequest();
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
