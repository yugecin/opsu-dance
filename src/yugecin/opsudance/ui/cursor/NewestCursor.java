// Copyright 2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor;

import itdelatrisu.opsu.render.Rendertarget;
import yugecin.opsudance.Dancer;
import yugecin.opsudance.render.TextureData;
import yugecin.opsudance.skinning.SkinService;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.newdawn.slick.Color;

import static itdelatrisu.opsu.GameImage.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.utils.GLHelper.*;

public class NewestCursor implements Cursor
{
	private final Rendertarget fbo;
	private final CursorTrail trail;
	private final Runnable cursorSizeListener;

	private final TextureData cursorTexture, cursorMiddleTexture, cursorTrailTexture;
	private float cursorAngle;

	public NewestCursor()
	{
		this.fbo = Rendertarget.createRTTFramebuffer(width, height);
		this.trail = new CursorTrail();

		this.cursorTexture = new TextureData(CURSOR);
		this.cursorMiddleTexture = new TextureData(CURSOR_MIDDLE);
		this.cursorTrailTexture = new TextureData(CURSOR_TRAIL);

		this.cursorSizeListener = this::onCursorSizeOptionChanged;
		OPTION_CURSOR_SIZE.addListener(this.cursorSizeListener);
		this.onCursorSizeOptionChanged();
	}

	private void onCursorSizeOptionChanged()
	{
		final float scale = OPTION_CURSOR_SIZE.val / 100f;
		this.cursorTexture.useScale(scale);
		// middle is not scaled apparently?
		this.cursorTrailTexture.useScale(scale);
	}

	@Override
	public void draw(boolean expanded)
	{
		if (OPTION_DISABLE_CURSOR.state) {
			return;
		}

		this.cursorAngle = (this.cursorAngle + renderDelta / 40f) % 360f;

		// stuff copied from CurveRenderState and stuff, I don't know what I'm doing
		int oldFb = glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
		int oldTex = glGetInteger(GL_TEXTURE_BINDING_2D);
		IntBuffer oldViewport = BufferUtils.createIntBuffer(16);
		glGetInteger(GL_VIEWPORT, oldViewport);
		EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, fbo.getID());
		glViewport(0, 0, fbo.width, fbo.height);
		glClearColor(0f, 0f, 0f, 0f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		final TextureData td = this.cursorTrailTexture;
		float alpha = 0f;
		float alphaIncrease = .4f / trail.size;
		glBindTexture(GL_TEXTURE_2D, td.id);
		glBegin(GL_QUADS);
		for (CursorTrail.Part p : this.trail) {
			alpha += alphaIncrease;
			glColor4f(p.color.r, p.color.g, p.color.b, alpha);
			glTexCoord2f(0f, 0f);
			glVertex2f(p.x + -td.width2, p.y + -td.height2);
			glTexCoord2f(td.txtw, 0);
			glVertex2f(p.x +td.width2, p.y + -td.height2);
			glTexCoord2f(td.txtw, td.txth);
			glVertex2f(p.x +td.width2, p.y + td.height2);
			glTexCoord2f(0f, td.txth);
			glVertex2f(p.x +-td.width2, p.y + td.height2);
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

		if (!OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL.state) {
			Dancer.cursorColorOverride.getColor().bind();
		}

		glPushMatrix();
		glTranslatef(cx, cy, 0.0f);

		// cursor
		if (SkinService.skin.isCursorRotated()) {
			glPushMatrix();
			glRotatef(this.cursorAngle, 0.0f, 0.0f, 1.0f);
		}
		simpleTexturedQuad(cursorTexture);
		if (SkinService.skin.isCursorRotated()) {
			glPopMatrix();
		}
		// cursormiddle
		simpleTexturedQuad(cursorMiddleTexture);

		glPopMatrix();
	}

	@Override
	public void setCursorPosition(int x, int y)
	{
		final Color color = Dancer.cursorColorOverride.getColor();
		if (!OPTION_TRAIL_COLOR_PARTS.state) {
			for (CursorTrail.Part p : this.trail) {
				p.color = color;
			}
		}
		this.trail.lineTo(x, y, color);
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
	public void destroy()
	{
		this.fbo.destroyRTT();
		this.trail.dispose();
		OPTION_CURSOR_SIZE.removeListener(this.cursorSizeListener);
	}
}
