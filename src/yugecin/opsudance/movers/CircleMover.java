/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016-2018 yugecin
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

import static yugecin.opsudance.core.InstanceContainer.*;

public class CircleMover extends Mover {

	private double radius;
	private double SOME_CONSTANT;
	private double middlexoffset;
	private double middleyoffset;
	private double ang;

	public CircleMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		if (startX - endX == 0 && startY - endY == 0) {
			int quadr = Utils.getRegion(startX, startY);
			switch (quadr) {
				case 3: ang = 135d / 180d * Math.PI; break;
				case 2: ang = 45d / 180d * Math.PI; break;
				case 0: ang = -45d / 180d * Math.PI; break;
				case 1: ang = -135d / 180d * Math.PI; break;
			}
		} else {
			ang = Math.atan2(startY - endY, startX - endX);
		}
		SOME_CONSTANT = dir * 2d * Math.PI;
		// TODO: circle into slider?
		radius = end.getTime() - start.getEndTime();
		if (start.isSpinner() || end.isSpinner()) {
			middlexoffset = -Math.cos(ang) * radius;
			middleyoffset = -Math.sin(ang) * radius;
		} else {
			calcMaxRadius();
		}
	}

	private void calcMaxRadius() {
		double[] pos = new double[2];
		for (int tries = 0; tries < 7; tries++) {
			middlexoffset = -Math.cos(ang) * radius;
			middleyoffset = -Math.sin(ang) * radius;
			boolean pass = true;
			for (double t = 0d; t < 1d; t += 0.1d) {
				double a = ang + SOME_CONSTANT * t;
				pos[0] = (startX + (endX - startX) * t) - middlexoffset - Math.cos(a) * radius;
				pos[1] = (startY + (endY - startY) * t) - middleyoffset - Math.sin(a) * radius;
				if (pos[0] < 0 || width < pos[0] || pos[1] < 0 || height < pos[1]) {
					pass = false;
					break;
				}
			}
			if (pass) {
				return;
			}
			radius *= 0.8d;
		}
	}

	@Override
	public double[] getPointAt(int time) {
		double t = getT(time);
		double ang = this.ang + SOME_CONSTANT * t;
		return new double[] {
			(startX + (endX - startX) * t) - middlexoffset - Math.cos(ang) * radius,
			(startY + (endY - startY) * t) - middleyoffset - Math.sin(ang) * radius
		};
	}

	@Override
	public String getName() {
		return "Circle";
	}

}
