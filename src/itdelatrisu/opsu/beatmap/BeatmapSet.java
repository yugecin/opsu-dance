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

/**
 * Data type containing all beatmaps in a beatmap set.
 */
public class BeatmapSet
{
	/** List of associated beatmaps. */
	public Beatmap[] beatmaps;

	/**
	 * Can be negative when it's improperly parsed for older maps.
	 */
	public final int setId;

	/**
	 * Constructor.
	 * @param beatmaps the beatmaps in this set, should not be empty
	 */
	public BeatmapSet(Beatmap[] beatmaps)
	{
		this.beatmaps = beatmaps;
		this.setId = beatmaps[0].beatmapSetID;
	}

	/**
	 * Returns a formatted string for the beatmap set:
	 * "Artist - Title"
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		Beatmap beatmap = beatmaps[0];
		return String.format("%s - %s", beatmap.getArtist(), beatmap.getTitle());
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
