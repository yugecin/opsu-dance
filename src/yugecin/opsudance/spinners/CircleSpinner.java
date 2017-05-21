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

import static yugecin.opsudance.core.InstanceContainer.*;

public class CircleSpinner extends Spinner {

	private int ang = 0;

	private double[] point = new double[2];

	@Override
	public void init() {
		ang = 0;
	}

	@Override
	public double[] getPoint() {
		if (waitForDelay()) {
			ang += 15;
		}

		double rad = displayContainer.width / 4.0f;

		point[0] = displayContainer.width / 2.0f + rad * Math.sin(ang / 180d * Math.PI);
		point[1] = displayContainer.height / 2.0f - rad * Math.cos(ang / 180d * Math.PI);

		return point;
	}

	@Override
	public String toString() {
		return "Circle";
	}

}
