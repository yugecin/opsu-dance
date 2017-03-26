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

import yugecin.opsudance.options.Options;

public class BeamSpinner extends Spinner {

	private double ang = 0;
	private double[] point;
	private int index;

	@Override
	public void init()
	{
		ang = 0;
		index = 0;
		point = new double[2];
	}

	@Override
	public double[] getPoint()
	{
		if (!waitForDelay()) {
			return point;
		}

		index = ++index % 4;
		final int MOD = 60;

		point[0] = Options.width / 2d;
		point[1] = Options.height / 2d;

		if( index == 0 )
		{
			add( MOD, 90 );
			add( MOD, 180 );
		}
		else if( index == 1 )
		{
			add( MOD, 90 );
			add( Options.height / 2 * 0.8d, 0 );
		}
		else if( index == 2 )
		{
			add( MOD, -90 );
			add( Options.height / 2 * 0.8d, 0 );
		}
		else if( index == 3 )
		{
			add( MOD, -90 );
			add( MOD, 180 );
			ang += 0.3;
		}

		return point;
	}

	private void add( double rad, double ang )
	{
		point[0] += rad * Math.cos( (this.ang + ang) / 180d * Math.PI);
		point[1] -= rad * Math.sin( (this.ang + ang) / 180d * Math.PI);
	}

	@Override
	public String toString() {
		return "Beam";
	}

}
