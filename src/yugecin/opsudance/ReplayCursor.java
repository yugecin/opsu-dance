/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
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
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package yugecin.opsudance;

import java.awt.Point;
import java.util.Iterator;

import org.lwjgl.opengl.*;
import org.newdawn.slick.*;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

public class ReplayCursor
{
	private final Point lastPosition;
	private TrailList trail = new TrailList();

	public Color filter;

	public ReplayCursor(Color filter) {
		this.filter = filter;
		this.lastPosition = new Point(width2, 0);
	}

	public void drawTrail(float trailw2, float trailh2, float txtw, float txth)
	{
		float alpha = 0f;
		float alphaIncrease = .4f / trail.size;
		for (Trailpart p : trail) {
			alpha += alphaIncrease;
			GL11.glColor4f(filter.r, filter.g, filter.b, alpha);
			GL11.glTexCoord2f(0f, 0f);
			GL11.glVertex3f(p.x - trailw2, p.y - trailh2, 0f);
			GL11.glTexCoord2f(txtw, 0);
			GL11.glVertex3f(p.x + trailw2, p.y - trailh2, 0f);
			GL11.glTexCoord2f(txtw, txth);
			GL11.glVertex3f(p.x + trailw2, p.y + trailh2, 0f);
			GL11.glTexCoord2f(0f, txth);
			GL11.glVertex3f(p.x - trailw2, p.y + trailh2, 0f);
		}
	}
	
	public void drawCursor()
	{
		CURSOR.getScaledImage(OPTION_CURSOR_SIZE.val / 100f).drawCentered(lastPosition.x, lastPosition.y, filter);
		CURSOR_MIDDLE.getScaledImage(OPTION_CURSOR_SIZE.val / 100f).drawCentered(lastPosition.x, lastPosition.y, filter);
	}

	/**
	 * Sets the cursor position to given point and updates trail.
	 * @param mouseX x coordinate to set position to
	 * @param mouseY y coordinate to set position to
	 */
	public void setCursorPosition(int delta, int mouseX, int mouseY) {
		nowtime = System.currentTimeMillis();

		addCursorPoints(lastPosition.x, lastPosition.y, mouseX, mouseY);
		lastPosition.move(mouseX, mouseY);

		int removecount = 0;
		TrailNode newfirst = trail.first;
		while (newfirst != null && newfirst.value.time < nowtime - 400) {
			newfirst = newfirst.next;
			removecount++;
		}
		trail.first = newfirst;
		if (newfirst == null) {
			trail.last = null;
		}
		trail.size -= removecount;
	}

	/**
	 * Adds all points between (x1, y1) and (x2, y2) to the cursor point lists.
	 * @author http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Java
	 */
	private boolean addCursorPoints(int x1, int y1, int x2, int y2) {
		int size = trail.size;
		// delta of exact value and rounded value of the dependent variable
		int d = 0;
		int dy = Math.abs(y2 - y1);
		int dx = Math.abs(x2 - x1);

		int dy2 = (dy << 1);  // slope scaling factors to avoid floating
		int dx2 = (dx << 1);  // point
		int ix = x1 < x2 ? 1 : -1;  // increment direction
		int iy = y1 < y2 ? 1 : -1;

		if (dy <= dx) {
			for (;;) {
				if (x1 == x2)
					break;
				trail.add(new Trailpart(x1, y1));
				x1 += ix;
				d += dy2;
				if (d > dx) {
					y1 += iy;
					d -= dx2;
				}
			}
		} else {
			for (;;) {
				if (y1 == y2)
					break;
				trail.add(new Trailpart(x1, y1));
				y1 += iy;
				d += dx2;
				if (d > dy) {
					x1 += ix;
					d -= dy2;
				}
			}
		}
		return trail.size != size;
	}

	private static class TrailList implements Iterable<Trailpart>
	{
		TrailNode first;
		TrailNode last;
		int size;

		public void add(Trailpart t)
		{
			if (last == null) {
				last = first = new TrailNode(t);
			} else {
				TrailNode n = new TrailNode(t);
				last.next = n;
				last = n;
			}
			size++;
		}

		@Override
		public Iterator<Trailpart> iterator()
		{
			return new Iterator<Trailpart>() {
				TrailNode node = first;
				@Override
				public boolean hasNext() {
					return node != null;
				}

				@Override
				public Trailpart next() {
					Trailpart v = node.value;
					node = node.next;
					return v;
				}
			};
		}
	}

	private static class TrailNode
	{
		TrailNode next;
		Trailpart value;
		TrailNode(Trailpart t) {
			value = t;
		}
	}

	private static class Trailpart {
		int x, y;
		long time;
		Trailpart(int x, int y) {
			this.x = x;
			this.y = y;
			this.time = nowtime;
		}
	}
}
