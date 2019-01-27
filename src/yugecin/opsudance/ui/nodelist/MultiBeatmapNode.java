// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSet;
import itdelatrisu.opsu.ui.Fonts;
import yugecin.opsudance.skinning.SkinService;

import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * Node that holds 2 or more beatmaps of the same set
 */
class MultiBeatmapNode extends Node
{
	final Beatmap[] beatmaps;

	/**
	 * If this node is the result of collapsing set nodes,
	 * this was the combined height of the set nodes.
	 * To be eased in {@link #getHeight()}
	 */
	float fromHeight;

	MultiBeatmapNode(Beatmap[] beatmaps)
	{
		this.beatmaps = beatmaps;
	}

	@Override
	BeatmapNode attemptFocusMap(Beatmap beatmap)
	{
		for (int i = this.beatmaps.length; i > 0;) {
			if (beatmaps[--i] == beatmap) {
				nodeList.unexpandAll();
				return this.expand()[i];
			}
		}
		return null;
	}

	@Override
	float getHeight()
	{
		if (appearTime < APPEAR_TIME) {
			return buttonOffset + (fromHeight - buttonOffset) * (1f - appearValue);
		}
		return buttonOffset;
	}

	@Override
	protected boolean belongsToSet(BeatmapSet focusedSet)
	{
		return this.beatmaps[0].beatmapSet == focusedSet;
	}

	BeatmapNode[] expand()
	{
		final BeatmapNode[] nodes = new BeatmapNode[this.beatmaps.length];
		for (int i = 0; i < nodes.length; i++) {
			final BeatmapNode n = new BeatmapNode(this.beatmaps[i]);
			n.isFromExpandedMultiNode = true;
			nodes[i] = n;
		}
		nodeList.replace(this, nodes);
		return nodes;
	}

	@Override
	void draw(Graphics g, Node focusNode)
	{
		final boolean isFocused = focusNode == this;

		Color textColor = SkinService.skin.getSongSelectInactiveTextColor();

		final Color buttonColor;
		if (isFocused) {
			buttonColor = Color.white;
			textColor = SkinService.skin.getSongSelectActiveTextColor();
		} else if (this.beatmaps[0].beatmapSet.isPlayed()) {
			buttonColor = BUTTON_ORANGE;
		} else {
			buttonColor = BUTTON_PINK;
		}
		final float oldalpha = buttonColor.a;
		buttonColor.a = 0.9f;
		super.drawButton(buttonColor);
		buttonColor.a = oldalpha;

		float cx = x + Node.cx;
		float cy = y + Node.cy;

		final Beatmap bm = this.beatmaps[0];

		// draw text
		if (OPTION_SHOW_UNICODE.state) {
			Fonts.loadGlyphs(Fonts.MEDIUM, bm.titleUnicode);
			Fonts.loadGlyphs(Fonts.DEFAULT, bm.artistUnicode);
		}
		Fonts.MEDIUM.drawString(cx, cy, bm.getTitle(), textColor);
		Fonts.DEFAULT.drawString(cx, cy + Fonts.MEDIUM.getLineHeight() - 3,
				String.format("%s // %s", bm.getArtist(), bm.creator), textColor);
	}
}
