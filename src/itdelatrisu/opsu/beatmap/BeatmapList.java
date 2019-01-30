/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.beatmap;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.db.BeatmapDB;
import yugecin.opsudance.core.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static itdelatrisu.opsu.ui.Colors.*;
import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Indexed, expanding, doubly-linked list data type for song groups.
 */
public class BeatmapList
{
	/** Search pattern for conditional expressions. */
	private static final Pattern SEARCH_CONDITION_PATTERN = Pattern.compile(
		"(ar|cs|od|hp|bpm|length|stars?)(==?|>=?|<=?)((\\d*\\.)?\\d+)"
	);

	private final ArrayList<BeatmapSet> sets;
	/**
	 * public read access only
	 */
	public final ArrayList<Beatmap> maps;

	/** Current list of nodes (subset of parsedNodes, used for searches). */
	public ArrayList<Beatmap> nodes;

	private final HashMap<String, Beatmap> beatmapHashDB;
	private final HashSet<Integer> beatmapSetDb;

	/** The last search query. */
	private String lastQuery;

	BeatmapList()
	{
		this.sets = new ArrayList<>();
		this.maps = new ArrayList<>();
		this.nodes = new ArrayList<>();
		this.beatmapHashDB = new HashMap<>();
		this.beatmapSetDb = new HashSet<>();
	}

	public void reset()
	{
		this.nodes = BeatmapGroup.current.filter(this.maps);
		lastQuery = "";
	}

	public void resort()
	{
		this.nodes.sort(BeatmapSortOrder.current);
	}

	/**
	 * Returns the number of elements.
	 */
	public int getVisibleSetsCount()
	{
		return this.nodes.size();
	}

	public boolean isEmpty()
	{
		return this.nodes.isEmpty();
	}

	public int getBeatmapCount()
	{
		return this.maps.size();
	}

	public int getBeatmapSetCount()
	{
		return this.sets.size();
	}

	void addBeatmapSet(ArrayList<Beatmap> beatmaps)
	{
		final BeatmapSet set = new BeatmapSet(beatmaps);
		this.sets.add(set);

		this.maps.ensureCapacity(this.maps.size() + beatmaps.size());
		for (Beatmap beatmap : beatmaps) {
			this.maps.add(beatmap);
			this.nodes.add(beatmap); // TODO check condition here?
			beatmap.beatmapSet = set;
			if (beatmap.md5Hash != null) {
				beatmapHashDB.put(beatmap.md5Hash, beatmap);
			}
		}
	}

	/**
	 * @return random map or theme beatmap if there are no maps
	 */
	public Beatmap getRandom()
	{
		if (this.nodes.isEmpty()) {
			return themeBeatmap;
		}
		return this.nodes.get(rand.nextInt(this.nodes.size()));
	}

	///**
	// * @return the node corresponding to the given map, or {@code null} if not visible
	// */
	//@Nullable
	//public BeatmapSetNode findSetNode(int beatmapSetId)
	//{
	//	int idx = this.nodes.size();
	//	while (--idx >= 0) {
	//		final BeatmapSetNode node = this.nodes.get(idx);
	//		if (node.beatmapSet.setId == beatmapSetId) {
	//			return node;
	//		}
	//	}
	//	return null;
	//}

	///**
	// * Attempts to find the VISIBLE beatmap in the specified set
	// * Will expand the setNode if the beatmap is in it
	// *
	// * @return the VISIBLE beatmap node or {@code null}
	// */
	//@Nullable
	//public BeatmapNode findMapNode(BeatmapSetNode setNode, int beatmapId)
	//{
	//	// TODO actually make this only search for visible :)
	//	int index = 0;
	//	for (Beatmap map : setNode.beatmapSet) {
	//		if (map.beatmapID == beatmapId) {
	//			return this.expand(setNode).getRelativeNode(index);
	//		}
	//		index++;
	//	}
	//	return null;
	//}

//	/**
//	 * Adds a song group.
//	 * @param beatmaps the list of beatmaps in the group
//	 * @return the new BeatmapSetNode
//	 */
//	public BeatmapSetNode addSongGroup(ArrayList<Beatmap> beatmaps) {
//		BeatmapSet beatmapSet = new BeatmapSet(beatmaps);
//		BeatmapSetNode node = new BeatmapSetNode(beatmapSet);
//		parsedNodes.add(node);
//		mapCount += beatmaps.size();
//
//		// add beatmap set ID to set
//		int msid = beatmaps.get(0).beatmapSetID;
//		if (msid > 0)
//			MSIDdb.add(msid);
//
//		// add MD5 hashes to table
//		for (Beatmap beatmap : beatmaps) {
//			if (beatmap.md5Hash != null)
//				beatmapHashDB.put(beatmap.md5Hash, beatmap);
//		}
//
//		return node;
//	}
//
//	/**
//	 * Deletes a song group from the list, and also deletes the beatmap
//	 * directory associated with the node.
//	 * @param node the set to delete
//	 * @return {@code true} if the song group was deleted
//	 */
//	public boolean deleteBeatmapSet(final BeatmapSetNode set)
//	{
//		if (this.expandedSet == set) {
//			this.unexpand();
//		}
//
//		// re-link base nodes and update index
//		final BeatmapNode prev = set.prev;
//		final BeatmapNode next = set.next;
//
//		if (prev != null) {
//			prev.next = next;
//		}
//
//		if (next != null) {
//			next.prev = prev;
//			final int didx = next.index - set.index;
//			BeatmapNode n = next;
//			do {
//				if (n == this.expandedSetFirstNode) {
//					n = this.expandedSetLastNode;
//				} else {
//					n.index -= didx;
//				}
//			} while ((n = n.next) != null);
//		}
//
//		// remove all node references
//		final BeatmapSet beatmapSet = set.beatmapSet;
//		final Beatmap beatmap = beatmapSet.get(0);
//		this.nodes.remove(set);
//		this.parsedNodes.remove(set);
//		this.groupNodes.remove(set);
//		this.mapCount -= beatmapSet.size();
//		if (beatmap.beatmapSetID > 0) {
//			MSIDdb.remove(beatmap.beatmapSetID);
//		}
//		for (Beatmap bm : beatmapSet) {
//			if (bm.md5Hash != null) {
//				this.beatmapHashDB.remove(bm.md5Hash);
//			}
//		}
//
//		// stop playing the track
//		if (MusicController.trackExists() || MusicController.isTrackLoading()) {
//			final File audioFile = MusicController.getBeatmap().audioFilename;
//			if (audioFile != null && audioFile.equals(beatmap.audioFilename)) {
//				MusicController.reset();
//				System.gc(); // files won't be deleted if this wasn't called
//			}
//		}
//
//		final File dir = beatmap.getFile().getParentFile();
//
//		// remove entry from cache
//		BeatmapDB.delete(dir.getName());
//
//		// delete the directory
//		BeatmapWatchService ws = BeatmapWatchService.get();
//		if (ws != null) {
//			ws.pause();
//		}
//		try {
//			Utils.deleteToTrash(dir);
//		} catch (IOException e) {
//			bubNotifs.send(BUB_ORANGE, "Failed to delete song group");
//			return false;
//		}
//		if (ws != null) {
//			ws.resume();
//		}
//
//		return true;
//	}
//
//	/**
//	 * Deletes a single beatmap from its set, and also deletes the beatmap file.
//	 * If this causes the song group to be empty, then the song group and
//	 * beatmap directory will be deleted altogether.
//	 *
//	 * @param node the node containing the song group to delete
//	 * @return {@code true} if the beatmap was deleted
//	 */
//	public boolean deleteBeatmap(BeatmapNode node)
//	{
//		if (node.setNode.beatmapSet.size() == 1) {
//			return this.deleteBeatmapSet(node.setNode);
//		}
//
//		// update indices
//		if (this.expandedSet == node.setNode) {
//			if (this.expandedSetFirstNode == node) {
//				this.expandedSetFirstNode = node.next;
//			} else if (this.expandedSetLastNode == node) {
//				this.expandedSetLastNode = node.prev;
//			} else {
//				BeatmapNode n = this.expandedSetFirstNode.next;
//				do {
//					if (n == node) {
//						BeatmapNode prev = n.prev;
//						n = n.next;
//						prev.next = n;
//						n.prev = prev;
//						do {
//							n.beatmapIndex--;
//						} while ((n = n.next) != null);
//						break;
//					}
//				} while ((n = n.next) != this.expandedSetLastNode);
//			}
//		}
//
//		// remove song reference
//		Beatmap beatmap = node.beatmapSet.remove(node.beatmapIndex);
//		mapCount--;
//		if (beatmap.md5Hash != null) {
//			beatmapHashDB.remove(beatmap.md5Hash);
//		}
//
//		// remove entry from cache
//		File file = beatmap.getFile();
//		BeatmapDB.delete(file.getParentFile().getName(), file.getName());
//
//		// delete the associated file
//		BeatmapWatchService ws = BeatmapWatchService.get();
//		if (ws != null) {
//			ws.pause();
//		}
//		try {
//			Utils.deleteToTrash(file);
//		} catch (IOException e) {
//			bubNotifs.send(BUB_ORANGE, "Could not delete song");
//		}
//		if (ws != null)
//			ws.resume();
//
//		return true;
//	}
//
//	/**
//	 * @param index the beatmap set index, ignoring expanded nodes
//	 * @return the {@link BeatmapSetNode} at the specified index, or {@code null}
//	 */
//	@Nullable
//	public BeatmapSetNode getBeatmapSetNode(int index)
//	{
//		if (index < 0 || this.nodes.size() <= index) {
//			return null;
//		}
//		return this.nodes.get(index);
//	}
//
//	/**
//	 * @return a random beatmapset node, might be {@code null} if there are no beatmaps
//	 */
//	@Nullable
//	public BeatmapSetNode getRandomSetNode()
//	{
//		return this.getBeatmapSetNode((int) (Math.random() * this.nodes.size()));
//	}
//
//	/**
//	 * This also expands the set of the node.
//	 * @return a random beatmap node, might be {@code null} if there are no beatmaps
//	 */
//	@Nullable
//	public BeatmapNode getRandomNode()
//	{
//		final BeatmapSetNode set = this.getRandomSetNode();
//		if (set == null) {
//			return null;
//		}
//		return this.expand(set).getRelativeNode(rand.nextInt(set.beatmapSet.size()));
//	}
//
//	/**
//	 * @return next beatmap in the same set (wraps around),
//	 *         or next beatmap if node is the only one in the set.
//	 *         {@code null} if node is the only map
//	 */
//	@Nullable
//	public BeatmapNode nextInSet(BeatmapNode node)
//	{
//		final BeatmapNode next = node.next;
//		if ((next == null || next.setNode != node.setNode) &&
//			node.prev.setNode == node.setNode)
//		{
//			return node.prev;
//		}
//		return next;
//	}
//
//	/**
//	 * @return next beatmapset or {@code null}
//	 */
//	@Nullable
//	public BeatmapSetNode nextSet(BeatmapNode node)
//	{
//		BeatmapNode next = node, prev = node;
//		for (;;) {
//			if (next != null) {
//				next = next.next;
//				if (next instanceof BeatmapSetNode) {
//					return (BeatmapSetNode) next;
//				}
//			}
//			if (prev != null) {
//				prev = prev.prev;
//				if (prev instanceof BeatmapSetNode) {
//					return (BeatmapSetNode) prev;
//				}
//			}
//			if (prev == null && next == null) {
//				return null;
//			}
//		}
//	}
//
//	/**
//	 * @return the index of the expanded node (or {@code -1} if nothing is expanded).
//	 */
//	public int getExpandedIndex()
//	{
//		if (this.expandedSet == null) {
//			return -1;
//		}
//		return this.expandedSet.index;
//	}
//
//	public int getNodesBetween()
//	{
//
//	}
//
//	/**
//	 * Expands the given {@link BeatmapSetNode}
//	 *
//	 * @param index index of the beatmap set node
//	 * @return the first {@Link BeatmapNode} of the newly-expanded nodes in the set
//	 */
//	public BeatmapNode expand(final BeatmapSetNode setNode)
//	{
//		if (this.expandedSet == setNode) {
//			return this.expandedSetFirstNode;
//		}
//
//		this.unexpand();
//
//		this.expandedSetFirstNode = this.expandedSetLastNode = null;
//		BeatmapNode nodeBefore = setNode.prev;
//		final BeatmapNode nodeAfter = setNode.next;
//		final Iterator<Beatmap> iter = setNode.beatmapSet.iterator();
//
//		// beatmapsets should really have at least one beatmap
//		int beatmapIndex = 0;
//		for (;;) {
//			final Beatmap beatmap = iter.next();
//			final BeatmapNode newNode = new BeatmapNode(setNode, beatmap, beatmapIndex);
//			++beatmapIndex;
//
//			if (this.expandedSetFirstNode == null) {
//				expandedSetFirstNode = newNode;
//			}
//
//			newNode.prev = nodeBefore;
//			if (nodeBefore != null) {
//				nodeBefore.next = newNode;
//			}
//			nodeBefore = newNode;
//
//			if (!iter.hasNext()) {
//				this.expandedSetLastNode = newNode;
//				newNode.next = nodeAfter;
//				if (nodeAfter != null) {
//					nodeAfter.prev = newNode;
//				}
//				break;
//			}
//		}
//
//		return this.expandedSetFirstNode;
//	}
//
//	/**
//	 * Undoes the current expansion, if any.
//	 */
//	private void unexpand()
//	{
//		if (this.expandedSet == null) {
//			return;
//		}
//
//		final BeatmapNode nodeBefore = this.expandedSetFirstNode.prev;
//		final BeatmapNode nodeAfter = this.expandedSetLastNode.next;
//
//		if (nodeBefore != null) {
//			nodeBefore.next = this.expandedSet;
//		}
//		if (nodeAfter != null) {
//			nodeAfter.prev = this.expandedSet;
//		}
//
//		this.expandedSet = null;
//		this.expandedSetFirstNode = this.expandedSetLastNode = null;
//	}
//
//	/**
//	 * Initializes the links in the list.
//	 */
//	public void init()
//	{
//		if (this.nodes.isEmpty()) {
//			return;
//		}
//
//		// sort the list
//		Collections.sort(nodes, BeatmapSortOrder.current().getComparator());
//		this.expandedSet = null;
//		this.expandedSetFirstNode = this.expandedSetLastNode = null;
//
//		// create links
//		BeatmapSetNode lastNode = nodes.get(0);
//		lastNode.index = 0;
//		lastNode.prev = null;
//		for (int i = 1, size = this.nodes.size(); i < size; i++) {
//			BeatmapSetNode node = nodes.get(i);
//			lastNode.next = node;
//			node.index = i;
//			node.prev = lastNode;
//
//			lastNode = node;
//		}
//		lastNode.next = null;
//	}
//
//	/**
//	 * Creates a new list of song groups in which each group contains a match to a search query.
//	 * @param query the search query (terms separated by spaces)
//	 * @return false if query is the same as the previous one, true otherwise
//	 */
//	public boolean search(String query) {
//		if (query == null)
//			return false;
//
//		// don't redo the same search
//		query = query.trim().toLowerCase();
//		if (lastQuery != null && query.equals(lastQuery))
//			return false;
//		lastQuery = query;
//		LinkedList<String> terms = new LinkedList<String>(Arrays.asList(query.split("\\s+")));
//
//		// if empty query, reset to original list
//		if (query.isEmpty() || terms.isEmpty()) {
//			nodes = groupNodes;
//			return true;
//		}
//
//		// find and remove any conditional search terms
//		LinkedList<String> condType     = new LinkedList<String>();
//		LinkedList<String> condOperator = new LinkedList<String>();
//		LinkedList<Float>  condValue    = new LinkedList<Float>();
//
//		Iterator<String> termIter = terms.iterator();
//		while (termIter.hasNext()) {
//			String term = termIter.next();
//			Matcher m = SEARCH_CONDITION_PATTERN.matcher(term);
//			if (m.find()) {
//				condType.add(m.group(1));
//				condOperator.add(m.group(2));
//				condValue.add(Float.parseFloat(m.group(3)));
//				termIter.remove();
//			}
//		}
//
//		// build an initial list from first search term
//		nodes = new ArrayList<BeatmapSetNode>();
//		if (terms.isEmpty()) {
//			// conditional term
//			String type = condType.remove();
//			String operator = condOperator.remove();
//			float value = condValue.remove();
//			for (BeatmapSetNode node : groupNodes) {
//				if (node.beatmapSet.matches(type, operator, value)) {
//					nodes.add(node);
//				}
//			}
//		} else {
//			// normal term
//			String term = terms.remove();
//			for (BeatmapSetNode node : groupNodes) {
//				if (node.beatmapSet.matches(term)) {
//					nodes.add(node);
//				}
//			}
//		}
//
//		// iterate through remaining normal search terms
//		while (!terms.isEmpty()) {
//			if (nodes.isEmpty())
//				return true;
//
//			String term = terms.remove();
//
//			// remove nodes from list if they don't match all terms
//			Iterator<BeatmapSetNode> nodeIter = nodes.iterator();
//			while (nodeIter.hasNext()) {
//				BeatmapSetNode node = nodeIter.next();
//				if (!node.beatmapSet.matches(term)) {
//					nodeIter.remove();
//				}
//			}
//		}
//
//		// iterate through remaining conditional terms
//		while (!condType.isEmpty()) {
//			if (nodes.isEmpty())
//				return true;
//
//			String type = condType.remove();
//			String operator = condOperator.remove();
//			float value = condValue.remove();
//
//			// remove nodes from list if they don't match all terms
//			Iterator<BeatmapSetNode> nodeIter = nodes.iterator();
//			while (nodeIter.hasNext()) {
//				BeatmapSetNode node = nodeIter.next();
//				if (!node.beatmapSet.matches(type, operator, value)) {
//					nodeIter.remove();
//				}
//			}
//		}
//
//		return true;
//	}

	/**
	 * Returns whether or not the list contains the given beatmap set ID.
	 * <p>
	 * Note that IDs for older maps might have been improperly parsed, so
	 * there is no guarantee that this method will return an accurate value.
	 * @param id the beatmap set ID to check
	 * @return true if id is in the list
	 */
	public boolean containsBeatmapSetID(int id)
	{
		return this.beatmapSetDb.contains(id);
	}

	/**
	 * Returns the beatmap associated with the given hash.
	 * @param beatmapHash the MD5 hash
	 * @return the associated beatmap, or {@code null} if no match was found
	 */
	public Beatmap getBeatmapFromHash(String beatmapHash) {
		return beatmapHashDB.get(beatmapHash);
	}
}