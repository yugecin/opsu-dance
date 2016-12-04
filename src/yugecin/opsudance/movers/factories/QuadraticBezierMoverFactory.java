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
import yugecin.opsudance.movers.Mover;
import yugecin.opsudance.movers.QuadraticBezierMover;

public class QuadraticBezierMoverFactory implements MoverFactory {

	@Override
	public Mover create(GameObject start, GameObject end, int dir) {
		return new QuadraticBezierMover(start, end, dir);
	}

	@Override
	public String toString() {
		return "Quadratic bezier";
	}

}
