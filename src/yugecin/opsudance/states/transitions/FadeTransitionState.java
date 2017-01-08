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
package yugecin.opsudance.states.transitions;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.Container;
import yugecin.opsudance.states.GameState;

public abstract class FadeTransitionState extends TransitionState {

	protected GameState applicableState;

	private final Container container;

	protected final int fadeTargetTime;
	protected int fadeTime;

	private final Color black;

	public FadeTransitionState(Container container, int fadeTargetTime) {
		super(fadeTargetTime);
		this.container = container;
		this.fadeTargetTime = fadeTargetTime;
		black = new Color(Color.black);
	}

	public void setApplicableState(GameState applicableState) {
		this.applicableState = applicableState;
	}

	@Override
	public void update(int delta) {
		applicableState.update(delta);
		fadeTime += delta;
		if (fadeTime >= fadeTargetTime) {
			onTransitionFinished();
		}
	}

	@Override
	public void render(Graphics g) {
		applicableState.render(g);
		black.a = getMaskAlphaLevel((float) fadeTime / fadeTargetTime);
		g.setColor(black);
		g.fillRect(0, 0, container.getWidth(), container.getHeight());
	}

	@Override
	public void enter() {
		fadeTime = 0;
	}

	@Override
	public void leave() { }

	protected abstract float getMaskAlphaLevel(float fadeProgress);

}
