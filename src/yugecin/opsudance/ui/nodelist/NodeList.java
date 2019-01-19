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
 * manages nodes UI to display in {@link itdelatrisu.opsu.states.SongMenu}
 */
public class NodeList
{
	private final StarStream starStream;

	public final Scrolling scrolling;

	float headerY, footerY;
	private float areaHeight, areaHeight2;
	private float scrollBarTopY, scrollBarHeight, scrollerHeight;

	private float maxVisibleButtons;

	private final NodeCollection nodes;
	private Node first;

	private Node hoverNode;
	private BeatmapNode focusNode;
	private boolean keepHover;

	private Node firstNodeToDraw;

	public NodeList()
	{
		this.nodes = new NodeCollection(0);

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
		for (int i = 0; i < this.nodes.size; i++) {
			final Node n = this.nodes.nodes[i];
			n.setHovered(false);
			n.redisplayReset();
		}
	}

	public void preRenderUpdate()
	{
		this.starStream.update(renderDelta);

		Node.update(renderDelta);
		Node newHoverNode = null;
		this.firstNodeToDraw = null;
		this.scrolling.setMax(this.nodes.size * Node.buttonOffset);
		this.scrolling.update(renderDelta);
		final float position = -this.scrolling.position + areaHeight2 + Node.buttonOffset2;
		final float midY = headerY + areaHeight2 - Node.buttonOffset2;
		final float invisibleYOffset = this.headerY - Node.buttonOffset * 2f;
		final boolean mouseYInHoverRange = headerY < mouseY && mouseY < footerY;
		this.starStream.pause();
		Node n = this.first;
		int idx = 0;
		while (n != null) {
			n.targetY = position + idx * Node.buttonOffset;
			n.targetXOffset = Math.abs(n.targetY - midY) / Node.indentPerOffset;

			n.update(renderDelta, this.hoverNode);

			if (n.y > invisibleYOffset && n.y < footerY) {
				if (this.firstNodeToDraw == null) {
					this.firstNodeToDraw = n;
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

			n = n.next;
			idx++;
		}
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
		Node node = this.firstNodeToDraw;
		while (node != null && node.y < footerY) {
			node.draw(g, this.focusNode);
			node = node.next;
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
		this.nodes.clear();
		Node before = this.first = null;

		final ArrayList<Beatmap> temp = new ArrayList<>(20);
		final ArrayList<Beatmap> maps = beatmapList.maps;
		this.nodes.ensureCapacity(maps.size());
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
			if (before == null) {
				this.first = newnode;
			} else {
				newnode.prev = before;
				before.next = newnode;
			}
			before = newnode;
			this.nodes.add(newnode);
		}
	}

	/**
	 * {@code nodesToInsert} should be at least of size 2
	 */
	void replace(Node node, Node[] nodesToInsert)
	{
		// though not really needed since the initial size is the beatmap count
		this.nodes.ensureCapacity(this.nodes.size + nodesToInsert.length - 1);
		final Node replacement = nodesToInsert[0];
		this.nodes.nodes[node.idx] = replacement;
		replacement.prev = node.prev;
		if (node.prev != null) {
			node.prev.next = replacement;
		}
		int idx = replacement.idx = node.idx;
		int inc = nodesToInsert.length - 1;
		++idx;
		this.nodes.shiftRight(idx, inc);
		System.arraycopy(nodesToInsert, 1, this.nodes.nodes, idx, inc);
		Node prev = replacement;
		for (int i = 1; i < nodesToInsert.length; i++) {
			nodesToInsert[i].prev = prev;
			prev = prev.next = nodesToInsert[i];
		}
		nodesToInsert[inc].next = node.next;
		if (node.next != null) {
			node.next.prev = nodesToInsert[inc];
		}
		for (int i = replacement.idx; i < this.nodes.size; i++) {
			this.nodes.nodes[i].idx = i;
		}
		if (this.firstNodeToDraw == node) {
			this.firstNodeToDraw = replacement;
		}
		if (this.first == node) {
			this.first = replacement;
		}
	}

	/**
	 * {@code length} should be at least 2
	 */
	void replace(int idx, int length, Node replacement)
	{
		final Node startnode = this.nodes.nodes[idx];
		final Node endNode = this.nodes.nodes[idx + length - 1];
		this.nodes.nodes[idx] = replacement;
		replacement.prev = startnode.prev;
		if (startnode.prev != null) {
			startnode.prev.next = replacement;
		}
		replacement.next = endNode.next;
		if (endNode.next != null) {
			endNode.next.prev = replacement;
		}
		this.nodes.shiftLeft(idx + length, length - 1);
		for (int i = this.nodes.size; i > idx;) {
			--i;
			this.nodes.nodes[i].idx = i;
		}
	}

	void unexpandAll()
	{
		int len = 0;
		BeatmapSet lastset = null;
		for (int i = 0; i <= this.nodes.size; i++) {
			Node node = null;
			if (i < this.nodes.size) {
				node = this.nodes.nodes[i];
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
					final Beatmap[] maps = new Beatmap[len];
					for (int j = i - len, k = 0; j < i; j++, k++) {
						final Node n = this.nodes.nodes[j];
						if (this.hoverNode == n) {
							this.hoverNode = null;
						}
						if (this.focusNode == n) {
							this.focusNode = null;
						}
						maps[k] = ((BeatmapNode) n).beatmap;
					}
					final MultiBeatmapNode mb = new MultiBeatmapNode(maps);
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
		if (this.nodes.isEmpty()) {
			return;
		}
		Node node;
		out: for (;;) {
			node = this.nodes.nodes[rand.nextInt(this.nodes.size)];
			if (node instanceof MultiBeatmapNode) {
				this.unexpandAll();
				final BeatmapNode[] nodes = ((MultiBeatmapNode) node).expand();
				node = nodes[rand.nextInt(nodes.length)];
			}
			if (node instanceof BeatmapNode) {
				break out;
			}
			final Node n = node;
			while ((node = node.prev) != null) {
				if (node instanceof BeatmapNode) {
					break out;
				}
			}
			node = n;
			while ((node = node.next) != null) {
				if (node instanceof BeatmapNode) {
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
		Node n = first;
		while (n != null) {
			final BeatmapNode node = n.attemptFocusMap(map);
			if (node != null) {
				this.focusNode(node, playAtPreviewTime);
				return true;
			}
			n = n.next;
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
		for (int i = this.nodes.size; i > 0;) {
			this.nodes.nodes[--i].focusChanged(node.beatmap.beatmapSet);
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
}
