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

import itdelatrisu.opsu.db.BeatmapDB;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Data type containing all beatmaps in a beatmap set.
 */
public class BeatmapSet implements Iterable<Beatmap> {
	/** List of associated beatmaps. */
	private final ArrayList<Beatmap> beatmaps;

	/**
	 * Can be negative when it's improperly parsed for older maps.
	 */
	public final int setId;

	/**
	 * Constructor.
	 * @param beatmaps the beatmaps in this set, should not be empty
	 */
	public BeatmapSet(ArrayList<Beatmap> beatmaps) {
		this.beatmaps = beatmaps;
		this.setId = beatmaps.get(0).beatmapSetID;
	}

	/**
	 * Returns the number of elements.
	 */
	public int size() { return beatmaps.size(); }

	/**
	 * Returns the beatmap at the given index.
	 * @param index the beatmap index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public Beatmap get(int index) { return beatmaps.get(index); }

	/**
	 * Removes the beatmap at the given index.
	 * @param index the beatmap index
	 * @return the removed beatmap
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public Beatmap remove(int index) { return beatmaps.remove(index); }

	@Override
	public Iterator<Beatmap> iterator() { return beatmaps.iterator(); }

	/**
	 * Returns a formatted string for the beatmap set:
	 * "Artist - Title"
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Beatmap beatmap = beatmaps.get(0);
		return String.format("%s - %s", beatmap.getArtist(), beatmap.getTitle());
	}


	/**
	 * Returns whether this beatmap set is a "favorite".
	 */
	public boolean isFavorite() {
		for (Beatmap map : beatmaps) {
			if (map.favorite)
				return true;
		}
		return false;
	}

	/**
	 * Sets the "favorite" status of this beatmap set.
	 * @param flag whether this beatmap set should have "favorite" status
	 */
	public void setFavorite(boolean flag) {
		for (Beatmap map : beatmaps) {
			map.favorite = flag;
			BeatmapDB.updateFavoriteStatus(map);
		}
	}

	/**
	 * Returns whether any beatmap in this set has been played.
	 */
	public boolean isPlayed() {
		for (Beatmap map : beatmaps) {
			if (map.playCount > 0)
				return true;
		}
		return false;
	}
}
