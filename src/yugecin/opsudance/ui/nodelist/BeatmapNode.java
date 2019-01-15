// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.ui.Fonts;
import yugecin.opsudance.skinning.SkinService;

import static itdelatrisu.opsu.GameImage.*;
import static yugecin.opsudance.options.Options.*;

class BeatmapNode extends Node
{
	final Beatmap beatmap;

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
	void draw(Graphics g, Node focusNode)
	{
		final boolean isFocused = focusNode == this;

		button.setAlpha(0.9f);
		Color textColor = SkinService.skin.getSongSelectInactiveTextColor();

		if (isFocused) {
			super.drawButton(Color.white);
			textColor = SkinService.skin.getSongSelectActiveTextColor();
		} else {
			super.drawButton(BUTTON_BLUE);
		}

		float cx = x + Node.cx;
		float cy = y + Node.cy;

		final Grade grade = Grade.B;
		// draw grade
		if (grade != Grade.NULL) {
			Image gradeImg = grade.getMenuImage();
			gradeImg.drawCentered(cx - buttonWidth * 0.01f + gradeImg.getWidth() / 2f, y + buttonHeight / 2.2f);
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
		Fonts.BOLD.drawString(cx, cy + Fonts.MEDIUM.getLineHeight() + Fonts.DEFAULT.getLineHeight() - 6,
			beatmap.version, textColor);

		// draw stars
		// (note: in osu!, stars are also drawn for beatmap sets of size 1)
		if (beatmap.starRating < 0) {
			return;
		}
		Image star = STAR.getImage();
		float starOffset = star.getWidth() * 1.7f;
		float starX = cx + starOffset * 0.04f;
		float starY = cy + Fonts.MEDIUM.getLineHeight() + Fonts.DEFAULT.getLineHeight() * 2 - 8f * GameImage.getUIscale();
		float starCenterY = starY + star.getHeight() / 2f;
		final float baseAlpha = isFocused ? 1f : 0.8f;
		final float smallStarScale = 0.4f;
		star.setAlpha(baseAlpha);
		int i = 1;
		for (; i < beatmap.starRating && i <= 5; i++) {
			if (isFocused)
				star.drawFlash(starX + (i - 1) * starOffset, starY, star.getWidth(), star.getHeight(), textColor);
			else
				star.draw(starX + (i - 1) * starOffset, starY);
		}

		if (i <= 5) {
			float partialStarScale = smallStarScale + (float) (beatmap.starRating - i + 1) * (1f - smallStarScale);
			Image partialStar = star.getScaledCopy(partialStarScale);
			partialStar.setAlpha(baseAlpha);
			float partialStarY = starCenterY - partialStar.getHeight() / 2f;
			if (isFocused)
				partialStar.drawFlash(starX + (i - 1) * starOffset, partialStarY, partialStar.getWidth(), partialStar.getHeight(), textColor);
			else
				partialStar.draw(starX + (i - 1) * starOffset, partialStarY);
		}

		if (++i <= 5) {
			Image smallStar = star.getScaledCopy(smallStarScale);
			smallStar.setAlpha(0.5f);
			float smallStarY = starCenterY - smallStar.getHeight() / 2f;
			for (; i <= 5; i++) {
				if (isFocused)
					smallStar.drawFlash(starX + (i - 1) * starOffset, smallStarY, smallStar.getWidth(), smallStar.getHeight(), textColor);
				else
					smallStar.draw(starX + (i - 1) * starOffset, smallStarY);
			}
		}
	}
}
