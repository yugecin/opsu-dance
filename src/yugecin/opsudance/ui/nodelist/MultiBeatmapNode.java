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

	MultiBeatmapNode(Beatmap[] beatmaps)
	{
		this.beatmaps = beatmaps;
	}

	@Override
	BeatmapNode attemptFocusMap(Beatmap beatmap)
	{
		for (int i = this.beatmaps.length; i > 0;) {
			if (beatmaps[--i] == beatmap) {
				final BeatmapNode[] newnodes = this.expand();
				nodeList.replace(this, newnodes);
				return newnodes[i];
			}
		}
		return null;
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
		return nodes;
	}

	@Override
	void draw(Graphics g, Node focusNode)
	{
		final boolean isFocused = focusNode == this;

		button.setAlpha(0.95f);
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
		super.drawButton(buttonColor);

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
