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
package yugecin.opsudance.movers.factories;

import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.HalfEllipseMover;
import yugecin.opsudance.movers.Mover;

public class AutoEllipseMoverFactory extends AutoMoverFactory {

	@Override
	protected Mover donext(GameObject start, GameObject end, int dir, int dt, double velocity) {
		float mod = (float)dt / /*110f*/ 90f;
		HalfEllipseMover m1 = new HalfEllipseMover(start, end, dir);
		HalfEllipseMover m2 = new HalfEllipseMover(start, end, -dir);
		for( ; ; )
		{
			m1.setMod( mod );
			m2.setMod( mod );
			if( inbounds( m1 ) )
			{
				super.m = m1;
				break;
			}
			if( inbounds( m2 ) )
			{
				super.m = m2;
				break;
			}
			mod *= .8d;
		}
		return super.m;
	}

	@Override
	public String toString() {
		return "Auto ellipse size";
	}

}
