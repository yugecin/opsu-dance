// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.opengl.Texture;

import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSet;
import itdelatrisu.opsu.ui.Fonts;
import yugecin.opsudance.render.TextureData;
import yugecin.opsudance.skinning.SkinService;

import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static itdelatrisu.opsu.Utils.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.core.InstanceContainer.nodeList;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.utils.GLHelper.*;

import java.awt.Font;

class BeatmapNode extends Node
{
	static TextureData starTexture;
	static float starXoffset, starYoffset;
	static float titleYoffset, authorYoffset, versionYoffset;

	static UnicodeFont titlefont, artistfont, versionfont;

	static void revalidate()
	{
		try {
			loadFont();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		starTexture = new TextureData(GameImage.STAR);
		starXoffset = 0f;
		titleYoffset = hitboxYtop + hitboxHeight * 0.088f - titlefont.getDescent();
		authorYoffset = hitboxYtop + hitboxHeight * 0.310f - artistfont.getDescent();
		versionYoffset = hitboxYtop + hitboxHeight * 0.510f - versionfont.getDescent();
		starYoffset = hitboxYtop + hitboxHeight * 0.600f;
		calcStarDimensions();
	}

	@SuppressWarnings("unchecked")
	private static void loadFont() throws SlickException
	{
		if (titlefont != null) {
			titlefont.destroy();
		}
		if (artistfont != null) {
			artistfont.destroy();
		}
		if (versionfont != null) {
			versionfont.destroy();
		}
		final Font deffont = Fonts.DEFAULT.getFont();
		titlefont = new UnicodeFont(deffont.deriveFont(hitboxHeight * 0.17f * 1.3f));
		titlefont.addAsciiGlyphs();
		titlefont.getEffects().add(new ColorEffect());
		titlefont.loadGlyphs();
		final float xx = 0.14f * 1.3f;
		artistfont = new UnicodeFont(deffont.deriveFont(hitboxHeight * xx));
		artistfont.addAsciiGlyphs();
		artistfont.getEffects().add(new ColorEffect());
		artistfont.loadGlyphs();
		versionfont = new UnicodeFont(deffont.deriveFont(Font.BOLD, hitboxHeight * xx));
		versionfont.addAsciiGlyphs();
		versionfont.getEffects().add(new ColorEffect());
		versionfont.loadGlyphs();
	}

	static void calcStarDimensions()
	{
		// inefficient way to get star dimensions minus transparency
		final Texture t = GameImage.STAR.getImage().getTexture();
		if (!t.hasAlpha()) {
			return;
		}

		final byte[] d = t.getTextureData();
		final int w = t.getImageWidth();
		final int h = t.getImageHeight();
		final int _w = t.getTextureHeight();
		final int _h = t.getTextureWidth();

		int minheight = h, maxheight = 0, left = w;
		for (int i = 0, x = 3; i < _h; i++) {
			for (int j = 0; j < _w; j++, x += 4) {
				if (i > h || j > w) {
					continue;
				}
				int v = d[x];
				if (v < 0) {
					v += 256;
				}
				if (v > 30) {
					minheight = minheight < i ? minheight : i;
					maxheight = maxheight > i ? maxheight : i;
					left = left < j ? left : j;
				}
			}
		}
		if (minheight > h / 2) minheight = 0;
		if (maxheight < h / 2) maxheight = h;
		if (left > w / 2) left = 0;
		starTexture.width = w;
		starTexture.height = h;
		final float desiredSize = hitboxHeight * 0.26f;
		final float visibleHeight = (maxheight - minheight);
		final float yo = .5f - visibleHeight / starTexture.height / 2f;
		final float xo = left / starTexture.width;
		final float scale = starTexture.height / visibleHeight;
		final float ratio = starTexture.width / starTexture.height;
		starTexture.height = desiredSize * scale;
		starTexture.width = starTexture.height * ratio;
		starTexture.height2 = starTexture.height / 2f;
		starTexture.width2 = starTexture.width / 2f;
		starYoffset += yo * starTexture.height;
		starXoffset -= xo * starTexture.width;
	}

	private float normalHeight = buttonOffset, normalOffset;

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
		this.normalHeight = buttonOffset;
		this.normalOffset = 0f;
		this.focusedHeight = buttonOffset + buttonInternalOffset;
		this.focusedInternalOffset = 0f;
		Node prevNode;
		if (this.idx == 0 ||
			!((prevNode = nodeList.nodes[this.idx - 1]) instanceof BeatmapNode) ||
			!((BeatmapNode) prevNode).isFromExpandedMultiNode)
		{
			this.focusedHeight += buttonInternalOffset;
			this.focusedInternalOffset = buttonInternalOffset;
		}
		if (this.idx == 0) {
			// TODO why is this needed? (see NodeList#scrollMakeNodeVisible)
			this.normalHeight = buttonOffset + buttonInternalOffset;
			this.normalOffset = buttonInternalOffset;
		}
	}

	@Override
	float getHeight()
	{
		if (this.setFocused) {
			return this.focusedHeight * this.appearValue;
		}
		return this.normalHeight;
	}

	@Override
	float getEventualHeight()
	{
		if (this.setFocused) {
			return this.focusedHeight;
		}
		return this.normalHeight;
	}

	@Override
	float getInternalOffset()
	{
		if (this.setFocused) {
			return this.focusedInternalOffset * this.appearValue;
		}
		return this.normalOffset;
	}

	@Override
	protected boolean belongsToSet(BeatmapSet focusedSet)
	{
		return this.beatmap.beatmapSet == focusedSet;
	}

	@Override
	void draw(Graphics g, Node focusNode, Node selectedNode)
	{
		final boolean isFocused = focusNode == this;

		float starProgress = 1f;
		float fade = 1f;
		if (this.appearTime < APPEAR_TIME) {
			starProgress = (clamp(this.appearValue, .5f, 1f) - .5f) * 2f;
			if (this.doFade) {
				fade = OUT_CUBIC.calc((float) this.appearTime / APPEAR_TIME);
			}
		}

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
		Color gradeStarCol = new Color(1f, 1f, 1f, fade);
		buttonColor.a = 0.9f * fade;
		textColor = new Color(textColor);
		textColor.a *= fade;
		super.drawButton(buttonColor, selectedNode == this);
		buttonColor.a = prevAlpha;

		float cx = x + Node.cx;

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
			Fonts.loadGlyphs(titlefont, beatmap.titleUnicode);
			Fonts.loadGlyphs(artistfont, beatmap.artistUnicode);
		}
		prevAlpha = textColor.a;
		if (this.setFocused && focusNode != this) {
			textColor.a *= 0.2f;
		}
		titlefont.drawString(cx, y + titleYoffset, beatmap.getTitle(), textColor);
		final String author = beatmap.getArtist() + " // " + beatmap.creator;
		artistfont.drawString(cx, y + authorYoffset, author, textColor);
		textColor.a = prevAlpha;
		// difficulty is also faded in, but don't care at this point
		versionfont.drawString(cx, y + versionYoffset, beatmap.version, textColor);

		// draw stars
		if (beatmap.starRating < 0) {
			return;
		}

		glPushMatrix();
		glTranslatef(cx, y + starYoffset, 0f);
		final float stars = (float) beatmap.starRating * starProgress;
		int fullStars = (int) Math.floor(stars);
		glColor3f(1f, 1f, 1f);
		glBindTexture(GL_TEXTURE_2D, starTexture.id);
		for (int i = 0; i < fullStars; i++) {
			simpleTexturedQuadTopLeft(starTexture);
			glTranslatef(starTexture.width, 0f, 0f);
			if (i >= 10) {
				glPopMatrix();
				return;
			}
		}
		float starPercent = stars - fullStars;
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
			glTranslatef(starTexture.width, 0f, 0f);
			fullStars++;
		} else {
			glColor4f(1f, 1f, 1f, .2f);
		}
		for (int i = fullStars; i < 10; i++) {
			simpleTexturedQuadTopLeft(starTexture);
			glTranslatef(starTexture.width, 0f, 0f);
		}
		glPopMatrix();
	}
}
