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
			this.maps.remove(bm);
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
		this.maps.remove(beatmap);

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