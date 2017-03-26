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

public class RektCircleSpinner extends Spinner {

	private double[] point;
	private int index;
	private int pos;
	private double size;
	private int delay = 0;

	@Override
	public void init()
	{
		index = 0;
		size = Options.height * 0.8d;
		point = new double[2];
	}

	@Override
	public double[] getPoint()
	{
		if (!waitForDelay()) {
			return point;
		}
		delay = 0;

		final int INC = 50;

		if( index == 0 )
		{
			point[0] = Options.width / 2d + size / 2d - pos;
			point[1] = Options.height / 2d - size / 2d;
			index++;
		}
		else if( index == 1 )
		{
			point[0] = Options.width / 2 - size / 2;
			point[1] = Options.height / 2 - size / 2 + pos;
			index++;
		}
		else if( index == 2 )
		{
			point[0] = Options.width / 2 - size / 2 + pos;
			point[1] = Options.height / 2 + size / 2;
			index++;
		}
		else if( index == 3 )
		{
			point[0] = Options.width / 2 + size / 2;
			point[1] = Options.height / 2 + size / 2 - pos;
			pos += INC;
			if( pos > size )
			{
				pos = INC;
			}
			index = 0;
		}

		return point;
	}

	@Override
	public String toString() {
		return "RektCircle";
	}

}
