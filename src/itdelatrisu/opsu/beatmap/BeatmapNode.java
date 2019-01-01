// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package itdelatrisu.opsu.beatmap;

import itdelatrisu.opsu.GameData.Grade;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

import yugecin.opsudance.core.Nullable;
import yugecin.opsudance.skinning.SkinService;

import static yugecin.opsudance.options.Options.*;

/**
 * Node in an {@link BeatmapList} representing a single beatmap.
 * Child of a {@link BeatmapSetNode}
 * Note that {@link BeatmapSetNode} extends this class
 */
public class BeatmapNode
{
	public final BeatmapSetNode setNode;
	public final BeatmapSet beatmapSet;
	public final Beatmap beatmap;
	public BeatmapNode prev, next;

	/** index of this node in the list */
	public int index;
	/**
	 * index of this node in setNode
	 * {@code -1} if this is a set node and has multiple maps)
	 */
	public int beatmapIndex;

	BeatmapNode(BeatmapSetNode setNode, Beatmap beatmap, int beatmapIndex)
	{
		this.setNode = setNode;
		this.beatmap = beatmap;
		this.beatmapSet = setNode.beatmapSet;
		this.beatmapIndex = beatmapIndex;
	}

	protected BeatmapNode(BeatmapSet beatmapSet)
	{
		this.setNode = (BeatmapSetNode) this;
		this.beatmap = null;
		this.beatmapSet = beatmapSet;
		this.beatmapIndex = -1;
	}

	/**
	 * Returns the node a given number of positions forward or backwards.
	 * @param node the starting node
	 * @param offset the number of nodes to shift forwards (+) or backwards (-).
	 * @return the node at the requested position, or {@code null}
	 */
	@Nullable
	public BeatmapNode getRelativeNode(int offset)
	{
		BeatmapNode node = this;
		if (offset > 0) {
			do {
				node = node.next;
			} while (--offset > 0 && node != null);
		} else {
			while (offset++ < 0 && node != null) {
				node = node.prev;
			}
		}
		return node;
	}

	/**
	 * Draws the button.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param grade the highest grade, if any
	 * @param focus true if this is the focused node
	 */
	public void draw(float x, float y, Grade grade, boolean focus) {
		Image bg = GameImage.MENU_BUTTON_BG.getImage();
		bg.setAlpha(0.9f);
		Color bgColor;
		Color textColor = SkinService.skin.getSongSelectInactiveTextColor();

		x -= bg.getWidth() / 10f;
		if (focus) {
			bgColor = Color.white;
			textColor = SkinService.skin.getSongSelectActiveTextColor();
		} else {
			bgColor = Colors.BLUE_BUTTON;
		}
		bg.draw(x, y, bgColor);

		float cx = x + (bg.getWidth() * 0.043f);
		float cy = y + (bg.getHeight() * 0.18f) - 3;

		// draw grade
		if (grade != Grade.NULL) {
			Image gradeImg = grade.getMenuImage();
			gradeImg.drawCentered(cx - bg.getWidth() * 0.01f + gradeImg.getWidth() / 2f, y + bg.getHeight() / 2.2f);
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
		Image star = GameImage.STAR.getImage();
		float starOffset = star.getWidth() * 1.7f;
		float starX = cx + starOffset * 0.04f;
		float starY = cy + Fonts.MEDIUM.getLineHeight() + Fonts.DEFAULT.getLineHeight() * 2 - 8f * GameImage.getUIscale();
		float starCenterY = starY + star.getHeight() / 2f;
		final float baseAlpha = focus ? 1f : 0.8f;
		final float smallStarScale = 0.4f;
		star.setAlpha(baseAlpha);
		int i = 1;
		for (; i < beatmap.starRating && i <= 5; i++) {
			if (focus)
				star.drawFlash(starX + (i - 1) * starOffset, starY, star.getWidth(), star.getHeight(), textColor);
			else
				star.draw(starX + (i - 1) * starOffset, starY);
		}

		if (i <= 5) {
			float partialStarScale = smallStarScale + (float) (beatmap.starRating - i + 1) * (1f - smallStarScale);
			Image partialStar = star.getScaledCopy(partialStarScale);
			partialStar.setAlpha(baseAlpha);
			float partialStarY = starCenterY - partialStar.getHeight() / 2f;
			if (focus)
				partialStar.drawFlash(starX + (i - 1) * starOffset, partialStarY, partialStar.getWidth(), partialStar.getHeight(), textColor);
			else
				partialStar.draw(starX + (i - 1) * starOffset, partialStarY);
		}

		if (++i <= 5) {
			Image smallStar = star.getScaledCopy(smallStarScale);
			smallStar.setAlpha(0.5f);
			float smallStarY = starCenterY - smallStar.getHeight() / 2f;
			for (; i <= 5; i++) {
				if (focus)
					smallStar.drawFlash(starX + (i - 1) * starOffset, smallStarY, smallStar.getWidth(), smallStar.getHeight(), textColor);
				else
					smallStar.draw(starX + (i - 1) * starOffset, smallStarY);
			}
		}
	}
}