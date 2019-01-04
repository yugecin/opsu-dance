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

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.*;
import itdelatrisu.opsu.audio.MultiClip;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapDifficultyCalculator;
import itdelatrisu.opsu.beatmap.BeatmapGroup;
import itdelatrisu.opsu.beatmap.BeatmapNode;
import itdelatrisu.opsu.beatmap.BeatmapSet;
import itdelatrisu.opsu.beatmap.BeatmapSetNode;
import itdelatrisu.opsu.beatmap.BeatmapSortOrder;
import itdelatrisu.opsu.beatmap.BeatmapWatchService;
import itdelatrisu.opsu.beatmap.BeatmapWatchService.BeatmapWatchServiceListener;
import itdelatrisu.opsu.beatmap.LRUCache;
import itdelatrisu.opsu.db.BeatmapDB;
import itdelatrisu.opsu.db.ScoreDB;
import itdelatrisu.opsu.objects.curves.Vec2f;
import itdelatrisu.opsu.states.ButtonMenu.MenuState;
import itdelatrisu.opsu.states.game.Game.RestartReason;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.DropdownMenu;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.KineticScrolling;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SpriteSheet;
import org.newdawn.slick.gui.TextField;

import yugecin.opsudance.core.input.*;
import yugecin.opsudance.core.state.BaseOpsuState;

import static itdelatrisu.opsu.GameImage.*;
import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * "Song Selection" state.
 * <p>
 * Players are able to select a beatmap to play, view previous scores, choose game mods,
 * manage beatmaps, or change game options from this state.
 */
public class SongMenu extends BaseOpsuState
{
	/** The max number of score buttons to be shown at a time. */
	public static final int MAX_SCORE_BUTTONS = 7;

	/** Delay time, in milliseconds, between each search. */
	private static final int SEARCH_DELAY = 500;

	/** Delay time, in milliseconds, before moving to the beatmap menu after a right click. */
	private static final int BEATMAP_MENU_DELAY = 600;

	/** Time, in milliseconds, for the search bar to fade in or out. */
	private static final int SEARCH_TRANSITION_TIME = 250;

	/** Line width of the header/footer divider. */
	private static final int DIVIDER_LINE_WIDTH = 3;

	/** The first node is about this high above the header. */
	private KineticScrolling songScrolling = new KineticScrolling();
	private boolean isFastScrollingSongs;

	/** Current focus node's song information. */
	private String[] songInfo;

	/** The selection buttons. */
	private MenuButton selectModeButton, selectModsButton, selectRandomButton, selectMapOptionsButton;

	/** The search textfield. */
	private TextField searchTextField;

	/**
	 * Delay timer, in milliseconds, before running another search.
	 * This is overridden by character entry (reset) and 'esc' (immediate search).
	 */
	private int searchTimer = 0;

	private float searchRectHeight;

	/** Information text to display based on the search query. */
	private String searchResultString = null;

	/** Loader animation. */
	private Animation loader;

	/** Whether or not to reset game data upon entering the state. */
	private boolean resetGame = false;

	/** Whether or not to reset music track upon entering the state. */
	private boolean resetTrack = false;

	/** If non-null, determines the action to perform upon entering the state. */
	private MenuState stateAction;

	/** If non-null, the node that stateAction acts upon. */
	private Beatmap stateActionBeatmap;

	/** If non-null, the score data that stateAction acts upon. */
	private ScoreData stateActionScore;

	/** Timer before moving to the beatmap menu with the current focus node. */
	private int beatmapMenuTimer = -1;

	/** Beatmap reloading thread. */
	private BeatmapReloadThread reloadThread;

	/** Thread for reloading beatmaps. */
	private class BeatmapReloadThread extends Thread {
		/** If true, also clear the beatmap cache and invoke the unpacker. */
		private final boolean fullReload;

		/** Whether this thread has completed execution. */
		private boolean finished = false;

		/** Returns true only if this thread has completed execution. */
		public boolean isFinished() { return finished; }

		/**
		 * Constructor.
		 * @param fullReload if true, also clear the beatmap cache and invoke the unpacker
		 */
		public BeatmapReloadThread(boolean fullReload) {
			this.fullReload = fullReload;
		}

		@Override
		public void run() {
			try {
				reloadBeatmaps();
			} finally {
				finished = true;
			}
		}

		/** Reloads all beatmaps. */
		private void reloadBeatmaps() {
			if (fullReload) {
				BeatmapDB.clearDatabase();
				oszunpacker.unpackAll();
			}
			beatmapParser.parseAll();
		}
	}

	/** Current map of scores (Version, ScoreData[]). */
	private Map<String, ScoreData[]> scoreMap;

	/** Scores for the current focus node. */
	private ScoreData[] focusScores;

	/** Current start score (topmost score entry). */
	private KineticScrolling startScorePos = new KineticScrolling();

	/** Header and footer end and start y coordinates, respectively. */
	private float headerY, footerY;

	/** Footer pulsing logo button. */
	private MenuButton footerLogoButton;

	/** Size of the pulsing logo in the footer. */
	private float footerLogoSize;

	/** Time, in milliseconds, for fading the search bar. */
	private int searchTransitionTimer = SEARCH_TRANSITION_TIME;

	/** The text length of the last string in the search TextField. */
	private int lastSearchTextLength = -1;

	/** Whether the song folder changed (notified via the watch service). */
	private boolean songFolderChanged = false;

	/** The last background image. */
	private File lastBackgroundImage;

	/** Background alpha level (for fade-in effect). */
	private AnimatedValue bgAlpha = new AnimatedValue(800, 0f, .5f, AnimationEquation.OUT_QUAD);

	/** Timer for animations when a new song node is selected. */
	private AnimatedValue songChangeTimer = new AnimatedValue(900, 0f, 1f, AnimationEquation.LINEAR);

	/** Timer for the music icon animation when a new song node is selected. */
	private AnimatedValue musicIconBounceTimer = new AnimatedValue(350, 0f, 1f, AnimationEquation.LINEAR);

	/**
	 * Beatmaps whose difficulties were recently computed (if flag is non-null).
	 * Unless the Boolean flag is null, then upon removal, the beatmap's objects will
	 * be cleared (to be garbage collected). If the flag is true, also clear the
	 * beatmap's array fields (timing points, etc.).
	 */
	@SuppressWarnings("serial")
	private LRUCache<Beatmap, Boolean> beatmapsCalculated = new LRUCache<Beatmap, Boolean>(12) {
		@Override
		public void eldestRemoved(Map.Entry<Beatmap, Boolean> eldest) {
			Boolean b = eldest.getValue();
			if (b != null) {
				Beatmap beatmap = eldest.getKey();
				beatmap.objects = null;
				if (b) {
					beatmap.timingPoints = null;
					beatmap.breaks = null;
					beatmap.combo = null;
				}
			}
		}
	};

	/** Whether the menu is currently scrolling to the focus node (blocks other actions). */
	private boolean isScrollingToFocusNode = false;

	/** Sort order dropdown menu. */
	private DropdownMenu<BeatmapSortOrder> sortMenu;

	private final Runnable backButtonListener = this::exit;

	public SongMenu()
	{
	}

	@Override
	public void revalidate()
	{
		final float footerHeight = height * 0.116666666666f;

		// header/footer coordinates
		headerY = height * 0.0075f + GameImage.MENU_MUSICNOTE.getHeight() +
				Fonts.BOLD.getLineHeight() + Fonts.DEFAULT.getLineHeight() +
				Fonts.SMALL.getLineHeight();
		footerY = height - footerHeight;

		// footer logo coordinates
		footerLogoSize = footerHeight * 3.25f;
		Image logo = GameImage.MENU_LOGO.getImage();
		logo = logo.getScaledCopy(footerLogoSize / logo.getWidth());
		footerLogoButton = new MenuButton(logo, width - footerHeight * 0.8f, height - footerHeight * 0.65f);

		this.searchRectHeight = Fonts.BOLD.getLineHeight() * 2;

		// initialize sorts
		int sortWidth = (int) (width * 0.12f);
		int posX = (int) (width * 0.87f);
		int posY = (int) (headerY - GameImage.MENU_TAB.getHeight() * 2.25f);
		if (this.sortMenu != null) {
			this.sortMenu.closeReleaseFocus();
		}
		sortMenu = new DropdownMenu<BeatmapSortOrder>(BeatmapSortOrder.values(), posX, posY, sortWidth) {
			@Override
			public void itemSelected(int index, BeatmapSortOrder item) {
				/*
				BeatmapSortOrder.set(item);
				if (focusNode == null)
					return;
				BeatmapSetNode oldFocusBase = beatmapSetList.getBeatmapSetNode(focusNode.index);
				int oldFocusFileIndex = focusNode.beatmapIndex;
				focusNode = null;
				beatmapSetList.init();
				SongMenu.this.setFocus(oldFocusBase, oldFocusFileIndex, true);
				*/
				// TODO
			}

			@Override
			public boolean canSelect(int index) {
				if (isInputBlocked())
					return false;

				SoundController.playSound(SoundEffect.MENUCLICK);
				return true;
			}
		};
		sortMenu.setBackgroundColor(Colors.BLACK_BG_FOCUS);
		sortMenu.setBorderColor(Colors.BLUE_DIVIDER);

		// initialize group tabs
		for (BeatmapGroup group : BeatmapGroup.values())
			group.init(width, headerY - DIVIDER_LINE_WIDTH / 2);

		// initialize score data buttons
		ScoreData.init(width, headerY + height * 0.01f);

		final float shls = (footerLogoSize * .85f) / 2f; // small half logo size
		// distance logo center to border :)
		final float dlctb = (width - this.footerLogoButton.getX());
		nodeList.revalidate(
			headerY,
			footerY,
			headerY + this.searchRectHeight,
			this.footerLogoButton.getY() + shls * .01f -
				(float) Math.sqrt(shls * shls - dlctb * dlctb)
		);

		// search
		int textFieldX = (int) (width * 0.7125f + Fonts.BOLD.getWidth("Search: "));
		int textFieldY = (int) (headerY + Fonts.BOLD.getLineHeight() / 2);
		searchTextField = new TextField(Fonts.BOLD, textFieldX, textFieldY, (int) (width * 0.99f) - textFieldX, Fonts.BOLD.getLineHeight()) {
			@Override
			public boolean isFocusable() {
				return false;
			}
		};
		searchTextField.setBackgroundColor(Color.transparent);
		searchTextField.setBorderColor(Color.transparent);
		searchTextField.setTextColor(Color.white);
		searchTextField.setMaxLength(60);
		searchTextField.setFocused(true);

		// selection buttons
		// TODO: the origin should be bottomleft or something
		float selectX = width * (isWidescreen ? 0.164f : 0.1875f);
		final float footerButtonWidth = footerHeight * 0.85f;
		selectModeButton = new MenuButton(SELECTION_MODE_OVERLAY, selectX, footerY);
		selectX += footerHeight + 2;
		selectModsButton = new MenuButton(SELECTION_MODS_OVERLAY, selectX, footerY);
		selectX += footerButtonWidth;
		selectRandomButton = new MenuButton(SELECTION_RANDOM_OVERLAY, selectX, footerY);
		selectX += footerButtonWidth;
		selectMapOptionsButton = new MenuButton(SELECTION_OPTIONS_OVERLAY, selectX, footerY);
		selectModeButton.setHoverFade(0f);
		selectModsButton.setHoverFade(0f);
		selectRandomButton.setHoverFade(0f);
		selectMapOptionsButton.setHoverFade(0f);

		// loader
		int loaderDim = GameImage.MENU_MUSICNOTE.getWidth();
		SpriteSheet spr = new SpriteSheet(GameImage.MENU_LOADER.getImage(), loaderDim, loaderDim);
		loader = new Animation(spr, 50);

		// beatmap watch service listener
		BeatmapWatchService.addListener(new BeatmapWatchServiceListener() {
			@Override
			public void eventReceived(Kind<?> kind, Path child) {
				if (songFolderChanged || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					return;
				}
				songFolderChanged = true;
				if (!displayContainer.isIn(songMenuState)) {
					return;
				}
				barNotifs.send("Changes in Songs folder detected. Hit F5 to refresh.");
			}
		});
	}

	@Override
	public void render(Graphics g)
	{
		g.setBackground(Color.black);

		// background
		final Beatmap focusedMap = nodeList.getFocusedMap();
		if (focusedMap != null &&
			!focusedMap.drawBackground(width, height, bgAlpha.getValue(), true))
		{
			GameImage.PLAYFIELD.getImage().draw();
		}

		/*
		// song buttons & star stream
		BeatmapNode node = firstVisibleMapnode;
		int songButtonIndex = startNodeOffset;
		if (node != null && node.prev != null) {
			node = node.prev;
			songButtonIndex--;
		}
		starStream.pause();
		int starbtnidx = songButtonIndex;
		BeatmapNode starnode = node;
		while (starbtnidx < MAX_SONG_BUTTONS + 1 && starnode != null) {
			if (starnode == focusNode) {
				float ypos = buttonY + (starbtnidx * buttonOffset);
				ypos += buttonOffset / 2f;
				starStream.setPosition(width, ypos);
				starStream.resume();
				break;
			}
			
			starbtnidx++;
			starnode = starnode.next;
		}
		starStream.draw();
		while (songButtonIndex < MAX_SONG_BUTTONS + 1 && node != null) {
			// draw the node
			float offset = (node == hoverIndex) ? hoverOffset.getValue() : 0f;
			float ypos = buttonY + (songButtonIndex * buttonOffset);
			float mid = (height / 2) - ypos - (buttonOffset / 2);
			final float circleRadi = 700 * GameImage.getUIscale();
			//finds points along a very large circle  (x^2 = h^2 - y^2)
			float t = circleRadi * circleRadi - (mid * mid);
			float xpos = (float) ((t > 0) ? Math.sqrt(t) : 0) - circleRadi + 50 * GameImage.getUIscale();
			ScoreData[] scores = getScoreDataForNode(node, false);
			node.draw(buttonX - offset - xpos, ypos,
			          (scores == null) ? Grade.NULL : scores[0].getGrade(), (node == focusNode));

			songButtonIndex++;
			node = node.next;
		}
		*/

		/*
		// scroll bar
		if (focusNode != null && firstVisibleMapnode != null) {
			int focusNodes = focusNode.beatmapSet.size();
			int totalNodes = beatmapSetList.getBeatmapSetCount() + focusNodes - 1;
			if (totalNodes > MAX_SONG_BUTTONS) {
				UI.drawScrollbar(g,
						songScrolling.getPosition(),
						totalNodes * buttonOffset,
						MAX_SONG_BUTTONS * buttonOffset,
						width, headerY + DIVIDER_LINE_WIDTH / 2,
						0, MAX_SONG_BUTTONS * buttonOffset,
						Colors.BLACK_ALPHA, Color.white, true);
			}
		}
		*/

		// song nodes
		nodeList.render(g);

		// score buttons
		if (focusScores != null) {
			ScoreData.clipToArea(g);
			int startScore = (int) (startScorePos.getPosition() / ScoreData.getButtonOffset());
			int offset = (int) (-startScorePos.getPosition() + startScore * ScoreData.getButtonOffset());

			int scoreButtons = Math.min(focusScores.length - startScore, MAX_SCORE_BUTTONS + 1);
			float timerScale = 1f - (1 / 3f) * ((MAX_SCORE_BUTTONS - scoreButtons) / (float) (MAX_SCORE_BUTTONS - 1));
			int duration = (int) (songChangeTimer.getDuration() * timerScale);
			int segmentDuration = (int) ((2 / 3f) * songChangeTimer.getDuration());
			int time = songChangeTimer.getTime();
			for (int i = 0, rank = startScore; i < scoreButtons; i++, rank++) {
				if (rank < 0)
					continue;
				long prevScore = (rank + 1 < focusScores.length) ? focusScores[rank + 1].score : -1;
				float t = Utils.clamp((time - (i * (duration - segmentDuration) / scoreButtons)) / (float) segmentDuration, 0f, 1f);
				boolean focus = (t >= 0.9999f && ScoreData.buttonContains(mouseX, mouseY - offset, i));
				focusScores[rank].draw(g, offset + i * ScoreData.getButtonOffset(), rank, prevScore, focus, t);
			}
			g.clearClip();

			// scroll bar
			if (focusScores.length > MAX_SCORE_BUTTONS &&
				ScoreData.areaContains(mouseX, mouseY) &&
				!displayContainer.suppressHover)
			{
				ScoreData.drawScrollbar(g, startScorePos.getPosition(), focusScores.length * ScoreData.getButtonOffset());
			}
		}

		// top/bottom bars
		g.setColor(Color.black);
		g.fillRect(0, 0, width, headerY);
		g.fillRect(0, footerY, width, height - footerY);
		g.setColor(Colors.BLUE_DIVIDER);
		g.fillRect(0, headerY, width, DIVIDER_LINE_WIDTH);
		g.fillRect(0, footerY, width, DIVIDER_LINE_WIDTH);

		// footer logo (pulsing)
		final float fallbackProgress = System.currentTimeMillis() % 1000 / 1000f;
		final float position = MusicController.getBeatProgressOrDefault(fallbackProgress);
		if (footerLogoButton.isHovered()) {
			// hovering over logo: stop pulsing and scale
			footerLogoButton.draw(Color.white, 1.2f);
		} else {
			float expand = position * 0.15f;
			footerLogoButton.draw(Color.white, 1f - expand);
			Image ghostLogo = GameImage.MENU_LOGO.getImage();
			ghostLogo = ghostLogo.getScaledCopy((1f + expand) * footerLogoSize / ghostLogo.getWidth());
			ghostLogo.setAlpha(0.25f * (1f - position));
			ghostLogo.drawCentered(footerLogoButton.getX(), footerLogoButton.getY());
		}

		// header
		if (focusedMap != null) {
			// music/loader icon
			float marginX = width * 0.005f, marginY = height * 0.005f;
			Image musicNote = GameImage.MENU_MUSICNOTE.getImage();
			if (MusicController.isTrackLoading())
				loader.draw(marginX, marginY);
			else {
				float t = musicIconBounceTimer.getValue() * 2f;
				if (t > 1)
					t = 2f - t;
				float musicNoteScale = 1f + 0.3f * t;
				musicNote.getScaledCopy(musicNoteScale).drawCentered(marginX + musicNote.getWidth() / 2f, marginY + musicNote.getHeight() / 2f);
			}
			int iconWidth = musicNote.getWidth();

			// song info text
			if (songInfo == null) {
				songInfo = focusedMap.getInfo();
				// no need to load glyphs, since BeatmapNode does that already
			}
			marginX += 5;
			Color c = Colors.WHITE_FADE;
			float oldAlpha = c.a;
			float t = AnimationEquation.OUT_QUAD.calc(songChangeTimer.getValue());
			float headerTextY = marginY * 0.2f;
			c.a = Math.min(t * songInfo.length / 1.5f, 1f);
			if (c.a > 0)
				Fonts.LARGE.drawString(marginX + iconWidth * 1.05f, headerTextY, songInfo[0], c);
			headerTextY += Fonts.LARGE.getLineHeight() - 6;
			c.a = Math.min((t - 1f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0)
				Fonts.DEFAULT.drawString(marginX + iconWidth * 1.05f, headerTextY, songInfo[1], c);
			headerTextY += Fonts.DEFAULT.getLineHeight() - 2;
			c.a = Math.min((t - 2f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				float speedModifier = GameMod.getSpeedMultiplier();
				Color color2 = (speedModifier == 1f) ? c :
					(speedModifier > 1f) ? Colors.RED_HIGHLIGHT : Colors.BLUE_HIGHLIGHT;
				float oldAlpha2 = color2.a;
				color2.a = c.a;
				Fonts.BOLD.drawString(marginX, headerTextY, songInfo[2], color2);
				color2.a = oldAlpha2;
			}
			headerTextY += Fonts.BOLD.getLineHeight() - 4;
			c.a = Math.min((t - 3f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0)
				Fonts.DEFAULT.drawString(marginX, headerTextY, songInfo[3], c);
			headerTextY += Fonts.DEFAULT.getLineHeight() - 4;
			c.a = Math.min((t - 4f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				float multiplier = GameMod.getDifficultyMultiplier();
				Color color4 = (multiplier == 1f) ? c :
					(multiplier > 1f) ? Colors.RED_HIGHLIGHT : Colors.BLUE_HIGHLIGHT;
				float oldAlpha4 = color4.a;
				color4.a = c.a;
				Fonts.SMALL.drawString(marginX, headerTextY, songInfo[4], color4);
				color4.a = oldAlpha4;
			}
			c.a = oldAlpha;
		}

		// selection buttons
		Vec2f c;
		c = selectModeButton.bottomLeft();
		SELECTION_MODE.getImage().draw(c.x, c.y - SELECTION_MODE.getHeight());
		selectModeButton.draw();
		c = selectModsButton.bottomLeft();
		SELECTION_MODS.getImage().draw(c.x, c.y - SELECTION_MODS.getHeight());
		selectModsButton.draw();
		c = selectRandomButton.bottomLeft();
		SELECTION_RANDOM.getImage().draw(c.x, c.y - SELECTION_RANDOM.getHeight());
		selectRandomButton.draw();
		c = selectMapOptionsButton.bottomLeft();
		SELECTION_OPTIONS.getImage().draw(c.x, c.y - SELECTION_OPTIONS.getHeight());
		selectMapOptionsButton.draw();

		// group tabs
		BeatmapGroup currentGroup = BeatmapGroup.current();
		BeatmapGroup hoverGroup = null;
		if (!displayContainer.suppressHover) {
			for (BeatmapGroup group : BeatmapGroup.values()) {
				if (group.contains(mouseX, mouseY)) {
					hoverGroup = group;
					break;
				}
			}
		}
		for (BeatmapGroup group : BeatmapGroup.VALUES_REVERSED) {
			if (group != currentGroup)
				group.draw(false, group == hoverGroup);
		}
		currentGroup.draw(true, false);

		// search
		boolean searchEmpty = searchTextField.getText().isEmpty();
		int searchX = searchTextField.x;
		int searchY = searchTextField.y;
		float searchBaseX = width * 0.7f;
		float searchTextX = width * 0.7125f;
		float searchRectHeight = this.searchRectHeight;
		float searchExtraHeight = Fonts.DEFAULT.getLineHeight() * 0.7f;
		float searchProgress = (searchTransitionTimer < SEARCH_TRANSITION_TIME) ?
				((float) searchTransitionTimer / SEARCH_TRANSITION_TIME) : 1f;
		float oldAlpha = Colors.BLACK_ALPHA.a;
		if (searchEmpty) {
			searchRectHeight += (1f - searchProgress) * searchExtraHeight;
			Colors.BLACK_ALPHA.a = 0.5f - searchProgress * 0.3f;
		} else {
			searchRectHeight += searchProgress * searchExtraHeight;
			Colors.BLACK_ALPHA.a = 0.2f + searchProgress * 0.3f;
		}
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(searchBaseX, headerY + DIVIDER_LINE_WIDTH / 2, width - searchBaseX, searchRectHeight);
		Colors.BLACK_ALPHA.a = oldAlpha;
		Fonts.BOLD.drawString(searchTextX, searchY, "Search:", Colors.GREEN_SEARCH);
		if (searchEmpty) {
			Fonts.BOLD.drawString(searchX, searchY, "Type to search!", Color.white);
		} else {
			g.setColor(Color.white);
			searchTextField.render(g);
			Fonts.DEFAULT.drawString(searchTextX, searchY + Fonts.BOLD.getLineHeight(), (searchResultString == null) ? "Searching..." : searchResultString, Color.white);
		}

		this.sortMenu.render(g);

		// reloading beatmaps
		if (reloadThread != null) {
			displayContainer.disableBackButton = true;
			// darken the screen
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			UI.drawLoadingProgress(g);
		}
	}

	@Override
	public void preRenderUpdate()
	{
		int delta = renderDelta;
		UI.update(delta);
		if (reloadThread == null)
			MusicController.loopTrackIfEnded(true);
		else if (reloadThread.isFinished()) {
			BeatmapGroup.set(BeatmapGroup.ALL);
			BeatmapSortOrder.set(BeatmapSortOrder.TITLE);
			beatmapList.reset();
			nodeList.recreate();
			if (beatmapList.getBeatmapSetCount() > 0) {
				this.restoreFocusOrFocusRandom();
			} else {
				MusicController.playThemeSong();
			}
			reloadThread = null;
		}
		selectModeButton.hoverUpdate(delta, mouseX, mouseY);
		selectModsButton.hoverUpdate(delta, mouseX, mouseY);
		selectRandomButton.hoverUpdate(delta, mouseX, mouseY);
		selectMapOptionsButton.hoverUpdate(delta, mouseX, mouseY);
		footerLogoButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);

		this.sortMenu.updateHover(mouseX, mouseY);

		nodeList.preRenderUpdate();
		final Beatmap focusedMap = nodeList.getFocusedMap();

		// beatmap menu timer
		if (beatmapMenuTimer > -1) {
			beatmapMenuTimer += delta;
			if (beatmapMenuTimer >= BEATMAP_MENU_DELAY) {
				beatmapMenuTimer = -1;
				if (focusedMap != null) {
					MenuState state = focusedMap.beatmapSet.isFavorite() ?
						MenuState.BEATMAP_FAVORITE : MenuState.BEATMAP;
					buttonState.setMenuState(state, focusedMap);
					displayContainer.switchState(buttonState);
				}
				return;
			}
		}

		if (focusedMap != null) {
			// fade in background
			if (!focusedMap.isBackgroundLoading()) {
				bgAlpha.update(delta);
			}

			// song change timers
			songChangeTimer.update(delta);
			if (!MusicController.isTrackLoading()) {
				musicIconBounceTimer.update(delta);
			}
		}

		// search
		searchTimer += delta;
		if (searchTimer >= SEARCH_DELAY && reloadThread == null && beatmapMenuTimer == -1) {
			searchTimer = 0;

			/*
			 // TODO search
			if (beatmapSetList.search(searchTextField.getText())) {
				// empty search
				if (searchTextField.getText().isEmpty())
					searchResultString = null;

				// search produced new list: re-initialize it
				focusNode = null;
				scoreMap = null;
				focusScores = null;
				if (beatmapSetList.getBeatmapSetCount() > 0) {
					beatmapSetList.init();
					if (!searchTextField.getText().isEmpty()) {
						int size = beatmapSetList.getBeatmapSetCount();
						searchResultString = String.format("%d match%s found!",
								size, (size == 1) ? "" : "es");
					}
					this.restoreFocusOrFocusRandom();
				} else if (!searchTextField.getText().isEmpty())
					searchResultString = "No matches found. Hit ESC to reset.";
			}
			*/
		}
		if (searchTransitionTimer < SEARCH_TRANSITION_TIME) {
			searchTransitionTimer += delta;
			if (searchTransitionTimer > SEARCH_TRANSITION_TIME)
				searchTransitionTimer = SEARCH_TRANSITION_TIME;
		}

		// scores
		if (focusScores != null) {
			startScorePos.setMinMax(0, (focusScores.length - MAX_SCORE_BUTTONS) * ScoreData.getButtonOffset());
			startScorePos.update(delta);
		}

		// scrolling
		songScrolling.update(delta);
		/*
		if (isScrollingToFocusNode) {
			float distanceDiff = Math.abs(songScrolling.getPosition() - songScrolling.getTargetPosition());
			if (distanceDiff <= buttonOffset / 8f) {  // close enough, stop blocking input
				songScrolling.scrollToPosition(songScrolling.getTargetPosition());
				songScrolling.setSpeedMultiplier(1f);
				isScrollingToFocusNode = false;
			}
		}
		*/

		this.updateSongListPosition();

		// nodes mouse hover

		// tooltips
		if (displayContainer.suppressHover) {
			if (this.sortMenu.isHovered() && !this.sortMenu.isOpen()) {
				UI.updateTooltip(delta, "Sort by...", false);
			}
			return;
		}

		if (focusScores != null && ScoreData.areaContains(mouseX, mouseY)) {
			int startScore = (int) (startScorePos.getPosition() / ScoreData.getButtonOffset());
			int offset = (int) (-startScorePos.getPosition() + startScore * ScoreData.getButtonOffset());
			int scoreButtons = Math.min(focusScores.length - startScore, MAX_SCORE_BUTTONS);
			for (int i = 0, rank = startScore; i < scoreButtons; i++, rank++) {
				if (rank < 0)
					continue;
				if (ScoreData.buttonContains(mouseX, mouseY - offset, i)) {
					UI.updateTooltip(delta, focusScores[rank].getTooltipString(), true);
					break;
				}
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.button == Input.MMB) {
			return;
		}

		if (isScrollingToFocusNode) {
			return;
		}

		if (e.button == Input.RMB) {
			this.isFastScrollingSongs = true;
			songScrolling.setSpeedMultiplier(5f);
			this.updateFastSongScrollingPosition();
			return;
		}
		songScrolling.setSpeedMultiplier(1f);

		if (e.y <= headerY || footerY <= e.y) {
			return;
		}

		songScrolling.pressed();
		startScorePos.pressed();
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (e.button == Input.MMB) {
			return;
		}

		if (isScrollingToFocusNode) {
			return;
		}

		if (this.sortMenu.baseContains(e.x, e.y))
		{
			this.sortMenu.openGrabFocus();
			optionsOverlay.hide();
			e.consume();
			return;
		}

		if (this.isFastScrollingSongs) {
			if (e.button == Input.RMB) {
				this.isFastScrollingSongs = false;
			}
			return;
		}

		if (headerY < mousePressY && mousePressY < footerY) {
			songScrolling.released();
			startScorePos.released();
		}

		if (isInputBlocked()) {
			return;
		}

		final int x = e.x;
		final int y = e.y;

		// selection buttons
		if (selectModeButton.contains(x, y)) {
			barNotifs.send("There are no other modes available.");
			return;
		} else if (selectModsButton.contains(x, y)) {
			this.showMods();
			return;
		} else if (selectRandomButton.contains(x, y)) {
			this.selectRandomMap();
			return;
		} else if (selectMapOptionsButton.contains(x, y)) {
			this.showMapOptions();
			return;
		}

		// group tabs
		for (BeatmapGroup group : BeatmapGroup.values()) {
			if (!group.contains(x, y)) {
				continue;
			}
			if (group == BeatmapGroup.current()) {
				return;
			}
			BeatmapGroup.set(group);
			SoundController.playSound(SoundEffect.MENUCLICK);
			songInfo = null;
			scoreMap = null;
			focusScores = null;
			searchTextField.setText("");
			searchTimer = SEARCH_DELAY;
			searchTransitionTimer = SEARCH_TRANSITION_TIME;
			searchResultString = null;
			beatmapList.reset();
			nodeList.recreate();
			// TODO group tabs
			if (this.restoreFocusOrFocusRandom()) {
				nodeList.centerFocusedNodeNow();
			}

			/*
			 // TODO: filter stuff
			if (beatmapSetList.isEmpty() && group.getEmptyMessage() != null) {
				barNotifs.send(group.getEmptyMessage());
			}
			*/
			return;
		}

		if (nodeList.getFocusedMap() == null) {
			return;
		}

		// logo: start game
		if (footerLogoButton.contains(x, y, 0.25f)) {
			startGame();
			return;
		}

		// song buttons
		/*
		BeatmapNode node = getNodeAtPosition(x, y);
		if (node != null) {
			int expandedIndex = beatmapSetList.getExpandedIndex();

			// clicked node is already expanded
			if (node.index == expandedIndex) {
				if (node.beatmapIndex == focusNode.beatmapIndex) {
					// if already focused, load the beatmap
					if (e.button != Input.RMB)
						startGame();
					else
						SoundController.playSound(SoundEffect.MENUCLICK);
				} else {
					// focus the node
					SoundController.playSound(SoundEffect.MENUCLICK);
					this.setFocus(node, true);
				}
			}

			// clicked node is a new group
			else {
				SoundController.playSound(SoundEffect.MENUCLICK);
				this.setFocus(node, true);
			}

			// open beatmap menu
			if (e.button == Input.RMB)
				beatmapMenuTimer = (node.index == expandedIndex) ? BEATMAP_MENU_DELAY * 4 / 5 : 0;

			return;
		}
		*/

		// score buttons
		if (focusScores != null && ScoreData.areaContains(x, y)) {
			int startScore = (int) (startScorePos.getPosition() / ScoreData.getButtonOffset());
			int offset = (int) (-startScorePos.getPosition() + startScore * ScoreData.getButtonOffset());
			int scoreButtons = Math.min(focusScores.length - startScore, MAX_SCORE_BUTTONS);
			for (int i = 0, rank = startScore; i < scoreButtons; i++, rank++) {
				if (ScoreData.buttonContains(x, y - offset, i)) {
					SoundController.playSound(SoundEffect.MENUHIT);
					if (e.button != Input.RMB) {
						// view score
						gameRankingState.setGameData(new GameData(focusScores[rank]));
						displayContainer.switchState(gameRankingState);
					} else {
						// score management
						buttonState.setMenuState(MenuState.SCORE, focusScores[rank]);
						displayContainer.switchState(buttonState);
					}
					return;
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		// block input
		if ((reloadThread != null && e.keyCode != KEY_ESCAPE) ||
			beatmapMenuTimer > -1 || isScrollingToFocusNode)
		{
			return;
		}

		final Beatmap map = nodeList.getFocusedMap();

		switch (e.keyCode) {
		case KEY_ESCAPE:
			if (reloadThread != null) {
				// beatmap reloading: stop parsing beatmaps by sending interrupt to BeatmapParser
				reloadThread.interrupt();
			} else if (!searchTextField.getText().isEmpty()) {
				// clear search text
				searchTextField.setText("");
				searchTimer = SEARCH_DELAY;
				searchTransitionTimer = 0;
				searchResultString = null;
			} else {
				// return to main menu
				SoundController.playSound(SoundEffect.MENUBACK);
				displayContainer.switchState(mainmenuState);
			}
			return;
		case KEY_F1:
			this.showMods();
			return;
		case KEY_F2:
			this.selectRandomMap();
			return;
		case KEY_F3:
			this.showMapOptions();
			return;
			/*
			 * TODO: keystuff
		case KEY_F5:
			SoundController.playSound(SoundEffect.MENUHIT);
			if (songFolderChanged)
				reloadBeatmaps(false);
			else {
				buttonState.setMenuState(MenuState.RELOAD);
				displayContainer.switchState(buttonState);
			}
			return;
		case KEY_DELETE:
			if (map != null && input.isShiftDown()) {
				SoundController.playSound(SoundEffect.MENUHIT);
				MenuState ms = (focusNode.beatmapIndex == -1 || focusNode.beatmapSet.size() == 1) ?
						MenuState.BEATMAP_DELETE_CONFIRM : MenuState.BEATMAP_DELETE_SELECT;
				buttonState.setMenuState(ms, focusNode);
				displayContainer.switchState(buttonState);
			}
			return;
		case KEY_RETURN:
			if (map != null) {
				startGame();
			}
			return;
		case KEY_DOWN:
			nodeList.scrollButtonAmount(1f);
			return;
		case KEY_UP:
			nodeList.scrollButtonAmount(-1f);
			return;
		case KEY_RIGHT:
			if (nodeList.focusNextMap()) {
				SoundController.playSound(SoundEffect.MENUCLICK);
			}
			return;
		case KEY_LEFT:
			if (nodeList.focusPreviousMap()) {
				SoundController.playSound(SoundEffect.MENUCLICK);
			}
			return;
		case KEY_NEXT:
			nodeList.scrollPageDown();
			return;
		case KEY_PRIOR:
			nodeList.scrollPageUp();
			return;
			 */
		case KEY_O:
			if (reloadThread == null && input.isControlDown()) {
				optionsOverlay.show();
				return;
			}
		}

		// wait for user to finish typing
		// TODO: accept all characters (current conditions are from TextField class)
		if ((e.chr > 31 && e.chr < 127) || e.keyCode == KEY_BACK) {
			searchTimer = 0;
			searchTextField.keyPressed(e);
			int textLength = searchTextField.getText().length();
			if (lastSearchTextLength != textLength) {
				if (e.keyCode == KEY_BACK) {
					if (textLength == 0)
						searchTransitionTimer = 0;
				} else if (textLength == 1)
					searchTransitionTimer = 0;
				lastSearchTextLength = textLength;
			}
		}
	}

	private void showMods()
	{
		SoundController.playSound(SoundEffect.MENUHIT);
		buttonState.setMenuState(MenuState.MODS);
		displayContainer.switchState(buttonState);
	}

	private void selectRandomMap()
	{
		SoundController.playSound(SoundEffect.MENUHIT);

		if (!input.isShiftDown()) {
			this.setFocusToRandom();
			return;
		}

		// shift key: previous random track
		while (!songHistory.isEmpty() &&
			!nodeList.attemptFocusMap(songHistory.peek(), /*playAtPreviewTime*/ true));
	}

	private void showMapOptions()
	{
		final Beatmap map = nodeList.getFocusedMap();
		if (map == null) {
			return;
		}
		SoundController.playSound(SoundEffect.MENUHIT);
		MenuState state = map.beatmapSet.isFavorite() ?
			MenuState.BEATMAP_FAVORITE : MenuState.BEATMAP;
		buttonState.setMenuState(state, map);
		displayContainer.switchState(buttonState);
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
		if (e.button == Input.MMB) {
			return;
		}

		if (isInputBlocked()) {
			return;
		}

		if (e.dy == 0) {
			return;
		}

		if (this.isFastScrollingSongs) {
			if (e.button == Input.RMB) {
				this.updateFastSongScrollingPosition();
			}
			return;
		}

		if (mousePressY < headerY || footerY < mousePressY) {
			return;
		}

		if (focusScores != null &&
			focusScores.length >= MAX_SCORE_BUTTONS &&
			ScoreData.areaContains(mousePressX, mousePressY))
		{
			startScorePos.dragged(-e.dy * (Mouse.isButtonDown(Input.RMB) ? 10 : 1));
			return;
		}

		songScrolling.dragged(-e.dy);
	}

	private void updateFastSongScrollingPosition()
	{
		final float pos = -.05f + 1.1f * (mouseY - headerY) / (footerY - headerY);
		this.songScrolling.setPercentualPosition(pos);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (isInputBlocked()) {
			return;
		}

		if (this.isFastScrollingSongs) {
			return;
		}
		songScrolling.setSpeedMultiplier(1f);

		if (focusScores != null &&
			focusScores.length >= MAX_SCORE_BUTTONS &&
			ScoreData.areaContains(mouseX, mouseY))
		{
			// score buttons
			startScorePos.scrollOffset(ScoreData.getButtonOffset() * -e.direction);
		} else {
			nodeList.mouseWheelScroll(-e.direction);
		}
	}

	@Override
	public void enter() {
		super.enter();

		if (displayContainer.cursor.isBeatmapSkinned()) {
			displayContainer.cursor.reset();
		}

		UI.enter();
		selectModeButton.resetHover();
		selectModsButton.resetHover();
		selectRandomButton.resetHover();
		selectMapOptionsButton.resetHover();
		isScrollingToFocusNode = false;
		songScrolling.released();
		songScrolling.setSpeedMultiplier(1f);
		startScorePos.setPosition(0);
		beatmapMenuTimer = -1;
		searchTransitionTimer = SEARCH_TRANSITION_TIME;
		songInfo = null;
		bgAlpha.setTime(bgAlpha.getDuration());
		songChangeTimer.setTime(songChangeTimer.getDuration());
		musicIconBounceTimer.setTime(musicIconBounceTimer.getDuration());
		sortMenu.reset();

		if (songFolderChanged && stateAction != MenuState.RELOAD) {
			reloadBeatmaps(false);
		} else if (nodeList.getFocusedMap() == null &&
			beatmapList.getBeatmapSetCount() > 0)
		{
			this.setFocusToRandom();
		}

		// reset music track
		else if (resetTrack) {
			MusicController.pause();
			MusicController.playAt(MusicController.getBeatmap().previewTime, true);
			MusicController.setPitch(1.0f);
			resetTrack = false;
		}

		// unpause track
		else if (MusicController.isPaused())
			MusicController.resume();

		// undim track
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);

		nodeList.centerFocusedNodeNow();

		// reset game data
		if (resetGame) {
			gameState.resetGameData();

			// destroy extra Clips
			MultiClip.destroyExtraClips();

			// destroy skin images, if any
			for (GameImage img : GameImage.values()) {
				if (img.isBeatmapSkinnable())
					img.destroyBeatmapSkinImage();
			}

			/*
			 *  TODO: this score stuff
			// reload scores
			if (focusNode != null) {
				scoreMap = ScoreDB.getMapSetScores(focusNode.beatmap);
				focusScores = getScoreDataForNode(focusNode, true);
			}

			/*
			 * TODO: resort based on playcount
			// re-sort (in case play count updated)
			if (BeatmapSortOrder.current() == BeatmapSortOrder.PLAYS) {
				BeatmapSetNode oldFocusBase = beatmapist.getBeatmapSetNode(focusNode.index);
				int oldFocusFileIndex = focusNode.beatmapIndex;
				focusNode = null;
				beatmapSetList.init();
				this.setFocus(oldFocusBase, oldFocusFileIndex, true);
			}
			*/

			resetGame = false;
		}

		// state-based action
		if (stateAction != null) {
		/*
		 * TODO: this whole menu thing
			switch (stateAction) {
			case BEATMAP:  // clear all scores
				if (stateActionBeatmap == null || stateActionBeatmap.beatmapIndex == -1)
					break;
				Beatmap beatmap = stateActionBeatmap.beatmap;
				ScoreDB.deleteScore(beatmap);
				if (stateActionBeatmap == focusNode) {
					focusScores = null;
					scoreMap.remove(beatmap.version);
				}
				break;
			case SCORE:  // clear single score
				if (stateActionScore == null)
					break;
				ScoreDB.deleteScore(stateActionScore);
				scoreMap = ScoreDB.getMapSetScores(focusNode.beatmap);
				focusScores = getScoreDataForNode(focusNode, true);
				startScorePos.setPosition(0);
				break;
			case BEATMAP_DELETE_CONFIRM:  // delete song group
				if (stateActionBeatmap == null) {
					break;
				}
				/*
				 * TODO: delete
				beatmapList.deleteBeatmapSet(stateActionNode.setNode);
				this.setFocus(beatmapList.nextSet(stateActionNode), true);
				this.randomStack.remove(this.stateActionNode);
				break;
			case BEATMAP_DELETE_SELECT:  // delete single song
				if (stateActionBeatmap == null) {
					break;
				}
				/*
				 * TODO: delete
				this.setFocus(beatmapSetList.nextInSet(stateActionNode), true);
				beatmapSetList.deleteBeatmap(stateActionNode);
				this.randomStack.remove(this.stateActionNode);
				break;
			case RELOAD:  // reload beatmaps
				reloadBeatmaps(true);
				break;
			case BEATMAP_FAVORITE:  // removed favorite, reset beatmap list
				/*
				 * TODO: favorites
				if (BeatmapGroup.current() == BeatmapGroup.FAVORITE) {
					focusNode = null;
					songInfo = null;
					scoreMap = null;
					focusScores = null;
					beatmapSetList.reset();
					beatmapSetList.init();
					this.setFocusToRandom();
				}
				break;
			default:
				break;
			}
				 */
			stateAction = null;
			stateActionBeatmap = null;
			stateActionScore = null;
		}

		displayContainer.addBackButtonListener(this.backButtonListener);
	}

	@Override
	public void leave()
	{
		this.sortMenu.closeReleaseFocus();
		displayContainer.removeBackButtonListener(this.backButtonListener);
	}

	private void exit()
	{
		SoundController.playSound(SoundEffect.MENUBACK);
		displayContainer.switchState(mainmenuState);
	}

	/**
	 * Updates the song list data required for drawing.
	 */
	private void updateSongListPosition()
	{
	}

	/**
	 * @return {@code true} if focus was restored
	 */
	private boolean restoreFocusOrFocusRandom()
	{
		/*
		if (currentNode != null) {
			final BeatmapSet set = currentNode.beatmapSet;
			BeatmapSetNode setNode = beatmapSetList.findSetNode(set.setId);
			if (setNode != null) {
				this.setFocus(setNode, currentNode.beatmapIndex, true);
				return true;
			}
		}
		this.setFocusToRandom();
		*/
		return false;
	}

	/**
	 * Plays audio at preview time.
	 * Also expands.
	 */
	public void setFocusToRandom()
	{
		nodeList.focusRandomMap(/*playAtPreviewTime*/ true);
	}

	/**
	 * Sets a new focus node.
	 * @param node the beatmap set node; it will be expanded if it isn't already
	 * @param beatmapIndex the beatmap element to focus; or random one if out of bounds
	 * @param startMusicAtPreviewTime whether to start at the preview time
	 */
	public void setFocus(
		final BeatmapSetNode node,
		int beatmapIndex,
		boolean startMusicAtPreviewTime)
	{
		/*
		final BeatmapNode firstNode = beatmapSetList.expand(node);
		final int mapcount = node.beatmapSet.size();
		if (beatmapIndex < 0 || mapcount <= beatmapIndex) {
			beatmapIndex = (int) (Math.random() * (mapcount - 1));
		}

		final BeatmapNode actualNode = firstNode.getRelativeNode(beatmapIndex);
		this.setFocus(actualNode, startMusicAtPreviewTime);
		*/
	}

	/**
	 * Sets a new focus node.
	 * @return the old focus node
	 */
	public void setFocus(BeatmapNode node, boolean startMusicAtPreviewTime)
	{
		/*
		this.calculateStarRatings(node.beatmapSet);

		songInfo = null;
		songChangeTimer.setTime(0);
		musicIconBounceTimer.setTime(0);

		currentNode = focusNode = node;
		Beatmap beatmap = node.beatmap;
		if (beatmap.timingPoints == null) {
			// parse timing points so we can pulse the logo
			BeatmapParser.parseTimingPoints(beatmap);
		}
		MusicController.play(beatmap, false, startMusicAtPreviewTime);

		// load scores
		scoreMap = ScoreDB.getMapSetScores(beatmap);
		focusScores = this.getScoreDataForNode(focusNode, true);
		startScorePos.setPosition(0);

		this.updateSongListPosition();
		this.beatmapListRenderer.centerFocusedNodeSmooth();

		// load background image
		beatmap.loadBackground();
		boolean isBgNull = lastBackgroundImage == null || beatmap.bg == null;
		if ((isBgNull && lastBackgroundImage != beatmap.bg) ||
			(!isBgNull && !beatmap.bg.equals(lastBackgroundImage)))
		{
			bgAlpha.setTime(0);
			lastBackgroundImage = beatmap.bg;
		}
		*/
	}

	/**
	 * Triggers a reset of game data upon entering this state.
	 */
	public void resetGameDataOnLoad() { resetGame = true; }

	/**
	 * Triggers a reset of the music track upon entering this state.
	 */
	public void resetTrackOnLoad() { resetTrack = true; }

	/**
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 */
	public void doStateActionOnLoad(MenuState menuState)
	{
		this.doStateActionOnLoad(menuState, (Beatmap) null, (ScoreData) null);
	}

	/**
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 * @param beatmap the beatmap to perform the action on
	 */
	public void doStateActionOnLoad(MenuState menuState, Beatmap beatmap)
	{
		doStateActionOnLoad(menuState, beatmap, (ScoreData) null);
	}

	/**
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 * @param scoreData the score data to perform the action on
	 */
	public void doStateActionOnLoad(MenuState menuState, ScoreData scoreData)
	{
		doStateActionOnLoad(menuState, (Beatmap) null, scoreData);
	}

	/**
	 * Performs an action based on a menu state upon entering this state.
	 * @param menuState the menu state determining the action
	 * @param beatmap the song node to perform the action on
	 * @param scoreData the score data to perform the action on
	 */
	private void doStateActionOnLoad(MenuState menuState, Beatmap beatmap, ScoreData scoreData)
	{
		stateAction = menuState;
		stateActionBeatmap = beatmap;
		stateActionScore = scoreData;
	}

	/**
	 * Returns all the score data for an BeatmapSetNode from scoreMap.
	 * If no score data is available for the node, return null.
	 * @param node the BeatmapSetNode
	 * @param setTimeSince whether or not to set the "time since" field for the scores
	 * @return the ScoreData array
	 */
	private ScoreData[] getScoreDataForNode(BeatmapNode node, boolean setTimeSince) {
		if (scoreMap == null || scoreMap.isEmpty()) {
			return null;
		}

		Beatmap beatmap = node.beatmap;
		ScoreData[] scores = scoreMap.get(beatmap.version);
		if (scores == null || scores.length < 1)  // no scores
			return null;

		ScoreData s = scores[0];
		if (beatmap.beatmapID == s.MID && beatmap.beatmapSetID == s.MSID &&
		    beatmap.title.equals(s.title) && beatmap.artist.equals(s.artist) &&
		    beatmap.creator.equals(s.creator)) {
			if (setTimeSince) {
				for (int i = 0; i < scores.length; i++)
					scores[i].getTimeSince();
			}
			return scores;
		} else
			return null;  // incorrect map
	}

	/**
	 * Reloads all beatmaps.
	 * @param fullReload if true, also clear the beatmap cache and invoke the unpacker
	 */
	private void reloadBeatmaps(final boolean fullReload) {
		songFolderChanged = false;

		// reset state and node references
		MusicController.reset();
		scoreMap = null;
		focusScores = null;
		songInfo = null;
		searchTextField.setText("");
		searchTimer = SEARCH_DELAY;
		searchTransitionTimer = SEARCH_TRANSITION_TIME;
		searchResultString = null;
		lastBackgroundImage = null;

		// reload songs in new thread
		reloadThread = new BeatmapReloadThread(fullReload);
		reloadThread.start();
	}

	/**
	 * Returns whether a delayed/animated event is currently blocking user input.
	 * @return true if blocking input
	 */
	private boolean isInputBlocked() {
		return (reloadThread != null || beatmapMenuTimer > -1 || isScrollingToFocusNode);
	}

	/**
	 * Calculates all star ratings for a beatmap set.
	 * @param beatmapSet the set of beatmaps
	 */
	private void calculateStarRatings(BeatmapSet beatmapSet)
	{
		for (Beatmap beatmap : beatmapSet) {
			if (beatmap.starRating >= 0) {  // already calculated
				beatmapsCalculated.put(beatmap, beatmapsCalculated.get(beatmap));
				// ^ I guess this makes that it gets added as newest again, since
				// this is LRU cache?
				continue;
			}

			// if timing points are already loaded before this (for whatever reason),
			// don't clear the array fields to be safe
			boolean hasTimingPoints = (beatmap.timingPoints != null);

			BeatmapDifficultyCalculator diffCalc = new BeatmapDifficultyCalculator(beatmap);
			diffCalc.calculate();
			if (diffCalc.getStarRating() == -1)
				continue;  // calculations failed

			// save star rating
			beatmap.starRating = diffCalc.getStarRating();
			BeatmapDB.setStars(beatmap);
			beatmapsCalculated.put(beatmap, !hasTimingPoints);
		}
	}

	/**
	 * Starts the game.
	 */
	private void startGame()
	{
		if (MusicController.isTrackLoading())
			return;

		final Beatmap beatmap = MusicController.getBeatmap();
		final Beatmap focusedMap = nodeList.getFocusedMap();
		if (beatmap == null || beatmap != focusedMap) {
			barNotifs.send("Unable to load the beatmap audio.");
			return;
		}

		// turn on "auto" mod if holding "ctrl" key
		if (input.isControlDown() && !GameMod.AUTO.isActive()) {
			GameMod.AUTO.toggle(true);
		}

		optionsOverlay.hide();

		SoundController.playSound(SoundEffect.MENUHIT);
		MultiClip.destroyExtraClips();
		gameState.loadBeatmap(beatmap);
		gameState.setReplay(null);
		gameState.restart(RestartReason.NEWGAME);
	}
}
