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

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.curves.Vec2f;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.render.RenderUtils;

public abstract class StoryboardMover {

	public static final float CALC_DRAW_INTERVAL = 0.05f;

	protected float length;
	public Vec2f start;
	public Vec2f end;
	private final Color renderColor;
	public float timeLengthPercentOfTotalTime;

	public StoryboardMover(Color renderColor) {
		this.renderColor = renderColor;
	}

	public abstract float[] getPointAt(float t);
	public float getLength() {
		return length;
	}

	/**
	 * Set the start position after just creating this mover
	 * @param start start position
	 */
	public void setInitialStart(Vec2f start) {
		this.start = start;
	}

	public void update(int delta, int x, int y) {
	}

	/**
	 *
	 * @param x x pos of mouse
	 * @param y y pos of mouse
	 * @return true if mouse pressed something and consecutive checks should be ignored
	 */
	public boolean mousePressed(int x, int y) {
		return false;
	}

	/**
	 *
	 * @param x x pos of mouse
	 * @param y y pos of mouse
	 * @return true if mouse released something and length should be recalculated
	 */
	public boolean mouseReleased(int x, int y) {
		return false;
	}

	public void render(Graphics g) {
		g.setColor(renderColor);
		for (float t = 0; t <= 1f; t += StoryboardMover.CALC_DRAW_INTERVAL) {
			float[] p = getPointAt(t);
			RenderUtils.fillCenteredRect(g, p[0], p[1], 1);
		}
	}

	public void recalculateLength() {
		this.length = 0;
		float[] lastPoint = new float[] { start.x, start.y };
		for (float t = StoryboardMover.CALC_DRAW_INTERVAL; t <= 1f; t += StoryboardMover.CALC_DRAW_INTERVAL) {
			float[] p = getPointAt(t);
			this.length += Utils.distance(lastPoint[0], lastPoint[1], p[0], p[1]);
			lastPoint = p;
		}
	}

}
