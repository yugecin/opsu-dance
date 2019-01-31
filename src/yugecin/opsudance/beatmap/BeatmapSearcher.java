// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.beatmap;

import java.util.ArrayList;

import org.newdawn.slick.util.Log;

import itdelatrisu.opsu.beatmap.Beatmap;

public class BeatmapSearcher
{
	public static ArrayList<Beatmap> search(ArrayList<Beatmap> maps, String trimmedQuery)
	{
		if (trimmedQuery.isEmpty()) {
			return maps;
		}

		final long start = System.currentTimeMillis();

		final ArrayList<Beatmap> result = new ArrayList<>(maps.size());
		// this might not be very i18n friendly... :/
		final char[] search = trimmedQuery.toCharArray();
		int bufsize = 0;
		final String[] terms = new String[search.length / 2 + 1];
		int termsc = 0;

		for (int i = 0;; i++) {
			if (i == search.length || search[i] == ' ') {
				if (bufsize > 0) {
					terms[termsc++] = new String(search, i - bufsize, bufsize);
					bufsize = 0;
				}
				if (i == search.length) {
					break;
				}
				continue;
			}
			switch(search[i]) {
			default:
				bufsize++;
				break;
			}
		}

		nm: for (int i = 0, m = maps.size(); i < m; i++) {
			final Beatmap map = maps.get(i);
			for (int j = 0; j < termsc; j++) {
				final String t = terms[j];
				if (!(map.searchTitle.contains(t) ||
					map.searchArtist.contains(t) ||
					map.searchCreator.contains(t) ||
					map.searchSource.contains(t) ||
					map.searchVersion.contains(t) ||
					map.searchTitleUnicode.contains(t) ||
					map.searchArtistUnicode.contains(t) ||
					map.tags.contains(t)))
				{
					continue nm;
				}
			}
			result.add(map);
		}

		Log.debug("search: " + (System.currentTimeMillis() - start) + "ms");

		return result;
	}
}
