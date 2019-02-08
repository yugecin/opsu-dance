// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.opengl.Texture;

import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSet;
import yugecin.opsudance.core.InstanceContainer;
import yugecin.opsudance.render.TextureData;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.utils.GLHelper.*;

abstract class Node
{
	// TODO: (or not) missing stuff: version should fade in/out when (un)expanding,
	// TODO: (or not) missing stuff: button color should be eased

	public static final Color
		BUTTON_ORANGE   = new Color(255, 122, 20),
		BUTTON_PINK   = new Color(247, 81, 156),
		BUTTON_BLUE   = new Color(3, 144, 255);

	private static TextureData button;
	static int buttonWidth, buttonHeight;
	static float buttonOffset, buttonOffset2;
	/**
	 * used for internal y offset, ie offset from y pos to actual button drawing y pos
	 */
	static float buttonInternalOffset;
	private static float buttonOffsetX;

	public static float buttonIndent, indentPerOffset;
	private static float buttonHoverIndent;

	protected static float cx;

	static int hitboxYtop, hitboxYbot, hitboxXleft;
	static int hitboxHeight;

	static int fadeInTime;
	private static final int FADE_IN_TIME = 2000;
	private static float fadeInXMod;

	static void revalidate()
	{
		button = new TextureData(MENU_BUTTON_BG.getImage());

		final Texture t = button.image.getTexture();
		final int w = t.getImageWidth();
		final int h = t.getImageHeight();
		hitboxYtop = 0;
		hitboxYbot = h;
		hitboxXleft = 0;
		float hitboxXright = w;
		if (t.hasAlpha()) {
			final byte[] d = t.getTextureData();

			int minheight = h, maxheight = 0, left = w, right = 0;
			hitboxXright = w - left;
			int _w = t.getTextureWidth();
			int _h = t.getTextureHeight();
			for (int i = 0, x = 3; i < _h; i++) {
				for (int j = 0; j < _w; j++, x += 4) {
					if (i > h || j > w) {
						continue;
					}
					int v = d[x];
					if (v < 0) {
						v += 256;
					}
					if (v > 100) {
						minheight = minheight < i ? minheight : i;
						maxheight = maxheight > i ? maxheight : i;
						left = left < j ? left : j;
						right = right > j ? right : j;
					}
				}
			}
			if (minheight > h / 2) minheight = 0;
			if (maxheight < h / 2) maxheight = h;
			if (left > w / 2) left = 0;
			if (right < w / 2) right = w;
			hitboxYtop = minheight;
			hitboxYbot = maxheight;
			hitboxXleft = left;
			hitboxXright = right;
		}

		hitboxXright = hitboxXright / w;
		final float hbtop = (float) hitboxYtop / h;
		final float hbbot = (float) hitboxYbot / h;
		final float scaleup = (float) h / (hitboxYbot - hitboxYtop);
		final float ratio = button.width / button.height;
		final float desiredButtonHeight = InstanceContainer.height * 0.117f;
		button.width = ratio * (button.height = desiredButtonHeight * scaleup);
		button.height2 = button.height / 2f;
		button.width2 = button.width / 2f;
		buttonWidth = (int) button.width;
		buttonHeight = (int) button.height;
		hitboxYtop = (int) (hbtop * button.height);
		hitboxYbot = (int) (hbbot * button.height);
		hitboxHeight = hitboxYbot - hitboxYtop;
		buttonIndent = width * (isWidescreen ? 0.00875f : 0.0125f);
		buttonHoverIndent = buttonIndent * 6.6666f;
		buttonOffset = buttonHeight * 0.65f;
		buttonOffset2 = buttonOffset / 2f;
		buttonInternalOffset = (buttonHeight * .935f - buttonOffset) / 2f;
		indentPerOffset = buttonOffset / buttonIndent;
		buttonOffsetX = -(hitboxXright * buttonWidth) + buttonHoverIndent * 2;
		buttonOffsetX += 20; // padding because they're usually rounded
		cx = buttonWidth * 0.043f;
	}

	static boolean isHovered(Node node, int mouseX, int mouseY)
	{
		return mouseX > node.x + hitboxXleft &&
			(mouseY > node.y + hitboxYtop ||
				(node.idx > 0 &&
					mouseY > nodeList.nodes[node.idx - 1].y + hitboxYbot)) &&
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

	int idx;

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
	float prevPositionOffset;
	int repositionTime;
	static final int REPOSITION_TIME = 600;

	float appearValue = 1f;
	int appearTime = APPEAR_TIME;
	static final int APPEAR_TIME = 1000;

	/**
	 * may expand nodes (but must unexpand first)
	 * @return {@code null} if this map is not in this node, or the node that was focused
	 */
	abstract BeatmapNode attemptFocusMap(Beatmap beatmap);
	abstract void draw(Graphics g, Node focusNode, Node selectedNode);
	protected abstract boolean belongsToSet(BeatmapSet focusedSet);

	void update(int delta, Node hoveredNode)
	{
		if (this.hoverHighlightTime < HOVER_HIGHLIGHT_TIME) {
			this.hoverHighlightTime += delta;
		}

		if (this.appearTime < APPEAR_TIME) {
			if ((this.appearTime += delta) > APPEAR_TIME) {
				this.appearTime = APPEAR_TIME;
			}
			this.appearValue = OUT_QUART.calc((float) this.appearTime / APPEAR_TIME);
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
			+ (buttonOffsetX - this.hoverIndentValue + this.targetXOffset) * fadeInXMod
			- this.focusIndentValue;
		this.y = this.targetY + this.hoverSpreadValue + this.getInternalOffset();

		if (this.repositionTime > 0) {
			final float progress = 1f - (float) repositionTime / REPOSITION_TIME;
			this.y += this.prevPositionOffset * (1f - OUT_QUART.calc(progress));
			this.repositionTime -= delta;
		}
	}

	/**
	 * takes over animation progress from other node
	 *
	 * skips appearTime
	 */
	void takeOver(Node other)
	{
		this.hoverHighlightTime = other.hoverHighlightTime;
		this.hoverIndentTime = other.hoverIndentTime;
		this.hoverIndentValue = other.hoverIndentValue;
		this.hoverIndentFrom = other.hoverIndentFrom;
		this.hoverIndentTo = other.hoverIndentTo;
		this.hoverSpreadTime = other.hoverSpreadTime;
		this.hoverSpreadValue = other.hoverSpreadValue;
		this.hoverSpreadFrom = other.hoverSpreadFrom;
		this.hoverSpreadTo = other.hoverSpreadTo;
		this.focusIndentTime = other.focusIndentTime;
		this.focusIndentValue = other.focusIndentValue;
		this.focusIndentFrom = other.focusIndentFrom;
		this.focusIndentTo = other.focusIndentTo;
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
		this.appearTime = APPEAR_TIME;
		this.appearValue = 1f;
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

	/**
	 * gets called when either:
	 * <ul>
	 *   <li>this node was inserted</li>
	 *   <li>the node preceding this node was changed</li>
	 *   <li>the node following this node was changed</li>
	 * </ul>
	 */
	void onSiblingNodeUpdated()
	{
	}

	float getHeight()
	{
		return buttonOffset;
	}

	/**
	 * @return height that this node will take after all current animations are finished
	 */
	float getEventualHeight()
	{
		return buttonOffset;
	}

	float getInternalOffset()
	{
		return 0f;
	}

	protected void drawButton(Color color, boolean isSelected)
	{
		color = this.mixBackgroundColor(color, isSelected);
		glColor4f(color.r, color.g, color.b, color.a);
		glEnable(GL_TEXTURE_2D);
		glPushMatrix();
		glTranslatef(x, y, 0f);
		simpleTexturedQuadTopLeft(button);
		glPopMatrix();
	}

	private Color mixBackgroundColor(Color baseColor, boolean isSelected)
	{
		if (this.hoverHighlightTime >= HOVER_HIGHLIGHT_TIME) {
			if (isSelected) {
				return baseColor.brighter(.4f);
			}
			return baseColor;
		}
		return baseColor.brighter((1f - IN_QUAD.calc(
			(float) this.hoverHighlightTime / HOVER_HIGHLIGHT_TIME
		)) * .25f);
	}
}
