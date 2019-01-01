// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

import itdelatrisu.opsu.beatmap.Beatmap;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.IN_QUAD;

abstract class Node
{
	static Image button;
	static int buttonWidth, buttonHeight;

	protected static float cx, cy;

	private static int hitboxYtop, hitboxYbot, hitboxXleft;

	static void revalidate()
	{
		button =  MENU_BUTTON_BG.getImage();
		buttonWidth = button.getWidth();
		buttonHeight = button.getHeight();
		cx = buttonWidth * 0.043f;
		cy = buttonHeight * 0.18f - 3f;

		// button hitboxes, because they usually have lots of transparent margin
		final int midx = buttonWidth / 2;
		for (int y = 0; y < buttonHeight / 2; y++) {
			if (button.getAlphaAt(midx, y) > .7) {
				hitboxYtop = y;
				break;
			}
		}
		if (hitboxYtop > buttonHeight / 2) {
			hitboxYtop = 0;
		}
		for (int y = buttonHeight - 1; y > buttonHeight / 2; y--) {
			if (button.getAlphaAt(midx, y) > .7) {
				hitboxYbot = y;
				break;
			}
		}
		if (hitboxYbot < buttonHeight / 2) {
			hitboxYbot = buttonHeight - 1;
		}
		final int midy = buttonHeight * (hitboxYtop + hitboxYbot) / 2;
		for (int x = 0; x < buttonWidth; x += 2) {
			if (button.getAlphaAt(x, midy) > .7) {
				hitboxXleft = x;
				break;
			}
		}
		if (hitboxXleft > buttonWidth / 2) {
			hitboxXleft = 0;
		}
	}

	static boolean isHovered(Node node, int mouseX, int mouseY)
	{
		return mouseX > node.x + hitboxXleft &&
			mouseY > node.y + hitboxYtop &&
			mouseY < node.y + hitboxYbot;
	}

	private static final int HOVER_HIGHLIGHT_TIME = 400;

	Node prev, next;

	float height;
	float x, y;

	private boolean isHovered;
	private int hovertime = HOVER_HIGHLIGHT_TIME;

	/**
	 * @return {@code null} if this map is not in this node, or the node that was focused
	 */
	abstract BeatmapNode attemptFocusMap(Beatmap beatmap);
	abstract void draw(Graphics g, Node focusNode);

	void update(int delta)
	{
		if (this.hovertime < HOVER_HIGHLIGHT_TIME) {
			this.hovertime += delta;
		}
	}

	void toggleHovered()
	{
		if (this.isHovered = !this.isHovered) {
			this.hovertime = 0;
		}
	}

	protected void drawButton(Color color)
	{
		button.draw(x, y, this.mixBackgroundColor(color));
	}

	private Color mixBackgroundColor(Color baseColor)
	{
		if (this.hovertime >= HOVER_HIGHLIGHT_TIME) {
			return baseColor;
		}
		final float progress = IN_QUAD.calc((float) this.hovertime / HOVER_HIGHLIGHT_TIME);
		return baseColor.brighter(.25f * (1f - progress));
	}
}
