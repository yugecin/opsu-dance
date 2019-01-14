// Copyright 2018-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.cursor;

import yugecin.opsudance.render.TextureData;
import yugecin.opsudance.skinning.SkinService;

import static itdelatrisu.opsu.GameImage.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.utils.GLHelper.*;

public class NewestCursor implements Cursor
{
	private final CursorTrail trail;
	private final Runnable cursorSizeListener;

	private final TextureData cursorTexture, cursorMiddleTexture, cursorTrailTexture;
	private float cursorAngle;

	public NewestCursor()
	{
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

		if (OPTION_BLEND_TRAIL.state) { 
			glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		}
		final TextureData td = this.cursorTrailTexture;
		float alpha = 0f;
		float alphaIncrease = .4f / trail.size;
		glBindTexture(GL_TEXTURE_2D, td.id);
		glBegin(GL_QUADS);
		for (CursorTrail.Part p : this.trail) {
			alpha += alphaIncrease;
			glColor4f(
				((p.color >>> 16) & 0xFF) / 255f,
				((p.color >>> 8) & 0xFF) / 255f,
				(p.color & 0xFF) / 255f, alpha
			);
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
		if (!OPTION_BLEND_CURSOR.state) {
			glBlendFuncSeparate(
				GL_SRC_ALPHA,
				GL_ONE_MINUS_SRC_ALPHA,
				GL_ONE,
				GL_ONE_MINUS_SRC_ALPHA
			);
		}

		int cx = trail.lastX;
		int cy = trail.lastY;

		if (OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL.state) {
			glColor3f(1f, 1f, 1f);
		} else {
			cursorColor.bindCurrentColor();
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

		glBlendFuncSeparate(
			GL_SRC_ALPHA,
			GL_ONE_MINUS_SRC_ALPHA,
			GL_ONE,
			GL_ONE_MINUS_SRC_ALPHA
		);
	}

	@Override
	public void setCursorPosition(int x, int y)
	{
		if (!OPTION_TRAIL_COLOR_PARTS.state) {
			final int currentColor = cursorColor.getCurrentColor();
			for (CursorTrail.Part p : this.trail) {
				p.color = currentColor;
			}
		}
		cursorColor.onMovement(this.trail.lastX, this.trail.lastY, x, y);
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
	public void destroy()
	{
		this.trail.dispose();
		OPTION_CURSOR_SIZE.removeListener(this.cursorSizeListener);
	}
}
