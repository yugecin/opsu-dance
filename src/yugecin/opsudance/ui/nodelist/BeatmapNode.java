// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSet;
import itdelatrisu.opsu.ui.Fonts;
import yugecin.opsudance.render.TextureData;
import yugecin.opsudance.skinning.SkinService;

import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.core.InstanceContainer.nodeList;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.utils.GLHelper.*;

class BeatmapNode extends Node
{
	static TextureData starTexture;
	static float starSpacing;
	static float starYoffset;

	static void revalidate()
	{
		starTexture = new TextureData(GameImage.STAR);
		starSpacing = starTexture.width * 1.225f;
		starYoffset =
			Fonts.MEDIUM.getLineHeight() + Fonts.DEFAULT.getLineHeight() * 2f
			- GameImage.getUIscale() * 8f;
	}

	final Beatmap beatmap;
	boolean isFromExpandedMultiNode;

	boolean setFocused;
	float focusedHeight;
	float focusedInternalOffset;
	boolean doFade;

	BeatmapNode(Beatmap beatmap)
	{
		this.beatmap = beatmap;
	}

	@Override
	BeatmapNode attemptFocusMap(Beatmap beatmap)
	{
		if (this.beatmap == beatmap) {
			return this;
		}
		return null;
	}

	@Override
	void focusChanged(BeatmapSet focusedSet)
	{
		this.setFocused = this.belongsToSet(focusedSet); // same check is done twice :/
		super.focusChanged(focusedSet);
	}

	@Override
	void onSiblingNodeUpdated()
	{
		this.focusedHeight = buttonOffset + buttonInternalOffset;
		this.focusedInternalOffset = 0f;
		if (this.idx == 0) {
			return;
		}
		Node prevNode = nodeList.nodes[this.idx - 1];
		if (!(prevNode instanceof BeatmapNode) ||
			!((BeatmapNode) prevNode).isFromExpandedMultiNode)
		{
			this.focusedHeight += buttonInternalOffset;
			this.focusedInternalOffset = buttonInternalOffset;
		}
	}

	@Override
	float getHeight()
	{
		if (this.setFocused) {
			return this.focusedHeight * this.appearValue;
		}
		return buttonOffset;
	}

	@Override
	float getInternalOffset()
	{
		if (this.setFocused) {
			return this.focusedInternalOffset * this.appearValue;
		}
		return 0f;
	}

	@Override
	protected boolean belongsToSet(BeatmapSet focusedSet)
	{
		return this.beatmap.beatmapSet == focusedSet;
	}

	@Override
	void draw(Graphics g, Node focusNode)
	{
		final boolean isFocused = focusNode == this;

		float starProgress = 1f;
		float appearProgress = 1f;
		if (this.appearTime < APPEAR_TIME) {
			starProgress = OUT_QUART.calc(this.appearValue);
			if (this.doFade) {
				appearProgress = this.appearValue * 2f;
				if (appearProgress > 1f) {
					appearProgress = 1f;
				} else {
					appearProgress = OUT_QUART.calc(starProgress);
				}
			}
		}

		button.setAlpha(0.9f);
		Color textColor = SkinService.skin.getSongSelectInactiveTextColor();

		final Color buttonColor;
		if (isFocused) {
			buttonColor = Color.white;
			textColor = SkinService.skin.getSongSelectActiveTextColor();
		} else if (this.isFromExpandedMultiNode || this.setFocused) {
			buttonColor = BUTTON_BLUE;
		} else if (this.beatmap.beatmapSet.isPlayed()) {
			buttonColor = BUTTON_ORANGE;
		} else {
			buttonColor = BUTTON_PINK;
		}

		float prevAlpha = buttonColor.a;
		Color gradeStarCol = new Color(1f, 1f, appearProgress);
		buttonColor.a *= appearProgress;
		textColor = new Color(textColor);
		textColor.a *= appearProgress;
		super.drawButton(buttonColor);
		buttonColor.a = prevAlpha;

		float cx = x + Node.cx;
		float cy = y + Node.cy;

		final Grade grade = Grade.B;
		// draw grade
		if (grade != Grade.NULL) {
			Image gradeImg = grade.getMenuImage();
			gradeImg.drawCentered(
				cx - buttonWidth * 0.01f + gradeImg.getWidth() / 2f,
				y + buttonHeight / 2.2f,
				gradeStarCol
			);
			cx += gradeImg.getWidth();
		}

		// draw text
		if (OPTION_SHOW_UNICODE.state) {
			Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.titleUnicode);
			Fonts.loadGlyphs(Fonts.DEFAULT, beatmap.artistUnicode);
		}
		Fonts.MEDIUM.drawString(cx, cy, beatmap.getTitle(), textColor);
		Fonts.DEFAULT.drawString(cx, cy + Fonts.MEDIUM.getLineHeight() - 3,
				String.format("%s // %s", beatmap.getArtist(), beatmap.creator), textColor);
		// difficulty is also faded in, but don't care at this point
		Fonts.BOLD.drawString(cx, cy + Fonts.MEDIUM.getLineHeight() + Fonts.DEFAULT.getLineHeight() - 6,
			beatmap.version, textColor);

		// draw stars
		if (beatmap.starRating < 0) {
			return;
		}

		glPushMatrix();
		glTranslatef(cx, cy + starYoffset, 0f);
		final float stars = (float) beatmap.starRating * starProgress;
		int fullStars = (int) Math.floor(stars);
		glBindTexture(GL_TEXTURE_2D, starTexture.id);
		for (int i = 0; i < fullStars; i++) {
			simpleTexturedQuadTopLeft(starTexture);
			glTranslatef(starSpacing, 0f, 0f);
			if (i >= 10) {
				glPopMatrix();
				return;
			}
		}
		float starPercent = (float) stars - fullStars;
		if (starPercent > 0f) {
			glBegin(GL_QUADS);
			glTexCoord2f(0f, 0f);
			glVertex2f(0f, 0f);
			glTexCoord2f(starTexture.txtw * starPercent, 0f);
			glVertex2f(starTexture.width * starPercent, 0f);
			glTexCoord2f(starTexture.txtw * starPercent, starTexture.txth);
			glVertex2f(starTexture.width * starPercent, starTexture.height);
			glTexCoord2f(0f, starTexture.txth);
			glVertex2f(0f, starTexture.height);
			glEnd();
			glColor4f(1f, 1f, 1f, .2f);
			glBegin(GL_QUADS);
			glTexCoord2f(starPercent * starTexture.txtw, 0f);
			glVertex2f(starPercent * starTexture.width, 0f);
			glTexCoord2f(starTexture.txtw, 0f);
			glVertex2f(starTexture.width, 0f);
			glTexCoord2f(starTexture.txtw, starTexture.txth);
			glVertex2f(starTexture.width, starTexture.height);
			glTexCoord2f(starPercent * starTexture.txtw, starTexture.txth);
			glVertex2f(starPercent * starTexture.width, starTexture.height);
			glEnd();
			glTranslatef(starSpacing, 0f, 0f);
			fullStars++;
		} else {
			glColor4f(1f, 1f, 1f, .2f);
		}
		for (int i = fullStars; i < 10; i++) {
			simpleTexturedQuadTopLeft(starTexture);
			glTranslatef(starSpacing, 0f, 0f);
		}
		glPopMatrix();
	}
}
