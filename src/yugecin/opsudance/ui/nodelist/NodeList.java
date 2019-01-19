// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import static yugecin.opsudance.core.InstanceContainer.*;

import java.util.ArrayList;

import org.newdawn.slick.Graphics;

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.BeatmapSet;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.StarStream;
import yugecin.opsudance.core.Nullable;
import yugecin.opsudance.core.input.*;
import yugecin.opsudance.utils.FloatConsumer;

import static org.lwjgl.opengl.GL11.*;

/**
 * manages nodes to display in {@link itdelatrisu.opsu.states.SongMenu}
 */
public class NodeList
{
	private final StarStream starStream;

	public final Scrolling scrolling;

	float headerY, footerY;
	private float areaHeight, areaHeight2;
	private float scrollBarTopY, scrollBarHeight, scrollerHeight;

	private float maxVisibleButtons;

	private Node hoverNode;
	private BeatmapNode focusNode;
	private boolean keepHover;

	private int firstIdxToDraw;

	int size;
	Node[] nodes;

	public NodeList()
	{
		this.nodes = new Node[0];

		this.scrolling = new Scrolling();

		this.starStream = new StarStream(0, 0, 0, 0, 75);
		this.starStream.setDirectionSpread(10f);
		this.starStream.allowInOutQuad(false);
		this.starStream.staggerSpawn(false);
	}

	public void revalidate(
		float headerY,
		float footerY,
		float scrollBarTopY,
		float scrollBarBotY)
	{
		Node.revalidate();

		this.headerY = headerY;
		this.footerY = footerY;
		this.areaHeight = footerY - headerY;
		this.areaHeight2 = areaHeight / 2f;
		this.scrollBarTopY = scrollBarTopY;
		this.scrollBarHeight = scrollBarBotY - scrollBarTopY;
		this.scrollerHeight = 0.0422f * height;

		this.starStream.reinitStarImage();

		this.maxVisibleButtons = areaHeight / Node.buttonOffset;

		this.starStream.setDirection(-width, 0);
		this.starStream.setPositionSpread(Node.buttonOffset / 5);
	}

	/**
	 * Called by SongMenu#enter
	 */
	public void enter()
	{
		this.reFadeIn();
		this.centerFocusedNodeNow();
		this.scrolling.resetState();
		this.keepHover = false;
		this.hoverNode = null;
		for (int i = 0; i < this.size; i++) {
			final Node n = this.nodes[i];
			n.setHovered(false);
			n.redisplayReset();
		}
	}

	public void preRenderUpdate()
	{
		this.starStream.update(renderDelta);

		Node.update(renderDelta);
		Node newHoverNode = null;
		this.firstIdxToDraw = this.size;
		final float position = -this.scrolling.position + areaHeight2 + Node.buttonOffset2;
		final float midY = headerY + areaHeight2 - Node.buttonOffset2;
		final float invisibleYOffset = this.headerY - Node.buttonOffset * 2f;
		final boolean mouseYInHoverRange = headerY < mouseY && mouseY < footerY;
		this.starStream.pause();
		float offset = 0f;
		for (int idx = 0; idx < this.size; idx++) {
			final Node n = this.nodes[idx];

			n.targetY = position + offset;
			n.targetXOffset = Math.abs(n.targetY - midY) / Node.indentPerOffset;
			offset += n.getHeight();

			n.update(renderDelta, this.hoverNode);

			if (n.y > invisibleYOffset && n.y < footerY) {
				if (this.firstIdxToDraw == this.size) {
					this.firstIdxToDraw = idx;
				}

				if (mouseYInHoverRange &&
					n.y < footerY &&
					!displayContainer.suppressHover &&
					Node.isHovered(n, mouseX, mouseY))
				{
					newHoverNode = n;
				}

				if (n == this.focusNode)
				{
					this.starStream.setPosition(width, n.y + Node.buttonOffset2);
					this.starStream.resume();
				}
			}
		}
		this.scrolling.setMax(offset);
		this.scrolling.update(renderDelta);
		if (!this.keepHover && this.hoverNode != newHoverNode) {
			if (this.hoverNode != null) {
				this.hoverNode.setHovered(false);
			}
			if ((this.hoverNode = newHoverNode) != null) {
				this.hoverNode.setHovered(true);
			}
		}
	}

	public void render(Graphics g)
	{
		this.starStream.draw();
		for (int idx = this.firstIdxToDraw; idx < this.size; idx++) {
			final Node node = this.nodes[idx];
			if (node.y >= footerY) {
				break;
			}
			node.draw(g, this.focusNode);
		}

		final float scrollerY =
			this.scrolling.positionNorm * (this.scrollBarHeight - this.scrollerHeight);
		final float scrollerEnd = scrollerY + this.scrollerHeight;
		glDisable(GL_TEXTURE_2D);
		glPushMatrix();
		glTranslatef(width - 8f, this.scrollBarTopY, 0f);
		glColor4f(0f, 0f, 0f, .3f);
		glBegin(GL_QUADS);
		glVertex2f(0f, 0f);
		glVertex2f(8f, 0f);
		glVertex2f(8f, this.scrollBarHeight);
		glVertex2f(0f, this.scrollBarHeight);
		glColor3f(1f, 1f, 1f);
		glVertex2f(0f, scrollerY);
		glVertex2f(8f, scrollerY);
		glVertex2f(8f, scrollerEnd);
		glVertex2f(0f, scrollerEnd);
		glEnd();
		glPopMatrix();
		
		//Fonts.MEDIUM.drawString(20, height2, scrolling.currentSpeed + "");
		Fonts.MEDIUM.drawString(20, height2, scrolling.scrollProgress + "");
	}

	public void recreate()
	{
		for (int i = this.size; i > 0;) {
			this.nodes[--i] = null;
		}
		this.size = 0;

		final ArrayList<Beatmap> temp = new ArrayList<>(20);
		final ArrayList<Beatmap> maps = beatmapList.maps;
		this.ensureCapacity(maps.size());
		int idx = 0;
		for (int i = 0, size = maps.size(); i < size; i++) {
			final Beatmap map = maps.get(i);
			final BeatmapSet set = map.beatmapSet;
			Beatmap nextmap;
			final Node newnode;
			if (++i < size && (nextmap = maps.get(i)).beatmapSet == set) {
				temp.clear();
				temp.add(map);
				temp.add(nextmap);
				while (++i < size) {
					if ((nextmap = maps.get(i)).beatmapSet != set) {
						break;
					}
					temp.add(nextmap);
				}
				newnode = new MultiBeatmapNode(temp.toArray(Beatmap.EMPTY_ARRAY));
			} else {
				newnode = new BeatmapNode(map);
			}
			i--;
			newnode.idx = idx++;
			this.nodes[this.size++] = newnode;
		}
		for (int i = 0; i < this.size; i++) {
			this.nodes[i].onSiblingNodeUpdated();
		}
	}

	/**
	 * {@code nodesToInsert} should be at least of size 2
	 */
	void replace(Node node, Node[] nodesToInsert)
	{
		// though not really needed since the initial size is the beatmap count
		this.ensureCapacity(this.size + nodesToInsert.length - 1);
		final Node replacement = nodesToInsert[0];
		this.nodes[node.idx] = replacement;
		replacement.takeOver(node);
		int idx = replacement.idx = node.idx;
		int inc = nodesToInsert.length - 1;
		++idx;
		this.shiftNodesRight(idx, inc);
		final int endIdxExclusive = replacement.idx + nodesToInsert.length;
		System.arraycopy(nodesToInsert, 1, this.nodes, idx, inc);
		for (int i = replacement.idx; i < this.size; i++) {
			final Node n = this.nodes[i];
			n.idx = i;
			if (i < endIdxExclusive) {
				n.onSiblingNodeUpdated();
				if (i < endIdxExclusive - 1) {
					n.appearTime = 0;
					n.appearValue = 0f;
				}
			}
			if (i >= idx && n instanceof BeatmapNode) {
				((BeatmapNode) n).doFade = true;
			}
		}
		if (0 < replacement.idx) {
			this.nodes[replacement.idx].onSiblingNodeUpdated();
		}
		if (idx + inc < this.size) {
			this.nodes[idx + inc].onSiblingNodeUpdated();
		}
	}

	/**
	 * {@code length} should be at least 2
	 */
	void replace(int idx, int length, Node replacement)
	{
		replacement.takeOver(this.nodes[idx]);
		this.nodes[idx] = replacement;
		this.shiftNodesLeft(idx + length, length - 1);
		for (int i = this.size; i > idx;) {
			--i;
			this.nodes[i].idx = i;
		}
		replacement.onSiblingNodeUpdated();
		if (0 < idx) {
			this.nodes[idx - 1].onSiblingNodeUpdated();
		}
		if (idx < this.size) {
			final Node n = this.nodes[idx + 1];
			if (n != null) {
				this.nodes[idx + 1].onSiblingNodeUpdated();
			}
		}
	}

	void unexpandAll()
	{
		int len = 0;
		BeatmapSet lastset = null;
		for (int i = 0; i <= this.size; i++) {
			Node node = null;
			if (i < this.size) {
				node = this.nodes[i];
			}
			BeatmapSet newset = null;
			if (node instanceof BeatmapNode) {
				if (lastset == null) {
					lastset = ((BeatmapNode) node).beatmap.beatmapSet;
					len = 1;
					continue;
				}
				newset = ((BeatmapNode) node).beatmap.beatmapSet;
			}
			if (newset != lastset || newset == null) {
				if (len > 1 && lastset != null) {
					float totalHeight = 0f;
					final Beatmap[] maps = new Beatmap[len];
					for (int j = i - len, k = 0; j < i; j++, k++) {
						final Node n = this.nodes[j];
						totalHeight += n.getHeight();
						if (this.hoverNode == n) {
							this.hoverNode = null;
						}
						if (this.focusNode == n) {
							this.focusNode = null;
						}
						maps[k] = ((BeatmapNode) n).beatmap;
					}
					final MultiBeatmapNode mb = new MultiBeatmapNode(maps);
					mb.appearTime = 0;
					mb.appearValue = 0f;
					mb.fromHeight = totalHeight;
					this.replace(i - len, len, mb);
				}
				len = 1;
				lastset = newset;
				continue;
			}
			len++;
		}
	}

	public void reFadeIn()
	{
		Node.fadeInTime = 0;
	}

	@Nullable
	public Beatmap getFocusedMap()
	{
		if (this.focusNode != null) {
			return this.focusNode.beatmap;
		}
		return null;
	}

	public boolean isHoveredNodeFocusedNode()
	{
		return this.focusNode == this.hoverNode && this.focusNode != null;
	}

	public boolean focusHoveredNode()
	{
		if (this.hoverNode == null) {
			return false;
		}
		if (this.hoverNode instanceof MultiBeatmapNode) {
			this.unexpandAll();
			final BeatmapNode[] nodes = ((MultiBeatmapNode) this.hoverNode).expand();
			this.focusNode(nodes[0], /*playAtPreviewTime*/ true);
			return true;
		}
		if (this.hoverNode instanceof BeatmapNode) {
			this.focusNode((BeatmapNode) this.hoverNode, /*playAtPreviewTime*/ true);
			return true;
		}
		return false;
	}

	/**
	 * Has no effect if no beatmap nodes are in the list.
	 */
	public void focusRandomMap(boolean playAtPreviewTime)
	{
		if (this.size == 0) {
			return;
		}
		Node node;
		out: for (;;) {
			node = this.nodes[rand.nextInt(this.size)];
			if (node instanceof MultiBeatmapNode) {
				this.unexpandAll();
				final BeatmapNode[] nodes = ((MultiBeatmapNode) node).expand();
				node = nodes[rand.nextInt(nodes.length)];
			}
			if (node instanceof BeatmapNode) {
				break out;
			}
			int idx;
			for (idx = node.idx + 1; idx < this.size; idx++) {
				if ((node = this.nodes[idx]) instanceof BeatmapNode) {
					break out;
				}
			}
			for (idx = node.idx - 1; idx >= 0; idx--) {
				if ((node = this.nodes[idx]) instanceof BeatmapNode) {
					break out;
				}
			}
			return;
		}
		this.focusNode((BeatmapNode) node, playAtPreviewTime);
	}

	/**
	 * Focuses the node associated with the given map.
	 * Will expand sets if map is in a set.
	 *
	 * @return {@code true} if the map was visible and is now focused
	 */
	public boolean attemptFocusMap(Beatmap map, boolean playAtPreviewTime)
	{
		for (int i = 0; i < this.size; i++) {
			final BeatmapNode node = this.nodes[i].attemptFocusMap(map);
			if (node != null) {
				this.focusNode(node, playAtPreviewTime);
				return true;
			}
		}
		return false;
	}

	/**
	 * Will collapse previous expanded nodes, except if node is an expanded node
	 * Will scroll to node if in song menu state 
	 */
	void focusNode(BeatmapNode node, boolean playAtPreviewTime)
	{
		if (!node.isFromExpandedMultiNode) {
			this.unexpandAll();
		}
		this.focusNode = node;
		final Beatmap beatmap = node.beatmap;
		if (beatmap.timingPoints == null) {
			BeatmapParser.parseTimingPoints(beatmap);
		}
		MusicController.play(beatmap, /*loop*/ false, playAtPreviewTime);
		if (displayContainer.isIn(songMenuState)) {
			this.centerFocusedNodeSmooth();
		}
		for (int i = this.size; i > 0;) {
			this.nodes[--i].focusChanged(node.beatmap.beatmapSet);
		}
	}

	public boolean mousePressed(MouseEvent e)
	{
		if (e.button == Input.LMB) {
			this.keepHover = true;
		}
		return this.scrolling.mousePressed(e);
	}

	public boolean mouseDragged(MouseDragEvent e)
	{
		return this.scrolling.mouseDragged(e);
	}

	public boolean mouseReleased(MouseEvent e)
	{
		if (e.button == Input.LMB) {
			this.keepHover = false;
			// flash hovered node again after hold
			if (this.hoverNode != null) {
				this.hoverNode.hoverHighlightTime = 0;
			}
		}
		return this.scrolling.mouseReleased(e);
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		this.scrolling.mouseWheelMoved(e);
	}

	public void scrollPageUp()
	{
		this.scrollButtonAmount(-maxVisibleButtons);
	}

	public void scrollPageDown()
	{
		this.scrollButtonAmount(maxVisibleButtons);
	}

	/**
	 * Scrolls through the song list.
	 */
	public void scrollButtonAmount(float amountOfButtons)
	{
		if (amountOfButtons != 0) {
			scrolling.addOffset(amountOfButtons * Node.buttonOffset);
		}
	}

	public void centerFocusedNodeSmooth()
	{
		this.scrollMakeNodeVisible(this.focusNode, this.scrolling::scrollToPosition);
	}

	public void centerFocusedNodeNow()
	{
		this.scrollMakeNodeVisible(this.focusNode, this.scrolling::setPosition);
	}

	private void scrollMakeNodeVisible(Node node, FloatConsumer scrollMethod)
	{
		if (node != null) {
			scrollMethod.accept(node.idx * Node.buttonOffset);
		}
	}

	// collection methods

	void ensureCapacity(int size)
	{
		if (this.nodes.length < size) {
			final Node[] n = new Node[size];
			System.arraycopy(this.nodes, 0, n, 0, this.size);
			this.nodes = n;
		}
	}

	void shiftNodesRight(int from, int amount)
	{
		System.arraycopy(this.nodes, from, this.nodes, from + amount, this.size - from);
		this.size += amount;
	}

	void shiftNodesLeft(int from, int amount)
	{
		System.arraycopy(this.nodes, from, this.nodes, from - amount, this.size - from);
		this.size -= amount;
		for (int i = this.size + amount; i > this.size;) {
			this.nodes[--i] = null;
		}
	}
}
