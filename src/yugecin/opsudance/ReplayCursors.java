/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2018 yugecin
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

package yugecin.opsudance;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.Texture;

import itdelatrisu.opsu.render.Rendertarget;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

import java.nio.IntBuffer;

public class ReplayCursors
{
	public final ReplayPlayback[] playbacks;

	private final Rendertarget fbo;

	public ReplayCursors(int amount)
	{
		this.playbacks = new ReplayPlayback[amount];
		this.fbo = Rendertarget.createRTTFramebuffer(width, height);
	}

	public void draw()
	{
		final Image img = CURSOR_TRAIL.getImage();
		final Texture txt = img.getTexture();

		final float trailw2 = img.getWidth() * OPTION_CURSOR_SIZE.val / 100f / 2f;
		final float trailh2 = img.getHeight() * OPTION_CURSOR_SIZE.val / 100f / 2f;
		float txtw = txt.getWidth();
		float txth = txt.getHeight();

		for (ReplayPlayback p : playbacks) {
			if (!p.shouldDrawCursor()) {
				continue;
			}
			// stuff copied from CurveRenderState and stuff, I don't know what I'm doing
			int oldFb = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
			int oldTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			IntBuffer oldViewport = BufferUtils.createIntBuffer(16);
			GL11.glGetInteger(GL11.GL_VIEWPORT, oldViewport);
			EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo.getID());
			GL11.glViewport(0, 0, fbo.width, fbo.height);
			GL11.glClearColor(0f, 0f, 0f, 0f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

			txt.bind();
			GL11.glBegin(GL11.GL_QUADS);
			p.cursor.drawTrail(trailw2, trailh2, txtw, txth);
			GL11.glEnd();

			GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex);
			EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFb);
			GL11.glViewport(oldViewport.get(0), oldViewport.get(1), oldViewport.get(2), oldViewport.get(3));

			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			//GL11.glEnable(GL11.GL_TEXTURE_2D);
			//GL11.glDisable(GL11.GL_TEXTURE_1D);
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

			// something have to be drawn or things are broken... yeah...
			CURSOR.getImage().draw(width, height);
		}

		for (ReplayPlayback p : playbacks) {
			if (!p.shouldDrawCursor()) {
				continue;
			}
			p.cursor.drawCursor();
		}
	}

	public void destroy()
	{
		this.fbo.destroyRTT();
	}
}
