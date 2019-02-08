// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.beatmap;

import java.util.ArrayList;

import org.newdawn.slick.util.Log;

import itdelatrisu.opsu.beatmap.Beatmap;

public class BeatmapSearcher
{
	private static final int[] OPMAP = { /*<*/0, /*<=*/1, /*=*/2, /*==*/2, /*>*/3, /*>=*/4 };

	private static final Tester[]
		AR = {
			(m, v) -> m.approachRate < v,
			(m, v) -> m.approachRate <= v,
			(m, v) -> m.approachRate == v,
			(m, v) -> m.approachRate > v,
			(m, v) -> m.approachRate >= v,
		},
		CS = {
			(m, v) -> m.circleSize < v,
			(m, v) -> m.circleSize <= v,
			(m, v) -> m.circleSize == v,
			(m, v) -> m.circleSize > v,
			(m, v) -> m.circleSize >= v,
		},
		OD = {
			(m, v) -> m.overallDifficulty < v,
			(m, v) -> m.overallDifficulty <= v,
			(m, v) -> m.overallDifficulty == v,
			(m, v) -> m.overallDifficulty > v,
			(m, v) -> m.overallDifficulty >= v,
		},
		HP = {
			(m, v) -> m.HPDrainRate < v,
			(m, v) -> m.HPDrainRate <= v,
			(m, v) -> m.HPDrainRate == v,
			(m, v) -> m.HPDrainRate > v,
			(m, v) -> m.HPDrainRate >= v,
		},
		BPM = {
			(m, v) -> m.bpmMax < v,
			(m, v) -> m.bpmMax <= v,
			(m, v) -> m.bpmMax == v,
			(m, v) -> m.bpmMax > v,
			(m, v) -> m.bpmMax >= v,
		},
		LENGTH = {
			(m, v) -> m.endTime < v,
			(m, v) -> m.endTime <= v,
			(m, v) -> m.endTime == v,
			(m, v) -> m.endTime > v,
			(m, v) -> m.endTime >= v,
		},
		STARS = {
			(m, v) -> Math.floor(m.starRating * 100) < v,
			(m, v) -> Math.floor(m.starRating * 100) <= v,
			(m, v) -> Math.floor(m.starRating * 100) == v,
			(m, v) -> Math.floor(m.starRating * 100) > v,
			(m, v) -> Math.floor(m.starRating * 100) >= v,
		};

	public static ArrayList<Beatmap> search(ArrayList<Beatmap> maps, String trimmedQuery)
	{
		if (trimmedQuery.isEmpty()) {
			return maps;
		}

		final long start = System.currentTimeMillis();

		final ArrayList<Beatmap> result = new ArrayList<>(maps.size());
		// this might not be very i18n friendly... :/
		final char[] search = trimmedQuery.toCharArray();
		int buflen = 0;
		final String[] terms = new String[search.length / 2 + 1];
		int termsc = 0;
		final ArrayList<Tester> testers = new ArrayList<>(terms.length);
		final float[] testervalues = new float[terms.length];

		for (int i = 0;; i++) {
			if (i == search.length || search[i] == ' ') {
				if (buflen > 0) {
					terms[termsc++] = new String(search, i - buflen, buflen);
					buflen = 0;
				}
				if (i == search.length) {
					break;
				}
				continue;
			}
			char c = search[i];
			if (c == '<' || c == '=' || c == '>') {
				Tester[] t = null;
				switch (buflen) {
				case 2:
					if (search[i - 2] == 'a' && search[i - 1] == 'r') {
						t = AR;
					} else if (search[i - 2] == 'c' && search[i - 1] == 's') {
						t = CS;
					} else if (search[i - 2] == 'o' && search[i - 1] == 'd') {
						t = OD;
					} else if (search[i - 2] == 'h' && search[i - 1] == 'p') {
						t = HP;
					}
					break;
				case 3:
					if (search[i - 3] == 'b' && search[i - 2] == 'p' &&
						search[i - 1] == 'm')
					{
						t = BPM;
					}
					break;
				case 4:
					if (search[i - 4] == 's' && search[i - 3] == 't' &&
						search[i - 2] == 'a' && search[i - 1] == 'r')
					{
						t = STARS;
					}
					break;
				case 5:
					if (search[i - 5] == 's' && search[i - 4] == 't' &&
						search[i - 3] == 'a' && search[i - 2] == 'r' &&
						search[i - 1] == 's')
					{
						t = STARS;
					}
					break;
				case 6:
					if (search[i - 6] == 'l' && search[i - 5] == 'e' &&
						search[i - 4] == 'n' && search[i - 3] == 'g' &&
						search[i - 2] == 't' && search[i - 1] == 'h')
					{
						t = LENGTH;
					}
					break;
				}
				if (t != null) {
					int op = (c - '<') * 2;
					if (++i != search.length && search[i] == '=') {
						op++;
						i++;
					}
					buflen = 0;
					float value = 0f;
					for (;; i++) {
						if (i == search.length || search[i] == ' ') {
							if (buflen == 0) {
								break;
							}
							final String s;
							s = new String(search, i - buflen, buflen);
							try {
								value += Float.parseFloat(s);
							} catch (NumberFormatException ignored) {
							}
							break;
						}
						if (search[i] == ':') {
							if (buflen == 0) {
								continue;
							}
							final String s;
							s = new String(search, i - buflen, buflen);
							value *= 60;
							try {
								value += Float.parseFloat(s) * 60;
							} catch (NumberFormatException ignored) {
							}
							buflen = 0;
							continue;
						}
						buflen++;
					}

					if (t == LENGTH) {
						value *= 1000;
					} else if (t == STARS) {
						value *= 100;
					}
					testervalues[testers.size()] = value;
					final Tester tester = t[OPMAP[op]];
					testers.add(tester);
					if (i == search.length) {
						break;
					}
					buflen = 0;
					continue;
				}
			}
			buflen++;
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
			for (int j = testers.size() - 1; j >= 0; j--) {
				if (!testers.get(j).test(map, testervalues[j])) {
					continue nm;
				}
			}
			result.add(map);
		}

		Log.debug("search: " + (System.currentTimeMillis() - start) + "ms");
		return result;
	}

	private static interface Tester
	{
		boolean test(Beatmap map, float value);
	}
}
