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

import java.io.StringWriter;

public abstract class TransitionState extends BaseOpsuState {

	protected OpsuState applicableState;

	protected int transitionTargetTime;
	protected int transitionTime;

	private TransitionFinishedListener listener;

	public final TransitionState set(OpsuState applicableState, int targetTime, TransitionFinishedListener listener) {
		this.applicableState = applicableState;
		this.transitionTargetTime = targetTime;
		this.listener = listener;
		return this;
	}

	public final OpsuState getApplicableState() {
		return applicableState;
	}

	@Override
	public void update() {
		applicableState.update();
		transitionTime += displayContainer.delta;
		if (transitionTime >= transitionTargetTime) {
			finish();
		}
	}

	@Override
	public void preRenderUpdate() {
		applicableState.preRenderUpdate();
	}

	@Override
	public void render(Graphics g) {
		applicableState.render(g);
	}

	@Override
	public void enter() {
		super.enter();
		transitionTime = 0;
	}

	protected final void finish() {
		listener.onFinish();
	}

	@Override
	public boolean onCloseRequest() {
		return false;
	}

	@Override
	public void writeErrorDump(StringWriter dump) {
		dump.append("> TransitionState dump\n");
		dump.append("progress: ").append(String.valueOf(transitionTime)).append("/").append(String.valueOf(transitionTargetTime)).append('\n');
		dump.append("applicable state: ");
		if (applicableState == null) {
			dump.append("IS NULL");
			return;
		}
		dump.append(applicableState.getClass().getSimpleName()).append('\n');
		applicableState.writeErrorDump(dump);
	}

}
