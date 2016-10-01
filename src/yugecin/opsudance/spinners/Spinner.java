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

public abstract class Spinner {

	private double[][] points;
	private int length;
	private int index;
	private int delay;
	public static int DELAY = 3;

	public abstract void init();

	protected final void init(double[][] points) {
		this.points = points;
		this.length = points.length;
	}

	public double[] getPoint() {
		if (++delay > DELAY) {
			index = ++index % length;
			delay = 0;
		}
		return points[index];
	}

}
