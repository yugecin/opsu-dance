package itdelatrisu.opsu.beatmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Beatmap groups.
 */
public class BeatmapGroup
{
	public static final BeatmapGroup ALL = new BeatmapGroup("All Songs", null);

	public static final BeatmapGroup RECENT = new BeatmapGroup(
		"Last Played",
		"Your recently played beatmaps will appear in this list!")
	{
		/** Number of elements to show. */
		private static final int K = 20;

		/** Returns the latest "last played" time in a beatmap set. */
		private long lastPlayed(BeatmapSet set)
		{
			long max = 0;
			for (int i = 0; i < set.beatmaps.length; i++) {
				if (set.beatmaps[i].lastPlayed > max) {
					max = set.beatmaps[i].lastPlayed;
				}
			}
			return max;
		}

		@Override
		public ArrayList<Beatmap> filter(ArrayList<Beatmap> list) {
			// find top K elements
			PriorityQueue<Beatmap> pq = new PriorityQueue<Beatmap>(K, new Comparator<Beatmap>() {
				@Override
				public int compare(Beatmap v, Beatmap w) {
					return Long.compare(lastPlayed(v.beatmapSet), lastPlayed(w.beatmapSet));
				}
			});
			for (Beatmap node : list) {
				long timestamp = lastPlayed(node.beatmapSet);
				if (timestamp == 0)
					continue;  // skip unplayed beatmaps
				if (pq.size() < K || timestamp > lastPlayed(pq.peek().beatmapSet)) {
					if (pq.size() == K)
						pq.poll();
					pq.add(node);
				}
			}

			// return as list
			ArrayList<Beatmap> filteredList = new ArrayList<>();
			for (Beatmap node : pq) {
				filteredList.add(node);
			}
			return filteredList;
		}
	};

	public static final BeatmapGroup FAVORITE = new BeatmapGroup(
		"Favorites",
		"Right-click a beatmap to add it to your Favorites!")
	{
		@Override
		public ArrayList<Beatmap> filter(ArrayList<Beatmap> list) {
			// find "favorite" beatmaps
			ArrayList<Beatmap> filteredList = new ArrayList<Beatmap>();
			for (Beatmap node : list) {
				if (node.favorite) {
					filteredList.add(node);
				}
			}
			return filteredList;
		}
	};

	/**
	 * in reversed order because that's how they're drawn :)
	 */
	public static final BeatmapGroup[] GROUPS = {
		FAVORITE,
		RECENT,
		ALL,
	};

	/**
	 * should never be {@code null}
	 */
	public static BeatmapGroup current = ALL;

	public final String name;
	public final String emptyMessage;

	private BeatmapGroup(String name, String emptyMessage)
	{
		this.name = name;
		this.emptyMessage = emptyMessage;
	}

	/**
	 * may not modify or return given list
	 */
	public ArrayList<Beatmap> filter(ArrayList<Beatmap> list)
	{
		return new ArrayList<>(list);
	}
}
