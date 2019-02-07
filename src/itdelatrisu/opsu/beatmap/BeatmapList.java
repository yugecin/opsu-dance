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
import yugecin.opsudance.beatmap.BeatmapSearcher;
import yugecin.opsudance.core.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static itdelatrisu.opsu.ui.Colors.*;
import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Keeps track of all maps, maps in active group and filtered using search query
 */
public class BeatmapList
{
	/**
	 * public read access only
	 */
	public final ArrayList<BeatmapSet> sets;
	/**
	 * public read access only
	 */
	public final ArrayList<Beatmap> maps;

	/**
	 * nodes in the current group (see {@link BeatmapGroup#current})
	 */
	private ArrayList<Beatmap> nodesInGroup;
	/**
	 * subcollection of {@link #nodesInGroup} that conforms to search
	 * may point to {@link #nodesInGroup} itself when no search is active
	 */
	public ArrayList<Beatmap> visibleNodes;

	private final HashMap<String, Beatmap> beatmapHashDB;
	private final HashSet<Integer> beatmapSetDb;

	private String lastSearchQuery = "";

	BeatmapList()
	{
		this.sets = new ArrayList<>();
		this.maps = new ArrayList<>();
		this.visibleNodes = this.nodesInGroup = new ArrayList<>();
		this.beatmapHashDB = new HashMap<>();
		this.beatmapSetDb = new HashSet<>();
	}

	public void activeGroupChanged()
	{
		this.visibleNodes = this.nodesInGroup = BeatmapGroup.current.filter(this.maps);
		final String searchQuery = this.lastSearchQuery;
		this.lastSearchQuery = "";
		this.search(searchQuery);
		this.resort();
	}

	public void resort()
	{
		this.nodesInGroup.sort(BeatmapSortOrder.current);
		if (this.visibleNodes != this.nodesInGroup) {
			this.visibleNodes.sort(BeatmapSortOrder.current);
		}
	}

	public boolean isEmpty()
	{
		return this.visibleNodes.isEmpty();
	}

	public int getBeatmapCount()
	{
		return this.maps.size();
	}

	public int getBeatmapSetCount()
	{
		return this.sets.size();
	}

	void addBeatmapSet(Beatmap[] beatmaps)
	{
		final BeatmapSet set = new BeatmapSet(beatmaps);
		this.sets.add(set);

		this.maps.ensureCapacity(this.maps.size() + beatmaps.length);
		for (Beatmap beatmap : beatmaps) {
			this.maps.add(beatmap);

			// TODO this will only work when this method was called in splash,
			//      since at any later point visibleNodes might not point to
			//      nodesInGroup
			// TODO check search condition here then, too
			this.visibleNodes.add(beatmap);

			beatmap.beatmapSet = set;
			if (beatmap.md5Hash != null) {
				beatmapHashDB.put(beatmap.md5Hash, beatmap);
			}
		}
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
	/**
	 * Deletes a song group from the list, and also deletes the beatmap
	 * directory associated with the node.
	 * @return {@code true} if the song group was deleted
	 */
	public boolean deleteBeatmapSet(BeatmapSet set)
	{
		if (set.setId > 0) {
			this.beatmapSetDb.remove(set.setId);
		}
		for (Beatmap bm : set.beatmaps) {
			if (bm.md5Hash != null) {
				this.beatmapHashDB.remove(bm.md5Hash);
			}
		}

		// stop playing the track
		if (MusicController.trackExists() || MusicController.isTrackLoading()) {
			final File audioFile = MusicController.getBeatmap().audioFilename;
			if (audioFile != null) {
				for (Beatmap bm : set.beatmaps) {
					if (audioFile.equals(bm.audioFilename)) {
						MusicController.reset();
						// files won't be deleted if gc wasn't called
						System.gc();
						break;
					}
				}
			}
		}

		final File dir = set.beatmaps[0].getFile().getParentFile();

		// remove entry from cache
		BeatmapDB.delete(dir.getName());

		// delete the directory
		BeatmapWatchService ws = BeatmapWatchService.get();
		if (ws != null) {
			ws.pause();
		}
		try {
			Utils.deleteMaybeToTrash(dir);
		} catch (IOException e) {
			bubNotifs.send(BUB_ORANGE, "Failed to delete song group");
			return false;
		}
		if (ws != null) {
			ws.resume();
		}

		return true;
	}

	/**
	 * Deletes a single beatmap from its set, and also deletes the beatmap file.
	 * If this causes the song group to be empty, then the song group and
	 * beatmap directory will be deleted altogether.
	 *
	 * @return {@code true} if the beatmap was deleted
	 */
	public boolean deleteBeatmap(Beatmap beatmap)
	{
		final BeatmapSet set = beatmap.beatmapSet;
		if (set.beatmaps.length == 1) {
			return this.deleteBeatmapSet(set);
		}

		final Beatmap[] newBeatmapsInSet = new Beatmap[set.beatmaps.length - 1];
		int idx = 0;
		for (Beatmap m : set.beatmaps) {
			if (m != beatmap) {
				newBeatmapsInSet[idx++] = m;
			}
		}
		set.beatmaps = newBeatmapsInSet;

		if (beatmap.md5Hash != null) {
			beatmapHashDB.remove(beatmap.md5Hash);
		}

		// remove entry from cache
		File file = beatmap.getFile();
		BeatmapDB.delete(file.getParentFile().getName(), file.getName());

		// stop playing the track
		if (MusicController.trackExists() || MusicController.isTrackLoading()) {
			final File audioFile = MusicController.getBeatmap().audioFilename;
			if (audioFile != null && audioFile.equals(beatmap.audioFilename)) {
				MusicController.reset();
				// files won't be deleted if gc wasn't called
				System.gc();
			}
		}

		// delete the associated file
		BeatmapWatchService ws = BeatmapWatchService.get();
		if (ws != null) {
			ws.pause();
		}
		try {
			Utils.deleteMaybeToTrash(file);
		} catch (IOException e) {
			bubNotifs.send(BUB_ORANGE, "Could not delete song");
		}
		if (ws != null)
			ws.resume();

		return true;
	}
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
	/**
	 * Creates a new list of song groups in which each group contains a match to a search query.
	 * @param query the search query (terms separated by spaces)
	 * @return false if query is the same as the previous one, true otherwise
	 */
	public boolean search(@Nullable String query)
	{
		if (query == null) {
			return false;
		}

		query = query.trim();
		if (this.lastSearchQuery != null && this.lastSearchQuery.equals(query)) {
			return false;
		}

		this.lastSearchQuery = query;

		this.visibleNodes = BeatmapSearcher.search(this.nodesInGroup, query);
		return true;
	}

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