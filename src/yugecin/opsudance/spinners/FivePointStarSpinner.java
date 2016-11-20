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
package yugecin.opsudance.spinners;

import itdelatrisu.opsu.Options;

public class FivePointStarSpinner extends Spinner {

	@Override
	public void init() {
		double[][] points = new double[10][];
		double midx = Options.width / 2d;
		double midy = Options.height / 2d;
		double angleIncRads = Math.PI * 36d / 180d;
		double ang = -Math.PI / 2d;
		double maxrad = Options.width / 4d;
		double minrad = maxrad / 3d;
		for (int i = 0; i < 10; i++) {
			double rad = maxrad;
			if (i % 2 == 1) {
				rad = minrad;
			}
			points[i] = new double[] {
				midx + Math.cos(ang) * rad,
				midy + Math.sin(ang) * rad
			};
			ang += angleIncRads;
		}
		init(points);
	}

	@Override
	public String toString() {
		return "5-point star";
	}

}
