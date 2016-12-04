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
package yugecin.opsudance.movers;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.curves.Vec2f;

import java.awt.*;

public class CubicBezierMover extends Mover {

	public static int aggressivenessfactor = 4;

	private static Point p2 = new Point(0, 0);
	private static Point p1 = new Point(0, 0);

	private int startTime;
	private int totalTime;

	public CubicBezierMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);

		if (end instanceof Slider) {
			Slider s = (Slider) end;
			double ang = s.getCurve().getStartAngle() * Math.PI / 180d + Math.PI;
			Vec2f nextpos = s.getPointAt(s.getTime() + 10);
			double dist = Utils.distance(end.start.x, end.start.y, nextpos.x, nextpos.y);
			double speed = dist * QuadraticBezierMover.aggressiveness * aggressivenessfactor / 10;
			p2.x = (int) (end.start.x + Math.cos(ang) * speed);
			p2.y = (int) (end.start.y + Math.sin(ang) * speed);
		}

		this.startTime = start.getEndTime();
		this.totalTime = end.getTime() - startTime;

		double startAngle = Math.atan2(startY - QuadraticBezierMover.p.y, startX - QuadraticBezierMover.p.x);
		p1.x = (int) (startX + Math.cos(startAngle) * QuadraticBezierMover.getPrevspeed());
		p1.y = (int) (startY + Math.sin(startAngle) * QuadraticBezierMover.getPrevspeed());
	}

	@Override
	public double[] getPointAt(int time) {
		double t = (double) (time - startTime) / totalTime;
		double ct = (1 - t);
		return new double[] {
			ct * ct * ct * startX + 3 * ct * ct * t * p1.x + 3 * ct * t * t * p2.x + t * t * t * endX,
			ct * ct * ct * startY + 3 * ct * ct * t * p1.y + 3 * ct * t * t * p2.y + t * t * t * endY,
		};
	}

	@Override
	public String getName() {
		return "Quadratic Bezier";
	}

}
