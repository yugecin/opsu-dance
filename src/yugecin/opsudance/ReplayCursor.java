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
import java.nio.IntBuffer;
import java.util.Iterator;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.Texture;

import itdelatrisu.opsu.render.Rendertarget;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

public class ReplayCursor
{
	private final Point lastPosition;
	private TrailList trail = new TrailList();

	public Color filter;

	private Rendertarget fbo;

	private static long nowtime;

	public ReplayCursor(Color filter) {
		this.filter = filter;
		this.lastPosition = new Point(width2, 0);
		this.fbo = Rendertarget.createRTTFramebuffer(width, height);
	}

	public void draw()
	{
		nowtime = System.currentTimeMillis();

		// stuff copied from CurveRenderState and stuff, I don't know what I'm doing
		int oldFb = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
		int oldTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		//glGetInteger requires a buffer of size 16, even though just 4
		//values are returned in this specific case
		IntBuffer oldViewport = BufferUtils.createIntBuffer(16);
		GL11.glGetInteger(GL11.GL_VIEWPORT, oldViewport);
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo.getID());
		GL11.glViewport(0, 0, fbo.width, fbo.height);
		// render
		GL11.glClearColor(0f, 0f, 0f, 0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		final Image img = CURSOR_TRAIL.getImage();
		final Texture txt = img.getTexture();
		Color.white.bind();
		txt.bind();

		float alpha = 0f;
		float alphaIncrease = .5f / trail.size;
		float trailwidth2 = img.getWidth() * OPTION_CURSOR_SIZE.val / 100f / 2f;
		float trailheight2 = img.getHeight() * OPTION_CURSOR_SIZE.val / 100f / 2f;
		float txtwidth = txt.getWidth();
		float txtheight = txt.getHeight();
		GL11.glBegin(GL11.GL_QUADS);
		final Point lastpoint = new Point(-1, -1);
		for (Trailpart p : trail) {
			alpha += alphaIncrease;
			GL11.glColor4f(filter.r, filter.g, filter.b, alpha);
			GL11.glTexCoord2f(0f, 0f);
			GL11.glVertex3f(p.x - trailwidth2, p.y - trailheight2, 0f);
			GL11.glTexCoord2f(txtwidth, 0);
			GL11.glVertex3f(p.x + trailwidth2, p.y - trailheight2, 0f);
			GL11.glTexCoord2f(txtwidth, txtheight);
			GL11.glVertex3f(p.x + trailwidth2, p.y + trailheight2, 0f);
			GL11.glTexCoord2f(0f, txtheight);
			GL11.glVertex3f(p.x - trailwidth2, p.y + trailheight2, 0f);
			lastpoint.x = p.x;
			lastpoint.y = p.y;
		}
		GL11.glEnd();

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFb);
		GL11.glViewport(oldViewport.get(0), oldViewport.get(1), oldViewport.get(2), oldViewport.get(3));

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_TEXTURE_1D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.getTextureID());
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glTexCoord2f(1f, 1f);
		GL11.glVertex2i(fbo.width, 0);
		GL11.glTexCoord2f(0f, 1f);
		GL11.glVertex2i(0, 0);
		GL11.glTexCoord2f(0f, 0f);
		GL11.glVertex2i(0, fbo.height);
		GL11.glTexCoord2f(1f, 0f);
		GL11.glVertex2i(fbo.width, fbo.height);
		GL11.glEnd();
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

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

	public void destroy()
	{
		this.fbo.destroyRTT();
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
