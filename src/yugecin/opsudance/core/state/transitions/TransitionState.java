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
package yugecin.opsudance.core.state.transitions;

import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.state.BaseOpsuState;
import yugecin.opsudance.core.state.OpsuState;

public abstract class TransitionState extends BaseOpsuState {

	protected OpsuState applicableState;

	protected final int transitionTargetTime;
	protected int transitionTime;

	public TransitionState(int transitionTargetTime) {
		this.transitionTargetTime = transitionTargetTime;
	}

	public void setApplicableState(OpsuState applicableState) {
		this.applicableState = applicableState;
	}

	@Override
	public void update(int delta) {
		applicableState.update(delta);
		transitionTime += delta;
		if (transitionTime >= transitionTargetTime) {
			onTransitionFinished();
		}
	}

	@Override
	public void preRenderUpdate(int delta) {
	}

	@Override
	public void render(Graphics g) {
		applicableState.render(g);
	}

	@Override
	public void enter() {
		transitionTime = 0;
	}

	@Override
	public void leave() { }

	protected abstract void onTransitionFinished();

}
