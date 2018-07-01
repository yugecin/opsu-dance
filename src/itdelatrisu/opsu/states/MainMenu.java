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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapSetNode;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.states.ButtonMenu.MenuState;
import itdelatrisu.opsu.ui.*;
import itdelatrisu.opsu.ui.MenuButton.Expand;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.awt.Desktop;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import org.lwjgl.opengl.Display;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.Constants;
import yugecin.opsudance.core.state.BaseOpsuState;
import yugecin.opsudance.core.state.OpsuState;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.ui.Colors.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * "Main Menu" state.
 * <p>
 * Players are able to enter the song menu or downloads menu from this state.
 */
public class MainMenu extends BaseOpsuState {

	/** Idle time, in milliseconds, before returning the logo to its original position. */
	private static final short LOGO_IDLE_DELAY = 10000;

	/** Max alpha level of the menu background. */
	private static final float BG_MAX_ALPHA = 0.9f;

	/** Logo button that reveals other buttons on click. */
	private MenuButton logo;

	/** Logo states. */
	private enum LogoState { DEFAULT, OPENING, OPEN, CLOSING }

	/** Current logo state. */
	private LogoState logoState = LogoState.DEFAULT;

	/** Delay timer, in milliseconds, before starting to move the logo back to the center. */
	private int logoTimer = 0;

	/** Logo horizontal offset for opening and closing actions. */
	private AnimatedValue logoOpen, logoClose;

	/** Logo button alpha levels. */
	private AnimatedValue logoButtonAlpha;
	
	/** Now playing position vlaue. */
	private final AnimatedValue nowPlayingPosition;

	/** Main "Play" and "Exit" buttons. */
	private MenuButton playButton, exitButton;

	/** Music control buttons. */
	private MenuButton musicPlay, musicPause, musicStop, musicNext, musicPrev;
	private MenuButton[] musicButtons = new MenuButton[5];

	/** Button linking to Downloads menu. */
	private MenuButton downloadsButton;

	/** Button linking to repository. */
	private MenuButton repoButton;

	/** Button linking to dance repository. */
	private MenuButton danceRepoButton;

	/** Buttons for installing updates. */
	private MenuButton updateButton, restartButton;

	private int textMarginX;
	private int textTopMarginY;
	private int textBottomMarginY;
	private int textLineHeight;

	/** Application start time, for drawing the total running time. */
	private long programStartTime;

	/** Indexes of previous songs. */
	private Stack<Integer> previous;

	/** Background alpha level (for fade-in effect). */
	private AnimatedValue bgAlpha = new AnimatedValue(1100, 0f, BG_MAX_ALPHA, AnimationEquation.LINEAR);

	/** Whether or not a notification was already sent upon entering. */
	private boolean enterNotification = false;

	/** Music position bar coordinates and dimensions. */
	private int musicBarX, musicBarY, musicBarWidth, musicBarHeight;

	/** Last measure progress value. */
	private float lastMeasureProgress = 0f;

	/** The star fountain. */
	private StarFountain starFountain;
	
	private LinkedList<PulseData> pulseData = new LinkedList<>();
	private float lastPulseProgress;
	
	public MainMenu() {
		this.nowPlayingPosition = new AnimatedValue(1000, 0, 0, OUT_QUART);
	}

	@Override
	protected void revalidate() {
		programStartTime = System.currentTimeMillis();
		previous = new Stack<>();

		final int width = displayContainer.width;
		final int height = displayContainer.height;

		this.textMarginX = (int) (width * 0.015f);
		this.textTopMarginY = (int) (height * 0.01f);
		this.textBottomMarginY = (int) (height * 0.015f);
		this.textLineHeight = (int) (Fonts.MEDIUM.getLineHeight() * 0.925f);

		// initialize menu buttons
		Image logoImg = GameImage.MENU_LOGO.getImage();
		Image playImg = GameImage.MENU_PLAY.getImage();
		Image exitImg = GameImage.MENU_EXIT.getImage();
		float exitOffset = (playImg.getWidth() - exitImg.getWidth()) / 3f;
		logo = new MenuButton(logoImg, displayContainer.width / 2f, displayContainer.height / 2f);
		playButton = new MenuButton(playImg,
				displayContainer.width * 0.75f, (displayContainer.height / 2) - (logoImg.getHeight() / 5f)
		);
		exitButton = new MenuButton(exitImg,
				displayContainer.width * 0.75f - exitOffset, (displayContainer.height / 2) + (exitImg.getHeight() / 2f)
		);
		final int logoAnimationDuration = 350;
		logo.setHoverAnimationDuration(logoAnimationDuration);
		playButton.setHoverAnimationDuration(logoAnimationDuration);
		exitButton.setHoverAnimationDuration(logoAnimationDuration);
		final AnimationEquation logoAnimationEquation = AnimationEquation.IN_OUT_EXPO;
		logo.setHoverAnimationEquation(logoAnimationEquation);
		playButton.setHoverAnimationEquation(logoAnimationEquation);
		exitButton.setHoverAnimationEquation(logoAnimationEquation);
		final float logoHoverScale = 1.096f;
		logo.setHoverExpand(logoHoverScale);
		playButton.setHoverExpand(logoHoverScale);
		exitButton.setHoverExpand(logoHoverScale);

		// initialize music buttons
		final int musicSize = (int) (this.textLineHeight * 0.8f);
		final float musicScale = (float) musicSize / MUSIC_STOP.getImage().getWidth();
		final int musicSpacing = (int) (musicSize * 0.8f) + musicSize; // (center to center)
		int x = width - this.textMarginX - musicSize / 2;
		int y = this.textLineHeight * 2 + this.textLineHeight / 2;
		this.musicNext = new MenuButton(MUSIC_NEXT.getScaledImage(musicScale), x, y);
		x -= musicSpacing;
		this.musicStop = new MenuButton(MUSIC_STOP.getScaledImage(musicScale), x, y);
		x -= musicSpacing;
		this.musicPause = new MenuButton(MUSIC_PAUSE.getScaledImage(musicScale), x, y);
		x -= musicSpacing;
		this.musicPlay = new MenuButton(MUSIC_PLAY.getScaledImage(musicScale), x, y);
		x -= musicSpacing;
		this.musicPrev = new MenuButton(MUSIC_PREVIOUS.getScaledImage(musicScale), x, y);
		this.musicButtons[0] = this.musicPrev;
		this.musicButtons[1] = this.musicPlay;
		this.musicButtons[2] = this.musicPause;
		this.musicButtons[3] = this.musicStop;
		this.musicButtons[4] = this.musicNext;
		for (MenuButton b : this.musicButtons) {
			b.setHoverExpand(1.15f);
		}

		// initialize music position bar location
		this.musicBarX = x - musicSize / 2;
		this.musicBarY = y + musicSize;
		this.musicBarWidth = musicSize + musicSpacing * 4;
		this.musicBarHeight = (int) (musicSize * 0.3f);

		// initialize downloads button
		Image dlImg = GameImage.DOWNLOADS.getImage();
		downloadsButton = new MenuButton(dlImg, displayContainer.width - dlImg.getWidth() / 2f, displayContainer.height / 2f);
		downloadsButton.setHoverAnimationDuration(350);
		downloadsButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
		downloadsButton.setHoverExpand(1.03f, Expand.LEFT);

		// initialize repository button
		float startX = displayContainer.width * 0.997f, startY = displayContainer.height * 0.997f;
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {  // only if a webpage can be opened
			Image repoImg;
			repoImg = GameImage.REPOSITORY.getImage();
			repoButton = new MenuButton(repoImg,
					startX - repoImg.getWidth() * 2.5f, startY - repoImg.getHeight()
			);
			repoButton.setHoverAnimationDuration(350);
			repoButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
			repoButton.setHoverExpand();
			repoImg = GameImage.REPOSITORY.getImage();
			danceRepoButton = new MenuButton(repoImg,
				startX - repoImg.getWidth(), startY - repoImg.getHeight()
			);
			danceRepoButton.setHoverAnimationDuration(350);
			danceRepoButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
			danceRepoButton.setHoverExpand();
		}

		// initialize update buttons
		float updateX = displayContainer.width / 2f, updateY = displayContainer.height * 17 / 18f;
		Image downloadImg = GameImage.DOWNLOAD.getImage();
		updateButton = new MenuButton(downloadImg, updateX, updateY);
		updateButton.setHoverAnimationDuration(400);
		updateButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_QUAD);
		updateButton.setHoverExpand(1.1f);
		Image updateImg = GameImage.UPDATE.getImage();
		restartButton = new MenuButton(updateImg, updateX, updateY);
		restartButton.setHoverAnimationDuration(2000);
		restartButton.setHoverAnimationEquation(AnimationEquation.LINEAR);
		restartButton.setHoverRotate(360);

		// initialize star fountain
		starFountain = new StarFountain(displayContainer.width, displayContainer.height);

		// logo animations
		float centerOffsetX = displayContainer.width / 6.5f;
		logoOpen = new AnimatedValue(100, 0, centerOffsetX, AnimationEquation.OUT_QUAD);
		logoClose = new AnimatedValue(2200, centerOffsetX, 0, AnimationEquation.OUT_QUAD);
		logoButtonAlpha = new AnimatedValue(200, 0f, 1f, AnimationEquation.LINEAR);
	}

	@Override
	public void render(Graphics g) {
		int width = displayContainer.width;
		int height = displayContainer.height;

		// draw background
		Beatmap beatmap = MusicController.getBeatmap();
		if (OPTION_DYNAMIC_BACKGROUND.state &&
			beatmap != null && beatmap.drawBackground(width, height, bgAlpha.getValue(), true))
			;
		else {
			Image bg = GameImage.MENU_BG.getImage();
			bg.setAlpha(bgAlpha.getValue());
			bg.draw();
		}

		// top/bottom horizontal bars
		float oldAlpha = Colors.BLACK_ALPHA.a;
		Colors.BLACK_ALPHA.a = 0.4f;
		g.setColor(Colors.BLACK_ALPHA);
		final float barheight = height * 0.1125f;
		g.fillRect(0, 0, width, barheight);
		g.fillRect(0, height - barheight, width, barheight);
		Colors.BLACK_ALPHA.a = oldAlpha;

		// draw star fountain
		starFountain.draw();

		// draw downloads button
		downloadsButton.draw();

		// draw buttons
		if (logoState == LogoState.OPEN || logoState == LogoState.CLOSING) {
			playButton.draw();
			exitButton.draw();
		}

		// draw logo (pulsing)
		Color color = OPTION_COLOR_MAIN_MENU_LOGO.state ? Cursor.lastCursorColor : Color.white;
		for (PulseData pd : this.pulseData) {
			final float progress = OUT_CUBIC.calc(pd.position / 1000f);
			final float scale = pd.initialScale + (0.432f * progress);
			final Image p = GameImage.MENU_LOGO_PULSE.getImage().getScaledCopy(scale);
			p.setAlpha(0.15f * (1f - IN_QUAD.calc(progress)));
			p.drawCentered(logo.getX(), logo.getY(), color);
		}
		Float position = MusicController.getBeatProgress();
		Float beatLength = MusicController.getBeatLength();
		boolean renderPiece = position != null;
		if (position == null) {
			position = System.currentTimeMillis() % 1000 / 1000f;
			beatLength = 1000f;
		}
		final float hoverScale = logo.getCurrentHoverExpandValue();
		if (position < this.lastPulseProgress) {
			this.pulseData.add(new PulseData((int) (position*beatLength), hoverScale));
		}
		this.lastPulseProgress = position;
		final float smoothExpandProgress;
		if (position < 0.05f) {
			smoothExpandProgress = 1f - IN_CUBIC.calc(position / 0.05f);
		} else {
			smoothExpandProgress = (position - 0.05f) / 0.95f;
		}
		logo.draw(color, 0.9726f + smoothExpandProgress * 0.0274f);
		if (renderPiece) {
			Image piece = GameImage.MENU_LOGO_PIECE.getImage();
			piece = piece.getScaledCopy(hoverScale);
			piece.rotate(position * 360);
			piece.drawCentered(logo.getX(), logo.getY(), color);
		}
		final float ghostScale = hoverScale * 1.0186f - smoothExpandProgress * 0.0186f;
		Image ghostLogo = GameImage.MENU_LOGO.getImage().getScaledCopy(ghostScale);
		ghostLogo.setAlpha(0.25f);
		ghostLogo.drawCentered(logo.getX(), logo.getY(), color);
		
		// now playing
		if (OPTION_SHOW_UNICODE.state) {
			Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.titleUnicode);
			Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.artistUnicode);
		}
		final Image np = GameImage.MUSIC_NOWPLAYING.getImage();
		final String trackText = beatmap.getArtist() + ": " + beatmap.getTitle();
		final float textWidth = Fonts.MEDIUM.getWidth(trackText);
		final float npheight = Fonts.MEDIUM.getLineHeight() * 1.15f;
		final float npscale = npheight / np.getHeight();
		final float npwidth = np.getWidth() * npscale;
		float totalWidth = textMarginX + textWidth + npwidth;
		if (this.nowPlayingPosition.getMax() != totalWidth) {
			final float current = this.nowPlayingPosition.getValue();
			this.nowPlayingPosition.setValues(current, totalWidth);
		}
		final float npimgx = width - this.nowPlayingPosition.getValue();
		final float npx = npimgx + npwidth;
		MUSIC_NOWPLAYING_BG_BLACK.getImage().draw(npx, 0, width - npx, npheight);
		MUSIC_NOWPLAYING_BG_WHITE.getImage().draw(npimgx, npheight, width - npimgx, 2);
		np.draw(npimgx, 0, npscale);
		Fonts.MEDIUM.drawString(npx, 0, trackText);

		// draw music buttons
		for (MenuButton b : this.musicButtons) {
			b.draw();
		}

		// draw music position bar
		int mouseX = displayContainer.mouseX;
		int mouseY = displayContainer.mouseY;
		g.setColor((musicPositionBarContains(mouseX, mouseY)) ? Colors.BLACK_BG_HOVER : Colors.BLACK_BG_NORMAL);
		g.fillRect(this.musicBarX, this.musicBarY, this.musicBarWidth, this.musicBarHeight);
		g.setColor(Colors.WHITE_ALPHA_75);
		if (!MusicController.isTrackLoading() && beatmap != null) {
			final float trackpos = MusicController.getPosition();
			final float tracklen = MusicController.getDuration();
			final float barwidth = musicBarWidth * Math.min(trackpos / tracklen, 1f);
			g.fillRect(this.musicBarX, this.musicBarY, barwidth, this.musicBarHeight);
		}

		// draw repository buttons
		if (repoButton != null) {
			String text;
			int fheight, fwidth;
			repoButton.draw();
			text = "opsu!";
			fheight = Fonts.SMALL.getLineHeight();
			fwidth = Fonts.SMALL.getWidth(text);
			Fonts.SMALL.drawString(repoButton.getX() - fwidth / 2, repoButton.getY() - repoButton.getImage().getHeight() / 2 - fheight, text, Color.white);
			danceRepoButton.draw();
			text = "opsu!dance";
			fheight = Fonts.SMALL.getLineHeight();
			fwidth = Fonts.SMALL.getWidth(text);
			Fonts.SMALL.drawString(danceRepoButton.getX() - fwidth / 2, repoButton.getY() - repoButton.getImage().getHeight() / 2 - fheight, text, Color.white);
		}

		// draw update button
		if (updater.showButton()) {
			Updater.Status status = updater.getStatus();
			if (status == Updater.Status.UPDATE_AVAILABLE || status == Updater.Status.UPDATE_DOWNLOADING) {
				updateButton.draw();
			} else if (status == Updater.Status.UPDATE_DOWNLOADED) {
				restartButton.draw();
			}
		}

		// draw text
		g.setFont(Fonts.MEDIUM);
		final String beatmapText = String.format(
			"You have %d beatmaps (%d songs) available!",
			BeatmapSetList.get().getMapCount(),
			BeatmapSetList.get().getMapSetCount()
		);
		g.drawString(beatmapText, textMarginX, textTopMarginY);
		g.drawString(String.format("opsu! has been running for %s.",
				Utils.getTimeString((int) (System.currentTimeMillis() - programStartTime) / 1000)),
				textMarginX, height - textBottomMarginY - (textLineHeight * 2));
		g.drawString(String.format("It is currently %s.",
				new SimpleDateFormat("HH:mm").format(new Date())),
				textMarginX, height - textBottomMarginY - textLineHeight);

		UI.draw(g);
	}

	@Override
	public void preRenderUpdate() {
		int delta = displayContainer.renderDelta;
		
		final Iterator<PulseData> pulseDataIter = this.pulseData.iterator();
		while (pulseDataIter.hasNext()) {
			final PulseData pd = pulseDataIter.next();
			pd.position += delta;
			if (pd.position > 1000) {
				pulseDataIter.remove();
			}
		}

		UI.update(delta);
		if (MusicController.trackEnded())
			nextTrack(false);  // end of track: go to next track
		int mouseX = displayContainer.mouseX;
		int mouseY = displayContainer.mouseY;
		logo.hoverUpdate(delta, mouseX, mouseY, 0.25f);
		playButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);
		exitButton.hoverUpdate(delta, mouseX, mouseY, 0.25f);
		if (repoButton != null) {
			repoButton.hoverUpdate(delta, mouseX, mouseY);
			danceRepoButton.hoverUpdate(delta, mouseX, mouseY);
		}
		if (updater.showButton()) {
			updateButton.autoHoverUpdate(delta, true);
			restartButton.autoHoverUpdate(delta, false);
		}
		downloadsButton.hoverUpdate(delta, mouseX, mouseY);
		for (MenuButton b : this.musicButtons) {
			b.hoverUpdate(delta, b.contains(mouseX, mouseY));
		}
		starFountain.update(delta);

		// window focus change: increase/decrease theme song volume
		if (MusicController.isThemePlaying() &&
		    MusicController.isTrackDimmed() == Display.isActive())
				MusicController.toggleTrackDimmed(0.33f);

		// fade in background
		Beatmap beatmap = MusicController.getBeatmap();
		if (!(OPTION_DYNAMIC_BACKGROUND.state && beatmap != null && beatmap.isBackgroundLoading()))
			bgAlpha.update(delta);

		// check measure progress
		Float measureProgress = MusicController.getMeasureProgress(2);
		if (measureProgress != null) {
			if (measureProgress < lastMeasureProgress)
				starFountain.burst(true);
			lastMeasureProgress = measureProgress;
		}

		// buttons
		int centerX = displayContainer.width / 2;
		float currentLogoButtonAlpha;
		switch (logoState) {
		case DEFAULT:
			break;
		case OPENING:
			if (logoOpen.update(delta))  // shifting to left
				logo.setX(centerX - logoOpen.getValue());
			else {
				logoState = LogoState.OPEN;
				logoTimer = 0;
				logoButtonAlpha.setTime(0);
			}
			break;
		case OPEN:
			if (logoButtonAlpha.update(delta)) {  // fade in buttons
				currentLogoButtonAlpha = logoButtonAlpha.getValue();
				playButton.getImage().setAlpha(currentLogoButtonAlpha);
				exitButton.getImage().setAlpha(currentLogoButtonAlpha);
			} else if (logoTimer >= LOGO_IDLE_DELAY) {  // timer over: shift back to center
				logoState = LogoState.CLOSING;
				logoClose.setTime(0);
				logoTimer = 0;
			} else  // increment timer
				logoTimer += delta;
			break;
		case CLOSING:
			if (logoButtonAlpha.update(-delta)) {  // fade out buttons
				currentLogoButtonAlpha = logoButtonAlpha.getValue();
				playButton.getImage().setAlpha(currentLogoButtonAlpha);
				exitButton.getImage().setAlpha(currentLogoButtonAlpha);
			}
			if (logoClose.update(delta))  // shifting to right
				logo.setX(centerX - logoClose.getValue());
			break;
		}

		// tooltips
		if (musicPositionBarContains(mouseX, mouseY))
			UI.updateTooltip(delta, "Click to seek to a specific point in the song.", false);
		else if (musicPrev.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Previous track", false);
		else if (musicPlay.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Play", false);
		else if (musicPause.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Pause", false);
		else if (musicStop.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Stop", false);
		else if (musicNext.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Next track", false);
		else if (updater.showButton()) {
			Updater.Status status = updater.getStatus();
			if (((status == Updater.Status.UPDATE_AVAILABLE || status == Updater.Status.UPDATE_DOWNLOADING) && updateButton.contains(mouseX, mouseY)) ||
			    (status == Updater.Status.UPDATE_DOWNLOADED && restartButton.contains(mouseX, mouseY)))
				UI.updateTooltip(delta, status.getDescription(), true);
		}
		
		nowPlayingPosition.update(delta);
	}

	@Override
	public void enter() {
		super.enter();

		logo.setX(displayContainer.width / 2);
		logoOpen.setTime(0);
		logoClose.setTime(0);
		logoButtonAlpha.setTime(0);
		nowPlayingPosition.setTime(0);
		logoTimer = 0;
		logoState = LogoState.DEFAULT;

		UI.enter();
		if (!enterNotification) {
			if (updater.getStatus() == Updater.Status.UPDATE_AVAILABLE) {
				barNotifs.send("An opsu! update is available.");
			} else if (updater.justUpdated()) {
				barNotifs.send("opsu! is now up to date!");
			}
			enterNotification = true;
		}

		// reset measure info
		lastMeasureProgress = 0f;
		starFountain.clear();

		// reset button hover states if mouse is not currently hovering over the button
		int mouseX = displayContainer.mouseX;
		int mouseY = displayContainer.mouseY;
		if (!logo.contains(mouseX, mouseY, 0.25f))
			logo.resetHover();
		if (!playButton.contains(mouseX, mouseY, 0.25f))
			playButton.resetHover();
		if (!exitButton.contains(mouseX, mouseY, 0.25f))
			exitButton.resetHover();
		for (MenuButton b : this.musicButtons) {
			if (!b.contains(mouseX, mouseY)) {
				b.resetHover();
			}
		}
		if (repoButton != null && !repoButton.contains(mouseX, mouseY))
			repoButton.resetHover();
		if (danceRepoButton != null && !danceRepoButton.contains(mouseX, mouseY))
			danceRepoButton.resetHover();
		updateButton.resetHover();
		restartButton.resetHover();
		if (!downloadsButton.contains(mouseX, mouseY))
			downloadsButton.resetHover();
	}

	@Override
	public void leave() {
		super.leave();
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return false;

		// music position bar
		if (MusicController.isPlaying() && musicPositionBarContains(x, y)) {
			this.lastMeasureProgress = 0f;
			float pos = (float) (x - this.musicBarX) / this.musicBarWidth;
			MusicController.setPosition((int) (pos * MusicController.getDuration()));
			return true;
		}

		// music button actions
		if (musicPrev.contains(x, y)) {
			lastMeasureProgress = 0f;
			if (!previous.isEmpty()) {
				songMenuState.setFocus(BeatmapSetList.get().getBaseNode(previous.pop()), -1, true, false);
				if (OPTION_DYNAMIC_BACKGROUND.state) {
					bgAlpha.setTime(0);
				}
			} else {
				MusicController.setPosition(0);
			}
			barNotifs.send("<< Previous");
			return true;
		} else if (musicPlay.contains(x, y)) {
			if (MusicController.isPlaying()) {
				lastMeasureProgress = 0f;
				MusicController.setPosition(0);
			} else if (!MusicController.isTrackLoading()) {
				MusicController.resume();
			}
			barNotifs.send("Play");
			return true;
		} else if (musicPause.contains(x, y)) {
			if (MusicController.isPlaying()) {
				MusicController.pause();
				barNotifs.send("Pause");
			} else if (!MusicController.isTrackLoading()) {
				MusicController.resume();
				barNotifs.send("Unpause");
			}
		} else if (musicStop.contains(x, y)) {
			if (MusicController.isPlaying()) {
				MusicController.setPosition(0);
				MusicController.pause();
			}
			barNotifs.send("Stop Playing");
		} else if (musicNext.contains(x, y)) {
			nextTrack(true);
			barNotifs.send(">> Next");
			return true;
		}

		// downloads button actions
		if (downloadsButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			displayContainer.switchState(downloadState);
			return true;
		}

		// repository button actions
		if (repoButton != null && repoButton.contains(x, y)) {
			try {
				Desktop.getDesktop().browse(Constants.REPOSITORY_URI);
			} catch (UnsupportedOperationException e) {
				barNotifs.send("The repository web page could not be opened.");
			} catch (IOException e) {
				Log.error("could not browse to repo", e);
				bubNotifs.send(BUB_ORANGE, "Could not browse to repo");
			}
			return true;
		}

		if (danceRepoButton != null && danceRepoButton.contains(x, y)) {
			try {
				Desktop.getDesktop().browse(Constants.DANCE_REPOSITORY_URI);
			} catch (UnsupportedOperationException e) {
				barNotifs.send("The repository web page could not be opened.");
			} catch (IOException e) {
				Log.error("could not browse to repo", e);
				bubNotifs.send(BUB_ORANGE, "Could not browse to repo");
			}
			return true;
		}

		// update button actions
		if (updater.showButton()) {
			Updater.Status status = updater.getStatus();
			if (updateButton.contains(x, y) && status == Updater.Status.UPDATE_AVAILABLE) {
				SoundController.playSound(SoundEffect.MENUHIT);
				updater.startDownload();
				updateButton.removeHoverEffects();
				updateButton.setHoverAnimationDuration(800);
				updateButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_QUAD);
				updateButton.setHoverFade(0.6f);
				return true;
			} else if (restartButton.contains(x, y) && status == Updater.Status.UPDATE_DOWNLOADED) {
				SoundController.playSound(SoundEffect.MENUHIT);
				updater.prepareUpdate();
				displayContainer.exitRequested = true;
				return true;
			}
		}

		// start moving logo (if clicked)
		if (logoState == LogoState.DEFAULT || logoState == LogoState.CLOSING) {
			if (logo.contains(x, y, 0.25f)) {
				logoState = LogoState.OPENING;
				logoOpen.setTime(0);
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
				SoundController.playSound(SoundEffect.MENUHIT);
				return true;
			}
		}

		// other button actions (if visible)
		else if (logoState == LogoState.OPEN || logoState == LogoState.OPENING) {
			if (logo.contains(x, y, 0.25f) || playButton.contains(x, y, 0.25f)) {
				SoundController.playSound(SoundEffect.MENUHIT);
				enterSongMenu();
				return true;
			} else if (exitButton.contains(x, y, 0.25f)) {
				displayContainer.exitRequested = true;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseWheelMoved(int newValue) {
		if (super.mouseWheelMoved(newValue)) {
			return true;
		}

		UI.changeVolume((newValue < 0) ? -1 : 1);
		return true;
	}

	@Override
	public boolean keyPressed(int key, char c) {
		if (super.keyPressed(key, c)) {
			return true;
		}

		switch (key) {
		case KEY_ESCAPE:
		case KEY_Q:
			if (logoTimer > 0) {
				logoState = LogoState.CLOSING;
				logoClose.setTime(0);
				logoTimer = 0;
				break;
			}
			buttonState.setMenuState(MenuState.EXIT);
			displayContainer.switchState(buttonState);
			return true;
		case KEY_P:
			SoundController.playSound(SoundEffect.MENUHIT);
			if (logoState == LogoState.DEFAULT || logoState == LogoState.CLOSING) {
				logoState = LogoState.OPENING;
				logoOpen.setTime(0);
				logoTimer = 0;
				playButton.getImage().setAlpha(0f);
				exitButton.getImage().setAlpha(0f);
			} else
				enterSongMenu();
			return true;
		case KEY_D:
			SoundController.playSound(SoundEffect.MENUHIT);
			displayContainer.switchState(downloadState);
			return true;
		case KEY_R:
			nextTrack(true);
			return true;
		case KEY_UP:
			UI.changeVolume(1);
			return true;
		case KEY_DOWN:
			UI.changeVolume(-1);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the coordinates are within the music position bar bounds.
	 * @param cx the x coordinate
	 * @param cy the y coordinate
	 */
	private boolean musicPositionBarContains(float cx, float cy) {
		return ((cx > musicBarX && cx < musicBarX + musicBarWidth) &&
		        (cy > musicBarY && cy < musicBarY + musicBarHeight));
	}

	/**
	 * Plays the next track, and adds the previous one to the stack.
	 * @param user {@code true} if this was user-initiated, false otherwise (track end)
	 */
	private void nextTrack(boolean user) {
		lastMeasureProgress = 0f;
		boolean isTheme = MusicController.isThemePlaying();
		if (isTheme && !user) {
			// theme was playing, restart
			// NOTE: not looping due to inaccurate track positions after loop
			MusicController.playAt(0, false);
			return;
		}
		BeatmapSetNode node = songMenuState.setFocus(BeatmapSetList.get().getRandomNode(), -1, true, false);
		boolean sameAudio = false;
		if (node != null) {
			sameAudio = MusicController.getBeatmap().audioFilename.equals(node.getBeatmapSet().get(0).audioFilename);
			if (!isTheme && !sameAudio)
				previous.add(node.index);
		}
		if (OPTION_DYNAMIC_BACKGROUND.state && !sameAudio && !MusicController.isThemePlaying()) {
			bgAlpha.setTime(0);
		}
	}

	/**
	 * Enters the song menu, or the downloads menu if no beatmaps are loaded.
	 */
	private void enterSongMenu() {
		OpsuState state = songMenuState;
		if (BeatmapSetList.get().getMapSetCount() == 0) {
			downloadState.notifyOnLoad("Download some beatmaps to get started!");
			state = downloadState;
		}
		displayContainer.switchState(state);
	}
	
	private static class PulseData {
		private int position;
		private float initialScale;

		private PulseData(int position, float initialScale) {
			this.position = position;
			this.initialScale = initialScale;
		}
	}
}
