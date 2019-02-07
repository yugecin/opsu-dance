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

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.gui.TextField;
import org.newdawn.slick.opengl.Texture;

import yugecin.opsudance.core.input.*;
import yugecin.opsudance.core.state.BaseOpsuState;
import yugecin.opsudance.ui.BackButton.Listener;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static org.lwjgl.input.Keyboard.*;
import static org.lwjgl.opengl.GL11.*;
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

	/** Sort order dropdown menu. */
	private DropdownMenu<BeatmapSortOrder> sortMenu;

	private final Listener backButtonListener = Listener.fromState(this::exit);

	private float headerImgW, headerImgH;
	private float extraHeaderW;

	private BeatmapGroup hoveredTab;
	private float tabW, tabH, tabOverlap, tabRightPadding, tabFontYOffset;

	private float sortTextX, sortTextY;

	private int selectionFlashTime = SELECTION_FLASH_TIME;
	private static final int SELECTION_FLASH_TIME = 666;

	public SongMenu()
	{
		OPTION_SHOW_UNICODE.addListener(() -> this.songInfo = null);
	}

	@Override
	public void revalidate()
	{
		this.tabW = width * 0.1f;
		this.tabOverlap = 0.13f * this.tabW;
		this.tabH = this.tabW * MENU_TAB.getHeight() / MENU_TAB.getWidth();
		this.tabRightPadding = width * 0.0125f;
		this.tabFontYOffset =
			this.tabH / 2f - Fonts.SMALLBOLD.getDescent()
			- (Fonts.SMALLBOLD.getAscent() - Fonts.SMALLBOLD.getDescent()) / 2f;

		final float footerHeight = height * 0.116666666666f;

		// header/footer coordinates
		this.headerY =
			(Fonts.LARGE.getLineHeight() - Fonts.LARGE.getDescent()) * 2
			+ Fonts.MEDIUM.getLineHeight();
		this.footerY = height - footerHeight;

		this.headerImgW = 9f * (this.headerImgH = 1.8913f * this.headerY);
		this.extraHeaderW = (float) Math.ceil(width - (int) this.headerImgW);

		// footer logo coordinates
		footerLogoSize = footerHeight * 3.25f;
		Image logo = GameImage.MENU_LOGO.getImage();
		logo = logo.getScaledCopy(footerLogoSize / logo.getWidth());
		footerLogoButton = new MenuButton(logo, width - footerHeight * 0.8f, height - footerHeight * 0.65f);

		this.searchRectHeight = Fonts.BOLD.getLineHeight() * 2;

		// initialize sorts
		int sortWidth = (int) (width * 0.12f);
		final float sortMenuX = width - this.tabRightPadding - sortWidth;
		this.sortTextX = sortMenuX - Fonts.LARGE.getWidth("Sort ");
		this.sortTextY = this.headerY - this.tabH - 2 - Fonts.LARGE.getAscent();
		final float sortMenuY =
			this.sortTextY + Fonts.LARGE.getDescent()
			+ (Fonts.LARGE.getAscent() - Fonts.LARGE.getDescent()) / 2f
			- Fonts.MEDIUM.getLineHeight() / 2f;
		if (this.sortMenu != null) {
			this.sortMenu.closeReleaseFocus();
		}
		this.sortMenu = new DropdownMenu<BeatmapSortOrder>(
			BeatmapSortOrder.VALUES,
			(int) sortMenuX,
			(int) sortMenuY,
			sortWidth)
		{
			@Override
			public void itemSelected(int index, BeatmapSortOrder item) {
				nodeList.unexpandAllExceptInSet(null);
				BeatmapSortOrder.current = item;
				beatmapList.resort();
				nodeList.processSort();
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
		sortMenu.setBorderColor(Colors.GREEN_SORT);
		sortMenu.setHighlightColor(Colors.GREEN_SORT);

		// initialize score data buttons
		ScoreData.init(width, headerY + height * 0.01f);

		final float shls = (footerLogoSize * .85f) / 2f; // small half logo size
		// distance logo center to border :)
		final float dlctb = (width - this.footerLogoButton.getX());
		nodeList.revalidate(
			headerY,
			footerY,
			(int) Math.ceil(headerY + this.headerImgH * 0.0287f) - 1
				+ this.searchRectHeight,
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
		// background
		dynBg.draw();
		final Beatmap activeMap = MusicController.getBeatmap();

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

		// header
		final Image mh = MENU_HEADER.getImage();
		final Texture mht = mh.getTexture();
		glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, mht.getTextureID());
		glBegin(GL_QUADS);
		glTexCoord2f(0f, 0f);
		glVertex2f(0f, 0f);
		glTexCoord2f(mht.getWidth(), 0f);
		glVertex2f(this.headerImgW, 0f);
		glTexCoord2f(mht.getWidth(), mht.getHeight());
		glVertex2f(this.headerImgW, this.headerImgH);
		glTexCoord2f(0f, mht.getHeight());
		glVertex2f(0f, this.headerImgH);
		glEnd();
		if (this.extraHeaderW > 0f) {
			final float mintexx = mht.getWidth() - 0.2f;
			glPushMatrix();
			glTranslated(Math.floor(this.headerImgW) - 1, 0d, 0d);
			glBegin(GL_QUADS);
			glTexCoord2f(0f, 0f);
			glVertex2f(mintexx, 0f);
			glTexCoord2f(mht.getWidth(), 0f);
			glVertex2f(this.extraHeaderW + 1f, 0f);
			glTexCoord2f(mht.getWidth(), mht.getHeight());
			glVertex2f(this.extraHeaderW + 1f, this.headerImgH);
			glTexCoord2f(mintexx, mht.getHeight());
			glVertex2f(0f, this.headerImgH);
			glEnd();
			glPopMatrix();
		}

		// bottom bar
		g.setColor(Color.black);
		g.fillRect(0, footerY, width, height - footerY);
		g.setColor(Colors.BLUE_DIVIDER);
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
		if (activeMap != null) {
			// music/loader icon
			float textY = -Fonts.LARGE.getDescent();
			final float pad = 6f;
			float t = IN_OUT_BACK.calc(musicIconBounceTimer.getValue());
			final float iconsize = Fonts.MEDIUM.getLineHeight();
			int _size = (int) (iconsize * (.5f + t / 2f));
			final Image icon = MENU_ICON.getImage().getScaledCopy(_size, _size);
			icon.drawCentered(pad + iconsize / 2f, textY + Fonts.LARGE.getAscent());
			float textX = pad + iconsize + pad;

			// song info text
			if (songInfo == null) {
				songInfo = activeMap.getInfo();
				if (OPTION_SHOW_UNICODE.state) {
					Fonts.loadGlyphs(Fonts.LARGE, activeMap.titleUnicode);
					Fonts.loadGlyphs(Fonts.LARGE, activeMap.artistUnicode);
				}
			}

			Color c = Colors.WHITE_FADE;
			float oldAlpha = c.a;
			t = OUT_QUAD.calc(songChangeTimer.getValue());
			c.a = Math.min(t * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				Fonts.LARGE.drawString(textX, textY, songInfo[0], c);
			}
			textY += Fonts.LARGE.getLineHeight() - 6;
			c.a = Math.min((t - 1f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				Fonts.DEFAULT.drawString(textX, textY, songInfo[1], c);
			}
			textX = pad;
			textY += Fonts.DEFAULT.getLineHeight();
			c.a = Math.min((t - 2f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				float speedModifier = GameMod.getSpeedMultiplier();
				Color color2 = (speedModifier == 1f) ? c :
					(speedModifier > 1f) ? Colors.RED_HIGHLIGHT : Colors.BLUE_HIGHLIGHT;
				float oldAlpha2 = color2.a;
				color2.a = c.a;
				Fonts.BOLD.drawString(textX, textY, songInfo[2], color2);
				color2.a = oldAlpha2;
			}
			textY += Fonts.BOLD.getLineHeight() - 4;
			c.a = Math.min((t - 3f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0)
				Fonts.DEFAULT.drawString(textX, textY, songInfo[3], c);
			textY += Fonts.DEFAULT.getLineHeight() - 4;
			c.a = Math.min((t - 4f / (songInfo.length * 1.5f)) * songInfo.length / 1.5f, 1f);
			if (c.a > 0) {
				float multiplier = GameMod.getDifficultyMultiplier();
				Color color4 = (multiplier == 1f) ? c :
					(multiplier > 1f) ? Colors.RED_HIGHLIGHT : Colors.BLUE_HIGHLIGHT;
				float oldAlpha4 = color4.a;
				color4.a = c.a;
				Fonts.SMALL.drawString(textX, textY, songInfo[4], color4);
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

		// text above tabs
		Fonts.LARGE.drawString(this.sortTextX, this.sortTextY, "Sort", Colors.GREEN_SORT);

		// group tabs
		this.hoveredTab = null;
		final float tabOffset = this.tabW - this.tabOverlap;
		float tabX = width
			- this.tabRightPadding - this.tabOverlap
			- tabOffset * BeatmapGroup.GROUPS.length;
		float activeTabX = 0f;
		float tabXHoverOffset = 0f; // because active tab overlaps
		final float tabTextXOffset = this.tabW / 2f;
		final boolean tabYhovr =
			!displayContainer.suppressHover &&
			this.headerY - this.tabH < mouseY && mouseY < this.headerY;
		final Texture textab = MENU_TAB.getImage().getTexture();
		glPushMatrix();
		glTranslatef(tabX, this.headerY - this.tabH, 0f);
		//RED_HOVER       = new Color(255, 112, 112),
		for (BeatmapGroup group : BeatmapGroup.GROUPS) {
			if (group != BeatmapGroup.current) {
				glBindTexture(GL_TEXTURE_2D, textab.getTextureID());
				if (tabYhovr &&
					tabX + tabXHoverOffset < mouseX &&
					mouseX < tabX + tabOffset)
				{
					this.hoveredTab = group;
					glColor3f(1f, .44f, .44f);
				} else {
					glColor3f(1f, 0f, 0f);
				}
				glBegin(GL_QUADS);
				glTexCoord2f(0f, 0f);
				glVertex2f(0f, 0f);
				glTexCoord2f(textab.getWidth(), 0f);
				glVertex2f(this.tabW, 0f);
				glTexCoord2f(textab.getWidth(), textab.getHeight());
				glVertex2f(this.tabW, this.tabH);
				glTexCoord2f(0f, textab.getHeight());
				glVertex2f(0f, this.tabH);
				glEnd();
				tabXHoverOffset = 0f;
			} else {
				activeTabX = tabX;
				tabXHoverOffset = this.tabOverlap;
			}
			final float textOff = Fonts.SMALLBOLD.getWidth(group.name) / 2f;
			Fonts.SMALLBOLD.drawString(
				tabTextXOffset - textOff,
				this.tabFontYOffset,
				group.name
			);
			glTranslatef(tabOffset, 0f, 0f);
			tabX += tabOffset;
		}
		glPopMatrix();
		glPushMatrix();
		glTranslatef(activeTabX, this.headerY - this.tabH, 0f);
		glBindTexture(GL_TEXTURE_2D, textab.getTextureID());
		// TODO this color should ideally be eased
		glColor3f(1f, 1f, 1f);
		glBegin(GL_QUADS);
		glTexCoord2f(0f, 0f);
		glVertex2f(0f, 0f);
		glTexCoord2f(textab.getWidth(), 0f);
		glVertex2f(this.tabW, 0f);
		glTexCoord2f(textab.getWidth(), textab.getHeight());
		glVertex2f(this.tabW, this.tabH);
		glTexCoord2f(0f, textab.getHeight());
		glVertex2f(0f, this.tabH);
		glEnd();
		final String tabtext = BeatmapGroup.current.name;
		final float textOff = Fonts.SMALLBOLD.getWidth(tabtext) / 2f;
		Fonts.SMALLBOLD.drawString(
			tabTextXOffset - textOff,
			this.tabFontYOffset,
			tabtext,
			Color.black // TODO this color should ideally be eased too
		);
		glPopMatrix();

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
		final int searchRectY = (int) Math.ceil(headerY + this.headerImgH * 0.0287f) - 1;
		g.fillRect(searchBaseX, searchRectY, width - searchBaseX, searchRectHeight);
		Colors.BLACK_ALPHA.a = oldAlpha;
		Fonts.BOLD.drawString(searchTextX, searchY, "Search:", Colors.GREEN_SEARCH);
		if (searchEmpty) {
			Fonts.BOLD.drawString(searchX, searchY, "Type to search!", Color.white);
		} else {
			g.setColor(Color.white);
			searchTextField.render(g);
			final int y = searchY + Fonts.BOLD.getLineHeight();
			String txt;
			if ((txt = searchResultString) == null) {
				txt = "Searching...";
			}
			Color.white.a = searchProgress;
			Fonts.SMALLBOLD.drawString(searchTextX, y, txt, Color.white);
			Color.white.a = 1f;
		}

		// dropdowns above tabs
		this.sortMenu.render(g);

		// reloading beatmaps
		if (reloadThread != null) {
			displayContainer.disableBackButton = true;
			// darken the screen
			g.setColor(Colors.BLACK_ALPHA);
			g.fillRect(0, 0, width, height);

			UI.drawLoadingProgress(g);
		}

		if (this.selectionFlashTime < SELECTION_FLASH_TIME) {
			if ((this.selectionFlashTime += renderDelta) > SELECTION_FLASH_TIME) {
				this.selectionFlashTime = SELECTION_FLASH_TIME;
			}
			float alpha = (float) this.selectionFlashTime / SELECTION_FLASH_TIME;
			if (alpha < .1f) {
				alpha = IN_CUBIC.calc(alpha * 10f);
			} else {
				alpha = 1f - OUT_QUAD.calc((alpha - .1f) / .9f);
			}
			glDisable(GL_TEXTURE_2D);
			glColor4f(1f, 1f, 1f, alpha * .075f);
			glBegin(GL_QUADS);
			glVertex2i(0, 0);
			glVertex2i(width, 0);
			glVertex2i(width, height);
			glVertex2i(0, height);
			glEnd();
		}
	}

	@Override
	public void preRenderUpdate()
	{
		dynBg.update();

		int delta = renderDelta;
		UI.update(delta);
		if (reloadThread == null)
			MusicController.loopTrackIfEnded(true);
		else if (reloadThread.isFinished()) {
			BeatmapGroup.current = BeatmapGroup.ALL;
			BeatmapSortOrder.current = BeatmapSortOrder.TITLE;
			beatmapList.activeGroupChanged();
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

		// search
		searchTimer += delta;
		if (searchTimer >= SEARCH_DELAY && reloadThread == null && beatmapMenuTimer == -1) {
			searchTimer = 0;

			final String searchText = searchTextField.getText();
			if (beatmapList.search(searchText)) {
				// empty search
				final boolean emptysearch = searchText.isEmpty();
				if (emptysearch) {
					searchResultString = null;
				}

				this.updateSearchText();
				nodeList.processSearch();
				if (nodeList.getFocusedMap() == null) {
					scoreMap = null;
					focusScores = null;
				}
			}
		}
		if (searchTransitionTimer < SEARCH_TRANSITION_TIME) {
			searchTransitionTimer += delta;
			if (searchTransitionTimer > SEARCH_TRANSITION_TIME)
				searchTransitionTimer = SEARCH_TRANSITION_TIME;
		}

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
			// song change timers
			songChangeTimer.update(delta);
			if (!MusicController.isTrackLoading()) {
				musicIconBounceTimer.update(delta);
			}
		}

		// scores
		if (focusScores != null) {
			startScorePos.setMinMax(0, (focusScores.length - MAX_SCORE_BUTTONS) * ScoreData.getButtonOffset());
			startScorePos.update(delta);
		}

		// nodes mouse hover

		// tooltips
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

		if (nodeList.mousePressed(e)) {
			return;
		}
		startScorePos.pressed();
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (e.button == Input.MMB) {
			return;
		}

		if (nodeList.mouseReleased(e)) {
			return;
		}

		startScorePos.released();

		if (this.sortMenu.baseContains(e.x, e.y))
		{
			this.sortMenu.openGrabFocus();
			optionsOverlay.hide();
			e.consume();
			return;
		}

		if (e.dragDistance > 5f) {
			return;
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
		if (this.hoveredTab != null) {
			BeatmapGroup.current = this.hoveredTab;
			this.hoveredTab = null;
			SoundController.playSound(SoundEffect.MENUCLICK);
			beatmapList.activeGroupChanged();
			nodeList.recreate();
			nodeList.reFadeIn();
			this.updateSearchText();
			if (nodeList.getFocusedMap() == null) {
				scoreMap = null;
				focusScores = null;
			}
			if (beatmapList.isEmpty() && BeatmapGroup.current.emptyMessage != null) {
				barNotifs.send(BeatmapGroup.current.emptyMessage);
			}
			return;
		}

		final Beatmap map = nodeList.getFocusedMap();

		// logo: start game
		if (map != null && footerLogoButton.contains(x, y, 0.25f)) {
			startGame();
			return;
		}

		if (headerY < e.downY && e.downY < footerY && e.button == Input.LMB) {
			if (nodeList.isHoveredNodeFocusedNode()) {
				this.startGame();
				return;
			}
			if (nodeList.focusHoveredNode()) {
				this.songInfo = null;
				this.songChangeTimer.setTime(0);
				this.musicIconBounceTimer.setTime(0);
				this.selectionFlashTime = 0;
				SoundController.playSound(SoundEffect.MENUCLICK);
			}
			return;
		}

		if (map == null) {
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
		if ((reloadThread != null && e.keyCode != KEY_ESCAPE) || beatmapMenuTimer > -1)
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
			*/
		case KEY_RETURN:
			if (nodeList.pressedEnterShouldGameBeStarted()) {
				this.startGame();
			}
			return;
		case KEY_DOWN:
		case KEY_UP:
		case KEY_RIGHT:
		case KEY_LEFT:
		case KEY_NEXT:
		case KEY_PRIOR:
			final Beatmap bm = MusicController.getBeatmap();
			SoundController.playSound(SoundEffect.MENUCLICK);
			if (nodeList.navigationKeyPressed(e.keyCode)) {
				this.selectionFlashTime = 0;
			}
			if (bm != MusicController.getBeatmap()) {
				this.musicIconBounceTimer.setTime(0);
			}
			return;
		case KEY_O:
			if (reloadThread == null && input.isControlDown()) {
				optionsOverlay.show();
				return;
			}
		}

		// wait for user to finish typing
		if (e.chr > 31 || e.keyCode == KEY_BACK) {
			if (e.chr > 255) {
				Fonts.loadGlyphs(searchTextField.font, e.chr);
			}
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

	private void updateSearchText()
	{
		if (beatmapList.visibleNodes.isEmpty()) {
			searchResultString = "No matches found. Hit ESC to reset.";
			scoreMap = null;
			focusScores = null;
			return;
		}

		if (!searchTextField.getText().isEmpty()) {
			final int s = beatmapList.visibleNodes.size();
			if (s == 1) {
				searchResultString = "1 match found!";
			} else {
				searchResultString = s + " matches found!";
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
		this.selectRandomMap0();
		if (nodeList.getFocusedMap() != null) {
			this.songInfo = null;
			this.songChangeTimer.setTime(0);
		}
	}

	private void selectRandomMap0()
	{
		if (!input.isShiftDown()) {
			while (!nextSongs.isEmpty()) {
				final Beatmap map = nextSongs.peek();
				if (nodeList.attemptFocusMap(map, /*playAtPreviewTime*/ true)) {
					this.musicIconBounceTimer.setTime(0);
					return;
				}
			}
			this.setFocusToRandom();
			return;
		}

		// shift key: previous random track
		while (!songHistory.isEmpty() &&
			!nodeList.attemptFocusMap(songHistory.peek(), /*playAtPreviewTime*/ true));
		this.musicIconBounceTimer.setTime(0);
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

		if (nodeList.mouseDragged(e)) {
			return;
		}

		if (e.dy == 0) {
			return;
		}

		if (e.downY < headerY || footerY < e.downY) {
			return;
		}

		if (focusScores != null &&
			focusScores.length >= MAX_SCORE_BUTTONS &&
			ScoreData.areaContains(e.downX, e.downY))
		{
			startScorePos.dragged(-e.dy * (Mouse.isButtonDown(Input.RMB) ? 10 : 1));
			return;
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (isInputBlocked()) {
			return;
		}

		if (focusScores != null &&
			focusScores.length >= MAX_SCORE_BUTTONS &&
			ScoreData.areaContains(mouseX, mouseY))
		{
			// score buttons
			startScorePos.scrollOffset(ScoreData.getButtonOffset() * -e.direction);
		} else {
			nodeList.mouseWheelMoved(e);
		}
	}

	@Override
	public void enter() {
		super.enter();

		if (displayContainer.cursor.isBeatmapSkinned()) {
			displayContainer.cursor.reset();
		}

		dynBg.songChanged(); // in case dynamic bg in main menu is disabled
		dynBg.fadeInNow();
		UI.enter();
		selectModeButton.resetHover();
		selectModsButton.resetHover();
		selectRandomButton.resetHover();
		selectMapOptionsButton.resetHover();
		startScorePos.setPosition(0);
		beatmapMenuTimer = -1;
		searchTransitionTimer = SEARCH_TRANSITION_TIME;
		songInfo = null;
		songChangeTimer.setTime(songChangeTimer.getDuration());
		musicIconBounceTimer.setTime(musicIconBounceTimer.getDuration());
		sortMenu.reset();

		if (songFolderChanged && stateAction != MenuState.RELOAD) {
			reloadBeatmaps(false);
		} else if (nodeList.getFocusedMap() == null && MusicController.isThemePlaying()) {
			this.setFocusToRandom();
		}

		// reset music track
		else if (resetTrack || MusicController.isPaused()) {
			MusicController.pause();
			MusicController.playAt(MusicController.getBeatmap().previewTime, true);
			MusicController.setPitch(1.0f);
			resetTrack = false;
		}

		// undim track
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);

		nodeList.enter();

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
		final Beatmap bm = MusicController.getBeatmap();
		nodeList.focusRandomMap(/*playAtPreviewTime*/ true);
		if (bm != MusicController.getBeatmap()) {
			this.musicIconBounceTimer.setTime(0);
		}
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
		dynBg.reset();

		// reload songs in new thread
		reloadThread = new BeatmapReloadThread(fullReload);
		reloadThread.start();
	}

	/**
	 * Returns whether a delayed/animated event is currently blocking user input.
	 * @return true if blocking input
	 */
	private boolean isInputBlocked() {
		return (reloadThread != null || beatmapMenuTimer > -1);
	}

	/**
	 * Calculates all star ratings for a beatmap set.
	 * @param beatmapSet the set of beatmaps
	 */
	private void calculateStarRatings(BeatmapSet beatmapSet)
	{
		for (int i = 0; i < beatmapSet.beatmaps.length; i++) {
			final Beatmap beatmap = beatmapSet.beatmaps[i];
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
