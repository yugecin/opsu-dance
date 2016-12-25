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
package yugecin.opsudance.sbv2.movers;

import itdelatrisu.opsu.Utils;
import org.newdawn.slick.Color;

public class LinearStoryboardMover extends StoryboardMover {

	public LinearStoryboardMover() {
		super(Color.red);
	}

	@Override
	public float[] getPointAt(float t) {
		return new float[] {
			Utils.lerp(start.x, end.x, t),
			Utils.lerp(start.y, end.y, t),
		};
	}

	@Override
	public float getLength() {
		return length;
	}

	@Override
	public void recalculateLength() {
		length = Utils.distance(start.x, start.y, end.x, end.y);
	}

}
