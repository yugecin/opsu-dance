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
import yugecin.opsudance.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class StoryboardMultipointMover extends StoryboardMover {

	private static final int POINTSIZE = 6;

	private List<Vec2f> points;
	private final Color pointColor;

	private Vec2f currentPoint;

	public StoryboardMultipointMover(Color renderColor, Color pointColor) {
		super(renderColor);
		this.pointColor = pointColor;
		this.points = new ArrayList<>();
	}

	public void addPoint(Vec2f point) {
		points.add(point);
	}

	@Override
	public void update(int delta, int x, int y) {
		if (currentPoint != null) {
			currentPoint.x = x;
			currentPoint.y = y;
		}
	}

	@Override
	public boolean mousePressed(int x, int y) {
		for (Vec2f point : points) {
			if (point.x - POINTSIZE <= x && x <= point.x + POINTSIZE && point.y - POINTSIZE <= y && y <= point.y + POINTSIZE) {
				currentPoint = point;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(int x, int y) {
		if (currentPoint != null) {
			currentPoint = null;
			return true;
		}
		return false;
	}

	protected Vec2f getPoint(int index) {
		return points.get(index);
	}

	@Override
	public void render(Graphics g) {
		g.setColor(Color.gray);
		Vec2f lastPoint = start;
		for (Vec2f point : points) {
			RenderUtils.drawDottedLine(g, lastPoint.x, lastPoint.y, point.x, point.y, 20, 0);
			lastPoint = point;
		}
		RenderUtils.drawDottedLine(g, lastPoint.x, lastPoint.y, end.x, end.y, 20, 0);
		g.setColor(pointColor);
		for (Vec2f point : points) {
			RenderUtils.fillCenteredRect(g, point.x, point.y, POINTSIZE);
		}
		super.render(g);
	}
}
