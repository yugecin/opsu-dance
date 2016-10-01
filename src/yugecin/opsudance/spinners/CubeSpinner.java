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

public class CubeSpinner extends Spinner {

	private int[][] points = {
		{ -1, -1, 1 },
		{ 1, -1, 1 },
		{ 1, 1, 1 },
		{ -1, 1, 1 },
		{ -1, -1, 1 },
		{ -1, -1, -1 },
		{ -1, 1, -1 },
		{ -1, 1, 1 },
		{ -1, 1, -1 },
		{ 1, 1, -1 },
		{ 1, 1, 1 },
		{ 1, 1, -1 },
		{ 1, -1, -1 },
		{ 1, -1, 1 },
		{ 1, -1, -1 },
		{ -1, -1, -1 },
		{ -1, -1, 1 },
	};

	private int azi = 0;
	private int alt = 73;

	public double azimuth = 15.0d;
	public double altitude = 95.0d;

	private double size = 0;

	private int delay = 0;
	private int index = 0;

	private double[] point = new double[2];

	@Override
	public void init()
	{
		azi = 0;
		alt = 73;
	}

	@Override
	public double[] getPoint()
	{
		if( ++delay <= DELAY )
		{
			return point;
		}
		delay = 0;

		if( ++index >= 16 )
		{
			index = 0;
			azimuth += 2.0d;
			altitude += 6.0d;
			size += 15d;
			//size = 0;
			azi += 5;
			alt += 9;
			azimuth = 30d * Math.cos( azi / 180d * Math.PI );
			altitude = 30d * Math.cos( alt / 180d * Math.PI );
		}

		double theta = Math.PI * azimuth / 180.0d;
		double phi = Math.PI * altitude / 180.0d;

		double cosT = Math.cos( theta ), sinT = Math.sin(theta );
		double cosP = Math.cos( phi ), sinP = Math.sin( phi );

		double x = cosT * points[index][0] + sinT * points[index][2];
		double y = -sinT * sinP * points[index][0] + cosP * points[index][1] + cosT * sinP * points[index][2];

		// fix depth
		double z = cosT * cosP * points[index][2] - sinT * cosP * points[index][0] - sinP * points[index][1];
		x *= 3.0d / ( z + 3.0d + 5.0d + 0.5 );
		y *= 3.0d / ( z + 3.0d + 5.0d + 0.5 );

		double scale = Options.width / (3.0f + 0.5f * Math.cos(size / 180f * Math.PI));
		//double scale = Options.width / (3.0f + -1f * ((float)(Options.s.ElapsedMilliseconds % Options.beatTimeMs)/(float)Options.beatTimeMs));
		point[0] = (int) ( Options.width / 2.0f + scale * x );
		point[1] = (int) ( Options.height / 2.0f - scale * y );

		return point;
	}

	@Override
	public String toString() {
		return "Cube";
	}

}
