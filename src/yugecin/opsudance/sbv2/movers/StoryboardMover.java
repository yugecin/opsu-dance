/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
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
package yugecin.opsudance.sbv2.movers;

import itdelatrisu.opsu.objects.curves.Vec2f;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

public abstract class StoryboardMover {

	public static final float CALC_DRAW_INTERVAL = 0.05f;

	protected float length;
	public Vec2f start;
	public Vec2f end;
	private Color renderColor;
	public float timeLengthPercentOfTotalTime;

	public StoryboardMover(Color renderColor) {
		this.renderColor = renderColor;
	}

	public abstract float[] getPointAt(float t);
	public abstract void recalculateLength();

	public float getLength() {
		return length;
	}

	public void render(Graphics g) {
		g.setColor(renderColor);
		for (float t = 0; t <= 1f; t += StoryboardMover.CALC_DRAW_INTERVAL) {
			float[] p = getPointAt(t);
			g.fillRect(p[0] - 1, p[1] - 1, 3, 3);
		}
	}

}
