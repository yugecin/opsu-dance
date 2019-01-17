// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import static yugecin.opsudance.core.InstanceContainer.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	private final ArrayList<Node> nodes;
	private Node first;

	private Node hoverNode;
	private BeatmapNode focusNode;
	private boolean keepHover;

	private Node firstNodeToDraw;

	public NodeList()
	{
		this.nodes = new ArrayList<>();

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
	}

	public void preRenderUpdate()
	{
		this.starStream.update(renderDelta);

		Node.update(renderDelta);
		Node newHoverNode = null;
		this.firstNodeToDraw = null;
		this.scrolling.setMax(this.nodes.size() * Node.buttonOffset);
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

		final ArrayList<Beatmap> temp = new ArrayList<>(10);
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
		final Node replacement = nodesToInsert[0];
		this.nodes.set(node.idx, replacement);
		replacement.prev = node.prev;
		if (node.prev != null) {
			node.prev.next = replacement;
		}
		int idx = replacement.idx = node.idx;
		Node prev = replacement;
		for (int i = 1; i < nodesToInsert.length; i++) {
			nodesToInsert[i].prev = prev;
			prev = prev.next = nodesToInsert[i];
			nodesToInsert[i].idx = ++idx;
		}
		int inc = nodesToInsert.length - 1;
		nodesToInsert[inc].next = node.next;
		if (node.next != null) {
			node.next.prev = nodesToInsert[inc];
		}
		List<Node> l = Arrays.asList(nodesToInsert).subList(1, nodesToInsert.length);
		if (node.idx == this.nodes.size() - 1) {
			this.nodes.addAll(l);
		} else {
			this.nodes.addAll(node.idx + 1, l);
		}
		while (++idx < this.nodes.size() - 1) {
			this.nodes.get(idx).idx += inc;
		}
		if (this.firstNodeToDraw == node) {
			this.firstNodeToDraw = replacement;
		}
		if (this.first == node) {
			this.first = replacement;
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
			((MultiBeatmapNode) this.hoverNode).expand(true);
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
			node = this.nodes.get(rand.nextInt(this.nodes.size()));
			if (node instanceof MultiBeatmapNode) {
				final BeatmapNode[] nodes = ((MultiBeatmapNode) node).expand(false);
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
	 * Will scroll to node if in song menu state 
	 */
	void focusNode(BeatmapNode node, boolean playAtPreviewTime)
	{
		this.focusNode = node;
		final Beatmap beatmap = node.beatmap;
		if (beatmap.timingPoints == null) {
			BeatmapParser.parseTimingPoints(beatmap);
		}
		MusicController.play(beatmap, /*loop*/ false, playAtPreviewTime);
		if (displayContainer.isIn(songMenuState)) {
			this.centerFocusedNodeSmooth();
		}
		for (int i = this.nodes.size(); i > 0;) {
			this.nodes.get(--i).focusChanged(node.beatmap.beatmapSet);
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
