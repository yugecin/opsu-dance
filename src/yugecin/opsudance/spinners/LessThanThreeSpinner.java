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

import static java.lang.Math.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class LessThanThreeSpinner extends Spinner {

	private int angle = 0;

	@Override
	public void init() {
	}

	@Override
	public double[] getPoint()
	{
		if (waitForDelay()) {
			angle += 18;
		}
		if( angle > 360 ) angle = 0;
		double theta = angle / 180d * Math.PI;
		double[] pos = { width2, height2 };

		double r = 2 - 2 * sin(theta) + sin(theta) * sqrt(abs(cos(theta))) / (sin(theta) + 1.4);

		pos[0] += Math.cos(theta) * r * 100;
		pos[1] -= Math.sin(theta) * r * 100 + 100;

		return pos;
	}

	@Override
	public String toString() {
		return "LessThanThree";
	}

}
