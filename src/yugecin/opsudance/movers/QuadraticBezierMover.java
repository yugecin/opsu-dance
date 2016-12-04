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

import java.awt.*;

public class QuadraticBezierMover extends Mover {

	private static Point p;
	private static double prevspeed;

	public static void reset() {
		p = new Point(0, 0);
		prevspeed = 0;
	}

	private int startTime;
	private int totalTime;

	public QuadraticBezierMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		this.startTime = start.getEndTime();
		this.totalTime = end.getTime() - startTime;

		double startAngle = Math.atan2(startY - p.y, startX - p.x);
		double angDiff = Math.atan2(startY - endY, startX - endX) - startAngle;
		while (angDiff < 0) angDiff += Math.PI;
		while (angDiff > Math.PI) angDiff -= Math.PI;
		angDiff -= Math.PI / 2;
		if (angDiff < 0) angDiff = -angDiff;
		double dist = Utils.distance(startX, startY, endX, endY);
		//double speed = dist / 10 + dist * (Math.PI - angDiff) / Math.PI;
		p.x = (int) (startX + Math.cos(startAngle) * prevspeed);
		p.y = (int) (startY + Math.sin(startAngle) * prevspeed);
		prevspeed = (dist / totalTime) * 50;
	}

	@Override
	public double[] getPointAt(int time) {
		double t = (double) (time - startTime) / totalTime;
		double ct = (1 - t);
		return new double[] {
			ct * ct * startX + ct * 2 * t * p.x + t * t * endX,
			ct * ct * startY + ct * 2 * t * p.y + t * t * endY,
		};
	}

	@Override
	public String getName() {
		return "Quadratic Bezier";
	}

}
