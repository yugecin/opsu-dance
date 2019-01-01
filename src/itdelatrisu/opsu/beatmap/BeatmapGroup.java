package itdelatrisu.opsu.beatmap;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.newdawn.slick.Image;

/**
 * Beatmap groups.
 */
public enum BeatmapGroup {
	/** All beatmaps (no filter). */
	ALL (0, "All Songs", null),

	/** Most recently played beatmaps. */
	RECENT (1, "Last Played", "Your recently played beatmaps will appear in this list!") {
		/** Number of elements to show. */
		private static final int K = 20;

		/** Returns the latest "last played" time in a beatmap set. */
		private long lastPlayed(BeatmapSet set) {
			long max = 0;
			for (Beatmap beatmap : set) {
				if (beatmap.lastPlayed > max)
					max = beatmap.lastPlayed;
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
	},

	/** "Favorite" beatmaps. */
	FAVORITE (2, "Favorites", "Right-click a beatmap to add it to your Favorites!") {
		@Override
		public ArrayList<Beatmap> filter(ArrayList<Beatmap> list) {
			// find "favorite" beatmaps
			ArrayList<Beatmap> filteredList = new ArrayList<Beatmap>();
			for (Beatmap node : list) {
				if (node.beatmapSet.isFavorite())
					filteredList.add(node);
			}
			return filteredList;
		}
	};

	/** The ID of the group (used for tab positioning). */
	private final int id;

	/** The name of the group. */
	private final String name;

	/** The message to display if this list is empty. */
	private final String emptyMessage;

	/** The tab associated with the group (displayed in Song Menu screen). */
	private MenuButton tab;

	/** Total number of groups. */
	private static final int SIZE = values().length;

	/** Array of BeatmapGroup objects in reverse order. */
	public static final BeatmapGroup[] VALUES_REVERSED;
	static {
		VALUES_REVERSED = values();
		Collections.reverse(Arrays.asList(VALUES_REVERSED));
	}

	/** Current group. */
	private static BeatmapGroup currentGroup = ALL;

	/**
	 * Returns the current group.
	 * @return the current group
	 */
	public static BeatmapGroup current() { return currentGroup; }

	/**
	 * Sets a new group.
	 * @param group the new group
	 */
	public static void set(BeatmapGroup group) { currentGroup = group; }

	/**
	 * Constructor.
	 * @param id the ID of the group (for tab positioning)
	 * @param name the group name
	 * @param emptyMessage the message to display if this list is empty
	 */
	BeatmapGroup(int id, String name, String emptyMessage) {
		this.id = id;
		this.name = name;
		this.emptyMessage = emptyMessage;
	}

	/**
	 * Returns the message to display if this list is empty.
	 * @return the message, or null if none
	 */
	public String getEmptyMessage() { return emptyMessage; }

	public ArrayList<Beatmap> filter(ArrayList<Beatmap> list)
	{
		return list;
	}

	/**
	 * Initializes the tab.
	 * @param containerWidth the container width
	 * @param bottomY the bottom y coordinate
	 */
	public void init(int containerWidth, float bottomY) {
		Image tab = GameImage.MENU_TAB.getImage();
		int tabWidth = tab.getWidth();
		float buttonX = containerWidth / 2f;
		float tabOffset = (containerWidth - buttonX - tabWidth) / (SIZE - 1);
		if (tabOffset > tabWidth) {  // prevent tabs from being spaced out
			tabOffset = tabWidth;
			buttonX = (containerWidth * 0.99f) - (tabWidth * SIZE);
		}
		this.tab = new MenuButton(tab,
				(buttonX + (tabWidth / 2f)) + (id * tabOffset),
				bottomY - (tab.getHeight() / 2f)
		);
	}

	/**
	 * Checks if the coordinates are within the image bounds.
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return true if within bounds
	 */
	public boolean contains(float x, float y) { return tab.contains(x, y); }

	/**
	 * Draws the tab.
	 * @param selected whether the tab is selected (white) or not (red)
	 * @param isHover whether to include a hover effect (unselected only)
	 */
	public void draw(boolean selected, boolean isHover) {
		UI.drawTab(tab.getX(), tab.getY(), name, selected, isHover);
	}
}
