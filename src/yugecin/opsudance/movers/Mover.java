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

public abstract class Mover {

	protected int dir;

	private int startT;
	private int totalT;

	protected double startX;
	protected double startY;

	protected double endX;
	protected double endY;

	public Mover(GameObject start, GameObject end, int dir) {
		this.dir = dir;
		this.startX = start.end.x;
		this.startY = start.end.y;
		this.endX = end.start.x;
		this.endY = end.start.y;
		this.startT = start.getEndTime();
		this.totalT = end.getTime() - startT;
	}

	protected final double getT(int time) {
		return ((double)time - startT) / totalT;
	}

	public abstract double[] getPointAt(int time);
	public abstract String getName();

}
