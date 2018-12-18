// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor;

import itdelatrisu.opsu.render.Rendertarget;
import yugecin.opsudance.Dancer;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureImpl;

import static itdelatrisu.opsu.GameImage.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

public class NewestCursor implements Cursor
{
	private final Rendertarget fbo;
	private final CursorTrail trail;
	
	public NewestCursor()
	{
		this.fbo = Rendertarget.createRTTFramebuffer(width, height);
		this.trail = new CursorTrail();
	}

	@Override
	public void draw(boolean expanded)
	{
		final Color filter = Dancer.cursorColorOverride.getColor();

		final Image img = CURSOR_TRAIL.getImage();
		final Texture txt = img.getTexture();

		final float cursorsize = OPTION_CURSOR_SIZE.val / 100f;
		final float trailw2 = img.getWidth() * cursorsize / 2f;
		final float trailh2 = img.getHeight() * cursorsize / 2f;
		float txtw = txt.getWidth();
		float txth = txt.getHeight();

		// stuff copied from CurveRenderState and stuff, I don't know what I'm doing
		int oldFb = glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
		int oldTex = glGetInteger(GL_TEXTURE_BINDING_2D);
		IntBuffer oldViewport = BufferUtils.createIntBuffer(16);
		glGetInteger(GL_VIEWPORT, oldViewport);
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo.getID());
		glViewport(0, 0, fbo.width, fbo.height);
		glClearColor(0f, 0f, 0f, 0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		txt.bind();
		TextureImpl.unbind();
		glBegin(GL_QUADS);

		float alpha = 0f;
		float alphaIncrease = .4f / trail.size;
		for (CursorTrail.Part p : this.trail) {
			alpha += alphaIncrease;
			glColor4f(filter.r, filter.g, filter.b, alpha);
			glTexCoord2f(0f, 0f);
			glVertex3f(p.x - trailw2, p.y - trailh2, 0f);
			glTexCoord2f(txtw, 0);
			glVertex3f(p.x + trailw2, p.y - trailh2, 0f);
			glTexCoord2f(txtw, txth);
			glVertex3f(p.x + trailw2, p.y + trailh2, 0f);
			glTexCoord2f(0f, txth);
			glVertex3f(p.x - trailw2, p.y + trailh2, 0f);
		}

		glEnd();

		glBindTexture(GL_TEXTURE_2D, oldTex);
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, oldFb);
		glViewport(oldViewport.get(0), oldViewport.get(1), oldViewport.get(2), oldViewport.get(3));

		glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		//glEnable(GL_TEXTURE_2D);
		//glDisable(GL_TEXTURE_1D);
		glBindTexture(GL_TEXTURE_2D, fbo.getTextureID());
		glBegin(GL_QUADS);
		glColor4f(1f, 1f, 1f, 1f);
		glTexCoord2f(1f, 1f);
		glVertex2i(fbo.width, 0);
		glTexCoord2f(0f, 1f);
		glVertex2i(0, 0);
		glTexCoord2f(0f, 0f);
		glVertex2i(0, fbo.height);
		glTexCoord2f(1f, 0f);
		glVertex2i(fbo.width, fbo.height);
		glEnd();
		glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		
		int cx = trail.lastPosition.x;
		int cy = trail.lastPosition.y;

		CURSOR.getScaledImage(cursorsize).drawCentered(cx, cy, filter);
		CURSOR_MIDDLE.getScaledImage(cursorsize).drawCentered(cx, cy, filter);
	}

	@Override
	public void setCursorPosition(int x, int y)
	{
		this.trail.lineTo(x, y);
	}

	@Override
	public void reset()
	{
		this.trail.reset();
	}

	@Override
	public boolean isBeatmapSkinned()
	{
		// TODO(?)
		return false;
	}

	@Override
	public void updateAngle()
	{
		// TODO
	}

	@Override
	public void destroy()
	{
		this.fbo.destroyRTT();
	}
}
