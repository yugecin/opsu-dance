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

import itdelatrisu.opsu.objects.curves.Vec2f;
import org.newdawn.slick.Color;

public class QuadraticStoryboardMover extends StoryboardMultipointMover {

	public QuadraticStoryboardMover() {
		super(Color.magenta, Color.blue);
	}

	@Override
	public void setInitialStart(Vec2f start) {
		super.setInitialStart(start);
		super.movablePointCollectionRenderer.add(new Vec2f((start.x + end.x) / 2, (start.y + end.y) / 2));
	}

	@Override
	public float[] getPointAt(float t) {
		float ct = 1f - t;
		Vec2f p1 = super.movablePointCollectionRenderer.get(0);
		return new float[] {
			ct * ct * start.x + ct * 2 * t * p1.x + t * t * end.x,
			ct * ct * start.y + ct * 2 * t * p1.y + t * t * end.y,
		};
	}

	@Override
	public float getLength() {
		return length;
	}

}
