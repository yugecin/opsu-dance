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
package yugecin.opsudance.render;

import itdelatrisu.opsu.objects.curves.Vec2f;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import java.util.ArrayList;

public class MovablePointCollectionRenderer extends ArrayList<Vec2f> {

	private static final int POINTSIZE = 6;

	private final Color pointColor;

	private Vec2f currentPoint;
	private int currentPointIndex;

	public MovablePointCollectionRenderer(Color pointColor) {
		this.pointColor = pointColor;
	}

	public int getCurrentPointIndex() {
		return currentPointIndex;
	}

	public boolean update(int x, int y) {
		if (currentPoint != null) {
			currentPoint.x = x;
			currentPoint.y = y;
			return true;
		}
		return false;
	}

	public boolean mousePressed(int x, int y) {
		currentPointIndex = 0;
		for (Vec2f point : this) {
			if (point.x - POINTSIZE <= x && x <= point.x + POINTSIZE && point.y - POINTSIZE <= y && y <= point.y + POINTSIZE) {
				currentPoint = point;
				return true;
			}
			currentPointIndex++;
		}
		return false;
	}

	public boolean mouseReleased() {
		if (currentPoint != null) {
			currentPoint = null;
			return true;
		}
		return false;
	}

	public void render(Graphics g) {
		g.setColor(pointColor);
		for (Vec2f point : this) {
			RenderUtils.fillCenteredRect(g, point.x, point.y, POINTSIZE);
		}
	}

	public void renderWithDottedLines(Graphics g, Color lineColor, Vec2f start, Vec2f end) {
		g.setColor(lineColor);
		Vec2f lastPoint = start;
		for (Vec2f point : this) {
			if (lastPoint == null) {
				continue;
			}
			RenderUtils.drawDottedLine(g, lastPoint.x, lastPoint.y, point.x, point.y, 20, 0);
			lastPoint = point;
		}
		if (lastPoint != null && end != null) {
			RenderUtils.drawDottedLine(g, lastPoint.x, lastPoint.y, end.x, end.y, 20, 0);
		}
		render(g);
	}

}
