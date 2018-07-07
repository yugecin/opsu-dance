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
package yugecin.opsudance.spinners;

import static yugecin.opsudance.core.InstanceContainer.*;

public class FivePointStarApproachSpinner extends Spinner {

	final double angleIncRads = Math.PI * 36d / 180d;
	double ang;
	final double[] point = new double[2];
	boolean odd;

	@Override
	public void init() {
		ang = -Math.PI / 2d;
		odd = true;
	}

	@Override
	public double[] getPoint() {
		if (waitForDelay()) {
			ang += angleIncRads;
			odd = !odd;
		}

		double rad = width / 4.0f * (1d - Spinner.PROGRESS);
		if (!odd) {
			rad /= 3d;
		}
		point[0] = width2 + Math.cos(ang) * rad;
		point[1] = height2 + Math.sin(ang) * rad;
		return point;
	}

	@Override
	public String toString() {
		return "5-point approach star";
	}

}
