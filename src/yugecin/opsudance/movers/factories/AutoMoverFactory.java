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

import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.Pippi;
import yugecin.opsudance.movers.*;

public class AutoMoverFactory implements MoverFactory {

	public static boolean ONLY_CIRCLE_STACKS = false;
	public static int CIRCLE_STREAM = 58;
	private int endtime;
	protected Mover m;

	@Override
	public Mover create(GameObject start, GameObject end, int dir) {
		int dt = end.getTime() - start.getEndTime();
		double distance = Utils.distance(start.end.x, start.end.y, end.start.x, end.start.y);

		// linear if very fast
		if (dt < 40) {
			return new LinearMover(start, end, dir);
		}

		// stacked: circles if not too quick
		if (distance < Circle.diameter && ((dt > CIRCLE_STREAM && !ONLY_CIRCLE_STACKS) || distance < HitObject.getStackOffset() * 1.1f)) {
			return new CircleMover(start, end, dir);

		}

		if (Pippi.shouldPreventWobblyStream(distance)) {
			return new LinearMover(start, end, dir);
		}

		endtime = end.getTime();

		double velocity = distance / dt;
		return donext(start, end, dir, dt, velocity);
	}

	protected Mover donext(GameObject start, GameObject end, int dir, int dt, double velocity) {
		if( velocity < 4d )
		{
			// ellips, if in bounds
			if( inbounds( new HalfEllipseMover( start, end, dir ) ) ) return m;
			if( inbounds( new HalfEllipseMover( start, end, -dir ) ) ) return m;
		}

		if( velocity < 5.5d )
		{
			// halfcircle, if in bounds
			if( inbounds( new HalfCircleMover( start, end, dir ) ) ) return m;
			if( inbounds( new HalfCircleMover( start, end, -dir ) ) ) return m;
		}

		if( velocity < 7d )
		{
			// halfellips with 0.5 modifier
			HalfEllipseMover m;
			m = new HalfEllipseMover(start, end, dir);
			m.setMod(0.5d);
			if( inbounds(m) ) return m;
			m = new HalfEllipseMover(start, end, -dir);
			m.setMod(0.5d);
			if( inbounds(m) ) return m;
		}

		// quart circle
		Mover last = new QuartCircleMover( start, end, dir );
		if( inbounds( last ) ) return m;
		if( inbounds( new QuartCircleMover( start, end, -dir ) ) ) return m;
		return last;
	}

	protected boolean inbounds( Mover m )
	{
		this.m = m;
		return checkBounds(m.getPointAt(endtime));
	}

	private boolean checkBounds( double[] pos )
	{
		return 0 < pos[0] && pos[0] < Options.width - Options.width / 8 && 0 < pos[1] && pos[1] < Options.height - Options.height / 8;
	}

	@Override
	public String toString() {
		return "Auto decide";
	}

}
