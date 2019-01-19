// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSet;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static yugecin.opsudance.core.InstanceContainer.*;

abstract class Node
{
	public static final Color
		BUTTON_ORANGE   = new Color(255, 122, 20),
		BUTTON_PINK   = new Color(247, 81, 156),
		BUTTON_BLUE   = new Color(3, 144, 255);

	static Image button;
	static int buttonWidth, buttonHeight;
	static float buttonOffset, buttonOffset2;
	private static float buttonOffsetX;

	public static float buttonIndent, indentPerOffset;
	private static float buttonHoverIndent;

	protected static float cx, cy;

	private static int hitboxYtop, hitboxYbot, hitboxXleft;

	static int fadeInTime;
	private static final int FADE_IN_TIME = 2000;
	private static float fadeInXMod;

	static void revalidate()
	{
		button =  MENU_BUTTON_BG.getImage();
		buttonWidth = button.getWidth();
		buttonHeight = button.getHeight();
		buttonIndent = width * (isWidescreen ? 0.00875f : 0.0125f);
		buttonHoverIndent = buttonIndent * 6.6666f;
		buttonOffset = buttonHeight * 0.65f;
		buttonOffset2 = buttonOffset / 2f;
		indentPerOffset = buttonOffset / buttonIndent;
		buttonOffsetX = width - width * (isWidescreen ? 0.55f : 0.35f) - buttonHoverIndent;
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
			(mouseY > node.y + hitboxYtop ||
				// TODO: I don't like that this uses prev.
				(node.prev != null && mouseY > node.prev.y + hitboxYbot)) &&
			mouseY < node.y + hitboxYbot;
	}

	static void update(int delta)
	{
		if (fadeInTime < FADE_IN_TIME) {
			if ((fadeInTime += delta) > FADE_IN_TIME) {
				fadeInTime = FADE_IN_TIME;
			}
			fadeInXMod = OUT_EXPO.calc((float) fadeInTime / FADE_IN_TIME);
		}
	}

	Node prev, next;
	int idx;

	float height;
	float targetXOffset, targetY;
	float x, y;

	int hoverHighlightTime = HOVER_HIGHLIGHT_TIME;
	private static final int HOVER_HIGHLIGHT_TIME = 400;
	private int hoverIndentTime = HOVER_INDENT_TIME;
	private static final int HOVER_INDENT_TIME = 1000;
	private float hoverIndentValue;
	private float hoverIndentFrom;
	private float hoverIndentTo;
	private int hoverSpreadTime = HOVER_INDENT_TIME;
	private static final int HOVER_SPREAD_TIME = HOVER_INDENT_TIME;
	private float hoverSpreadValue;
	private float hoverSpreadFrom;
	private float hoverSpreadTo;
	private float focusIndentTime = HOVER_INDENT_TIME;
	private float focusIndentValue;
	private float focusIndentFrom;
	private float focusIndentTo;

	/**
	 * may expand nodes (but must unexpand first)
	 * @return {@code null} if this map is not in this node, or the node that was focused
	 */
	abstract BeatmapNode attemptFocusMap(Beatmap beatmap);
	abstract void draw(Graphics g, Node focusNode);
	protected abstract boolean belongsToSet(BeatmapSet focusedSet);

	void update(int delta, Node hoveredNode)
	{
		if (this.hoverHighlightTime < HOVER_HIGHLIGHT_TIME) {
			this.hoverHighlightTime += delta;
		}

		if (this.hoverIndentTime < HOVER_INDENT_TIME) {
			if ((this.hoverIndentTime += delta) > HOVER_INDENT_TIME) {
				this.hoverIndentTime = HOVER_INDENT_TIME;
			}
			this.hoverIndentValue = OUT_QUART.calc(
				(float) this.hoverIndentTime / HOVER_INDENT_TIME
			) * (this.hoverIndentTo - this.hoverIndentFrom) + this.hoverIndentFrom;
		}

		if (this.focusIndentTime < HOVER_INDENT_TIME) {
			if ((this.focusIndentTime += delta) > HOVER_INDENT_TIME) {
				this.focusIndentTime = HOVER_INDENT_TIME;
			}
			this.focusIndentValue = OUT_QUART.calc(
				(float) this.focusIndentTime / HOVER_INDENT_TIME
			) * (this.focusIndentTo - this.focusIndentFrom) + this.focusIndentFrom;
		}

		final float lastTo = this.hoverSpreadTo;
		if (hoveredNode == null || hoveredNode.idx == this.idx) {
			this.hoverSpreadTo = 0f;
		} else if (hoveredNode.idx < this.idx) {
			this.hoverSpreadTo = (buttonHeight - buttonOffset) / 2f;
		} else if (hoveredNode.idx > this.idx) {
			this.hoverSpreadTo = -(buttonHeight - buttonOffset) / 2f;
		}
		if (this.hoverSpreadTo != lastTo) {
			this.hoverSpreadFrom = this.hoverSpreadValue;
			this.hoverSpreadTime = 0;
		}

		if (this.hoverSpreadTime < HOVER_SPREAD_TIME) {
			if ((this.hoverSpreadTime += delta) > HOVER_SPREAD_TIME) {
				this.hoverSpreadTime = HOVER_SPREAD_TIME;
			}
			this.hoverSpreadValue = OUT_QUART.calc(
				(float) this.hoverSpreadTime / HOVER_SPREAD_TIME
			) * (this.hoverSpreadTo - this.hoverSpreadFrom) + this.hoverSpreadFrom;
		}

		this.x = width
			+ (-buttonOffsetX - this.hoverIndentValue + this.targetXOffset) * fadeInXMod
			- this.focusIndentValue;
		this.y = this.targetY + this.hoverSpreadValue;
	}

	void setHovered(boolean flag)
	{
		if (flag) {
			this.hoverHighlightTime = 0;
			this.hoverIndentTo = buttonHoverIndent;
		} else {
			this.hoverIndentTo = 0f;
		}
		this.hoverIndentFrom = this.hoverIndentValue;
		this.hoverIndentTime = 0;
	}

	void redisplayReset()
	{
		this.hoverSpreadValue = this.hoverSpreadFrom = this.hoverSpreadTo = 0f;
		this.hoverHighlightTime = 0;
		this.hoverIndentValue = this.hoverIndentFrom = this.hoverIndentTo = 0f;
		this.hoverIndentTime = 0;
	}

	void focusChanged(BeatmapSet focusedSet)
	{
		if (this.belongsToSet(focusedSet)) {
			this.focusIndentTo = buttonHoverIndent;
		} else {
			this.focusIndentTo = 0;
		}
		this.focusIndentFrom = this.focusIndentValue;
		this.focusIndentTime = 0;
		if (!displayContainer.isIn(songMenuState)) {
			this.focusIndentTime = HOVER_INDENT_TIME - 1;
		}
	}

	protected void drawButton(Color color)
	{
		button.draw(x, y, this.mixBackgroundColor(color));
	}

	private Color mixBackgroundColor(Color baseColor)
	{
		if (this.hoverHighlightTime >= HOVER_HIGHLIGHT_TIME) {
			return baseColor;
		}
		return baseColor.brighter((1f - IN_QUAD.calc(
			(float) this.hoverHighlightTime / HOVER_HIGHLIGHT_TIME
		)) * .25f);
	}
}
