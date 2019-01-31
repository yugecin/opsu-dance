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

import java.util.Comparator;

/**
 * Beatmap sorting orders.
 */
public abstract class BeatmapSortOrder implements Comparator<Beatmap>
{
	public static final BeatmapSortOrder TITLE = new BeatmapSortOrder("Title")
	{
		@Override
		public int compare(Beatmap v, Beatmap w)
		{
			return v.searchTitle.compareTo(w.searchTitle);
		}
	};

	public static final BeatmapSortOrder ARTIST = new BeatmapSortOrder("Artist")
	{
		@Override
		public int compare(Beatmap v, Beatmap w)
		{
			return v.searchArtist.compareTo(w.searchArtist);
		}
	};

	public static final BeatmapSortOrder CREATOR = new BeatmapSortOrder("Creator")
	{
		@Override
		public int compare(Beatmap v, Beatmap w) {
			return v.searchCreator.compareTo(w.searchCreator);
		}
	};

	public static final BeatmapSortOrder BPM = new BeatmapSortOrder("BPM")
	{
		@Override
		public int compare(Beatmap v, Beatmap w)
		{
			return Integer.compare(v.bpmMax, w.bpmMax);
		}
	};

	public static final BeatmapSortOrder LENGTH = new BeatmapSortOrder("Length")
	{
		@Override
		public int compare(Beatmap v, Beatmap w)
		{
			return Integer.compare(v.endTime, w.endTime);
		}
	};

	public static final BeatmapSortOrder DATE = new BeatmapSortOrder("Date added")
	{
		@Override
		public int compare(Beatmap v, Beatmap w) {
			return Long.compare(v.dateAdded, w.dateAdded);
		}
	};

	public static final BeatmapSortOrder PLAYS = new BeatmapSortOrder("Most played")
	{
		@Override
		public int compare(Beatmap v, Beatmap w)
		{
			return Integer.compare(v.playCount, w.playCount);
		}
	};

	public static BeatmapSortOrder[] VALUES = {
		TITLE,
		ARTIST,
		CREATOR,
		BPM,
		LENGTH,
		DATE,
		PLAYS,
	};
	public static BeatmapSortOrder current = TITLE;

	public final String name;

	private BeatmapSortOrder(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}