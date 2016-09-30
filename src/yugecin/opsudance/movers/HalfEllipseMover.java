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

public class HalfEllipseMover extends Mover {

	private double middlex;
	private double middley;
	private double radius;
	private double ang;
	private double mod;

	public HalfEllipseMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		middlex = (startX + endX) / 2d;
		middley = (startY + endY) / 2d;
		radius = Utils.distance(middlex, middley, startX, startY);
		ang = Math.atan2(startY - middley, startX - middlex);
		mod = 2d;
	}

	public void setMod(double mod) {
		this.mod = mod;
	}

	@Override
	public double[] getPointAt(int time) {
		double Tangle = Math.PI * getT(time) * dir;
		double x = middlex + Math.cos(Tangle) * radius;
		double y = middley + Math.sin(Tangle) * radius * mod;
		double dx = middlex - x;
		double dy = middley - y;

		double d = Math.sqrt(dx * dx + dy * dy);
		double a = Math.atan2(dy, dx);
		double my = a - ang;

		return new double[] {
			middlex - Math.cos(my) * d,
			middley + Math.sin(my) * d
		};
	}

	@Override
	public String getName() {
		return "Half ellipse " + mod;
	}

}
