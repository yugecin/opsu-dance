// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor;

import java.awt.Point;
import java.util.Iterator;

import yugecin.opsudance.options.NumericOption;
import yugecin.opsudance.options.Options;

import static yugecin.opsudance.core.InstanceContainer.*;

class CursorTrail implements Iterable<CursorTrail.Part>
{
	private static long nowtime;

	private Node first;
	private Node last;

	final Point lastPosition;

	int size;
	
	CursorTrail()
	{
		this.lastPosition = new Point();
		this.reset();
	}
	
	void reset()
	{
		this.lastPosition.move(mouseX, mouseY);
		System.out.printf("reset x %d y %d%n", mouseX, mouseY);
		this.first = this.last = null;
		this.size = 0;
	}

	void lineTo(int x, int y)
	{
		final NumericOption opt = Options.OPTION_DANCE_CURSOR_TRAIL_OVERRIDE;
		
		int fadeoff = 175;
		if (opt.val != opt.min) {
			fadeoff = (int) (1000f * opt.percentage());
		}
		nowtime = System.currentTimeMillis();

		this.addAllInbetween(lastPosition.x, lastPosition.y, mouseX, mouseY);
		lastPosition.move(mouseX, mouseY);

		int removecount = 0;
		Node newfirst = this.first;
		while (newfirst != null && newfirst.value.time < nowtime - fadeoff) {
			newfirst = newfirst.next;
			removecount++;
		}
		this.first = newfirst;
		if (newfirst == null) {
			this.last = null;
		}
		this.size -= removecount;
	}

	// from http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Java
	private void addAllInbetween(int x1, int y1, int x2, int y2)
	{
		// delta of exact value and rounded value of the dependent variable
		int d = 0;
		int dy = Math.abs(y2 - y1);
		int dx = Math.abs(x2 - x1);

		int dy2 = (dy << 1);  // slope scaling factors to avoid floating
		int dx2 = (dx << 1);  // point
		int ix = x1 < x2 ? 1 : -1;  // increment direction
		int iy = y1 < y2 ? 1 : -1;

		if (dy <= dx) {
			while (x1 != x2) {
				this.add(new Part(x1, y1));
				x1 += ix;
				d += dy2;
				if (d > dx) {
					y1 += iy;
					d -= dx2;
				}
			}
			return;
		}

		while (y1 != y2) {
			this.add(new Part(x1, y1));
			y1 += iy;
			d += dx2;
			if (d > dy) {
				x1 += ix;
				d -= dy2;
			}
		}
	}

	void add(Part t)
	{
		if (last == null) {
			last = first = new Node(t);
		} else {
			Node n = new Node(t);
			last.next = n;
			last = n;
		}
		size++;
	}

	@Override
	public Iterator<Part> iterator()
	{
		return new Iterator<Part>() {
			Node node = first;
			@Override
			public boolean hasNext()
			{
				return node != null;
			}

			@Override
			public Part next()
			{
				Part v = node.value;
				node = node.next;
				return v;
			}
		};
	}

	private static class Node
	{
		Node next;
		Part value;
		Node(Part t)
		{
			this.value = t;
		}
	}

	static class Part
	{
		int x, y;
		long time;
		Part(int x, int y)
		{
			this.x = x;
			this.y = y;
			this.time = nowtime;
		}
	}
}
