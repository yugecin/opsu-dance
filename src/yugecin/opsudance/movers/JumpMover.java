/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.movers;

import itdelatrisu.opsu.objects.GameObject;

public class JumpMover extends Mover {

	private double[] pos;

	public JumpMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		this.pos = new double[] {
			end.start.x,
			end.start.y
		};
	}

	@Override
	public double[] getPointAt(int time) {
		return pos;
	}

	@Override
	public String getName() {
		return "Jump";
	}

}
