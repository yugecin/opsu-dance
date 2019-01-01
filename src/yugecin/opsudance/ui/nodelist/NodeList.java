// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui.nodelist;

import static yugecin.opsudance.core.InstanceContainer.*;

import java.util.ArrayList;

import org.newdawn.slick.Graphics;

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.ui.KineticScrolling;
import itdelatrisu.opsu.ui.StarStream;
import yugecin.opsudance.core.Nullable;
import yugecin.opsudance.utils.FloatConsumer;

/**
 * manages nodes to display in {@link itdelatrisu.opsu.states.SongMenu}
 */
public class NodeList
{
	private final StarStream starStream;

	public final KineticScrolling scrolling;

	private float areaHeight, areaHeight2;
	private float headerY, footerY;

	private float buttonMinX, buttonIndent;
	private float buttonOffset, buttonOffset2;
	private float maxVisibleButtons;

	private final ArrayList<Node> nodes;
	private Node first;

	private Node hoverNode;
	private BeatmapNode focusNode;

	private Node firstNodeToDraw;

	public NodeList()
	{
		this.nodes = new ArrayList<>();

		this.scrolling = new KineticScrolling();
		this.scrolling.setAllowOverScroll(true);

		this.starStream = new StarStream(0, 0, 0, 0, 75);
		this.starStream.setDirectionSpread(10f);
		this.starStream.allowInOutQuad(false);
		this.starStream.staggerSpawn(false);
	}

	public void revalidate(final float headerY, final float footerY)
	{
		Node.revalidate();

		this.headerY = headerY;
		this.footerY = footerY;
		this.areaHeight = footerY - headerY;
		this.areaHeight2 = areaHeight / 2f;

		this.starStream.reinitStarImage();

		this.buttonMinX = width * (isWidescreen ? 0.55f : 0.35f);
		this.buttonIndent = width * (isWidescreen ? 0.00875f : 0.0125f);
		this.buttonOffset = Node.buttonHeight * 0.65f;
		this.buttonOffset2 = this.buttonOffset / 2f;
		this.maxVisibleButtons = areaHeight / buttonOffset;

		this.starStream.setDirection(-width, 0);
		this.starStream.setPositionSpread(buttonOffset / 5);

		this.lastNodeUpdate = System.currentTimeMillis();
	}

	public void preRenderUpdate()
	{
		this.starStream.update(renderDelta);

		this.updateNodePositionsNow(mouseX, mouseY);
	}

	private long lastNodeUpdate;
	private void updateNodePositionsNow(int mouseX, int mouseY)
	{
		// own deltas because this function can get called multiple times per update
		// TODO do this in a better way
		final long time = System.currentTimeMillis();
		final int delta = (int) (time - this.lastNodeUpdate);
		this.lastNodeUpdate = time;

		final Node lastHoverNode = hoverNode;
		this.hoverNode = null;
		this.firstNodeToDraw = null;
		this.scrolling.setMinMax(0, this.nodes.size() * this.buttonOffset);
		this.scrolling.update(delta);
		final float position = -this.scrolling.getPosition() + areaHeight2 + buttonOffset2;
		final float midY = headerY + areaHeight2 - buttonOffset2;
		final float invisibleYOffset = this.headerY - this.buttonOffset * 2f;
		Node n = this.first;
		int idx = 0;
		while (n != null) {
			n.update(delta);

			if (n.y > invisibleYOffset && this.firstNodeToDraw == null) {
				this.firstNodeToDraw = n;
			}
			n.y = position + idx * this.buttonOffset;
			final float midoffset = Math.abs(n.y - midY);
			n.x = this.buttonMinX + midoffset / this.buttonOffset * this.buttonIndent;

			if (Node.isHovered(n, mouseX, mouseY)) {
				this.hoverNode = n;
			}

			n = n.next;
			idx++;
		}
		if (this.hoverNode != lastHoverNode) {
			if (lastHoverNode != null) {
				lastHoverNode.toggleHovered();
			}
			if (this.hoverNode != null) {
				this.hoverNode.toggleHovered();
			}
		}
	}

	public void render(Graphics g)
	{
		Node node = this.firstNodeToDraw;
		while (node != null && node.y < footerY) {
			node.draw(g, this.focusNode);
			node = node.next;
		}
	}

	public void recreate()
	{
		this.nodes.clear();
		Node before = this.first = null;

		final ArrayList<Beatmap> maps = beatmapList.maps;
		this.nodes.ensureCapacity(maps.size());
		for (int i = 0, size = maps.size(); i < size; i++) {
			final Beatmap map = maps.get(i);
			final Node newnode = new BeatmapNode(map);
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

	@Nullable
	public Beatmap getFocusedMap()
	{
		if (this.focusNode != null) {
			return this.focusNode.beatmap;
		}
		return null;
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

	private void focusNode(BeatmapNode node, boolean playAtPreviewTime)
	{
		this.focusNode = node;
		final Beatmap beatmap = node.beatmap;
		if (beatmap.timingPoints == null) {
			BeatmapParser.parseTimingPoints(beatmap);
		}
		MusicController.play(beatmap, /*loop*/ false, playAtPreviewTime);
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
			scrolling.scrollOffset(amountOfButtons * buttonOffset);
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
			//final int index = node.index + node.beatmapIndex;
			int index = 2;
			scrollMethod.accept(index * this.buttonOffset);
		}
	}
}
