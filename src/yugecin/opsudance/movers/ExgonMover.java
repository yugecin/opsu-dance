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
package yugecin.opsudance.movers;

import itdelatrisu.opsu.objects.GameObject;

import java.util.Random;

import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class ExgonMover extends Mover {

	private double[] pos;
	private int nextTime;
	private static final Random randgen = new Random();

	public ExgonMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		nextTime = start.getEndTime() + OPTION_DANCE_EXGON_DELAY.val;
		pos = new double[] { start.end.x, start.end.y };
	}

	@Override
	public double[] getPointAt(int time) {
		if (time > nextTime) {
			nextTime = time + OPTION_DANCE_EXGON_DELAY.val;
			if (time > getEnd().getEndTime() - OPTION_DANCE_EXGON_DELAY.val) {
				pos[0] = endX;
				pos[1] = endY;
			} else {
				pos[0] = randgen.nextInt(displayContainer.width);
				pos[1] = randgen.nextInt(displayContainer.height);
			}
		}
		return pos;
	}

	@Override
	public String getName() {
		return "ExGon";
	}

}
