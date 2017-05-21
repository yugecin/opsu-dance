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
import java.util.Stack;

import org.lwjgl.opengl.Display;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.Constants;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.state.BaseOpsuState;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.events.BarNotificationEvent;
import yugecin.opsudance.events.BubbleNotificationEvent;

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

	/** Main "Play" and "Exit" buttons. */
	private MenuButton playButton, exitButton;

	/** Music control buttons. */
	private MenuButton musicPlay, musicPause, musicNext, musicPrevious;

	/** Button linking to Downloads menu. */
	private MenuButton downloadsButton;

	/** Button linking to repository. */
	private MenuButton repoButton;

	/** Button linking to dance repository. */
	private MenuButton danceRepoButton;

	/** Buttons for installing updates. */
	private MenuButton updateButton, restartButton;

	/** Application start time, for drawing the total running time. */
	private long programStartTime;

	/** Indexes of previous songs. */
	private Stack<Integer> previous;

	/** Background alpha level (for fade-in effect). */
	private AnimatedValue bgAlpha = new AnimatedValue(1100, 0f, BG_MAX_ALPHA, AnimationEquation.LINEAR);

	/** Whether or not a notification was already sent upon entering. */
	private boolean enterNotification = false;

	/** Music position bar coordinates and dimensions. */
	private float musicBarX, musicBarY, musicBarWidth, musicBarHeight;

	/** Last measure progress value. */
	private float lastMeasureProgress = 0f;

	/** The star fountain. */
	private StarFountain starFountain;

	@Override
	protected void revalidate() {
		programStartTime = System.currentTimeMillis();
		previous = new Stack<>();

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
		final AnimationEquation logoAnimationEquation = AnimationEquation.IN_OUT_BACK;
		logo.setHoverAnimationEquation(logoAnimationEquation);
		playButton.setHoverAnimationEquation(logoAnimationEquation);
		exitButton.setHoverAnimationEquation(logoAnimationEquation);
		final float logoHoverScale = 1.08f;
		logo.setHoverExpand(logoHoverScale);
		playButton.setHoverExpand(logoHoverScale);
		exitButton.setHoverExpand(logoHoverScale);

		// initialize music buttons
		int musicWidth  = GameImage.MUSIC_PLAY.getImage().getWidth();
		int musicHeight = GameImage.MUSIC_PLAY.getImage().getHeight();
		musicPlay     = new MenuButton(GameImage.MUSIC_PLAY.getImage(), displayContainer.width - (2 * musicWidth), musicHeight / 1.5f);
		musicPause    = new MenuButton(GameImage.MUSIC_PAUSE.getImage(), displayContainer.width - (2 * musicWidth), musicHeight / 1.5f);
		musicNext     = new MenuButton(GameImage.MUSIC_NEXT.getImage(), displayContainer.width - musicWidth, musicHeight / 1.5f);
		musicPrevious = new MenuButton(GameImage.MUSIC_PREVIOUS.getImage(), displayContainer.width - (3 * musicWidth), musicHeight / 1.5f);
		musicPlay.setHoverExpand(1.5f);
		musicPause.setHoverExpand(1.5f);
		musicNext.setHoverExpand(1.5f);
		musicPrevious.setHoverExpand(1.5f);

		// initialize music position bar location
		musicBarX = displayContainer.width - musicWidth * 3.5f;
		musicBarY = musicHeight * 1.25f;
		musicBarWidth = musicWidth * 3f;
		musicBarHeight = musicHeight * 0.11f;

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
		Colors.BLACK_ALPHA.a = 0.2f;
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(0, 0, width, height / 9f);
		g.fillRect(0, height * 8 / 9f, width, height / 9f);
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
		Float position = MusicController.getBeatProgress();
		boolean renderPiece = position != null;
		if (position == null) {
			position = System.currentTimeMillis() % 1000 / 1000f;
		}
		float scale = 1f + position * 0.05f;
		logo.draw(color, scale);
		if (renderPiece) {
			Image piece = GameImage.MENU_LOGO_PIECE.getImage().getScaledCopy(logo.getLastScale());
			piece.rotate(position * 360);
			piece.drawCentered(logo.getX(), logo.getY(), color);
		}
		float ghostScale = logo.getLastScale() / scale * 1.05f;
		Image ghostLogo = GameImage.MENU_LOGO.getImage().getScaledCopy(ghostScale);
		ghostLogo.drawCentered(logo.getX(), logo.getY(), Colors.GHOST_LOGO);

		// draw music buttons
		if (MusicController.isPlaying())
			musicPause.draw();
		else
			musicPlay.draw();
		musicNext.draw();
		musicPrevious.draw();

		// draw music position bar
		int mouseX = displayContainer.mouseX;
		int mouseY = displayContainer.mouseY;
		g.setColor((musicPositionBarContains(mouseX, mouseY)) ? Colors.BLACK_BG_HOVER : Colors.BLACK_BG_NORMAL);
		g.fillRoundRect(musicBarX, musicBarY, musicBarWidth, musicBarHeight, 4);
		g.setColor(Color.white);
		if (!MusicController.isTrackLoading() && beatmap != null) {
			float musicBarPosition = Math.min((float) MusicController.getPosition() / MusicController.getDuration(), 1f);
			g.fillRoundRect(musicBarX, musicBarY, musicBarWidth * musicBarPosition, musicBarHeight, 4);
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
		float marginX = width * 0.015f, topMarginY = height * 0.01f, bottomMarginY = height * 0.015f;
		g.setFont(Fonts.MEDIUM);
		float lineHeight = Fonts.MEDIUM.getLineHeight() * 0.925f;
		g.drawString(String.format("Loaded %d songs and %d beatmaps.",
			BeatmapSetList.get().getMapSetCount(), BeatmapSetList.get().getMapCount()), marginX, topMarginY);
		if (MusicController.isTrackLoading()) {
			g.drawString("Track loading...", marginX, topMarginY + lineHeight);
		} else if (MusicController.trackExists()) {
			if (OPTION_SHOW_UNICODE.state) {
				Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.titleUnicode);
				Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.artistUnicode);
			}
			g.drawString((MusicController.isPlaying()) ? "Now Playing:" : "Paused:", marginX, topMarginY + lineHeight);
			g.drawString(String.format("%s: %s", beatmap.getArtist(), beatmap.getTitle()), marginX + 25, topMarginY + (lineHeight * 2));
		}
		g.drawString(String.format("opsu! has been running for %s.",
				Utils.getTimeString((int) (System.currentTimeMillis() - programStartTime) / 1000)),
				marginX, height - bottomMarginY - (lineHeight * 2));
		g.drawString(String.format("It is currently %s.",
				new SimpleDateFormat("h:mm a").format(new Date())),
				marginX, height - bottomMarginY - lineHeight);

		UI.draw(g);
	}

	@Override
	public void preRenderUpdate() {
		int delta = displayContainer.renderDelta;

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
		// ensure only one button is in hover state at once
		boolean noHoverUpdate = musicPositionBarContains(mouseX, mouseY);
		boolean contains = musicPlay.contains(mouseX, mouseY);
		musicPlay.hoverUpdate(delta, !noHoverUpdate && contains);
		musicPause.hoverUpdate(delta, !noHoverUpdate && contains);
		noHoverUpdate |= contains;
		musicNext.hoverUpdate(delta, !noHoverUpdate && musicNext.contains(mouseX, mouseY));
		musicPrevious.hoverUpdate(delta, !noHoverUpdate && musicPrevious.contains(mouseX, mouseY));
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
		else if (musicPlay.contains(mouseX, mouseY))
			UI.updateTooltip(delta, (MusicController.isPlaying()) ? "Pause" : "Play", false);
		else if (musicNext.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Next track", false);
		else if (musicPrevious.contains(mouseX, mouseY))
			UI.updateTooltip(delta, "Previous track", false);
		else if (updater.showButton()) {
			Updater.Status status = updater.getStatus();
			if (((status == Updater.Status.UPDATE_AVAILABLE || status == Updater.Status.UPDATE_DOWNLOADING) && updateButton.contains(mouseX, mouseY)) ||
			    (status == Updater.Status.UPDATE_DOWNLOADED && restartButton.contains(mouseX, mouseY)))
				UI.updateTooltip(delta, status.getDescription(), true);
		}
	}

	@Override
	public void enter() {
		super.enter();

		logo.setX(displayContainer.width / 2);
		logoOpen.setTime(0);
		logoClose.setTime(0);
		logoButtonAlpha.setTime(0);
		logoTimer = 0;
		logoState = LogoState.DEFAULT;

		UI.enter();
		if (!enterNotification) {
			if (updater.getStatus() == Updater.Status.UPDATE_AVAILABLE) {
				EventBus.post(new BarNotificationEvent("An opsu! update is available."));
				enterNotification = true;
			} else if (updater.justUpdated()) {
				EventBus.post(new BarNotificationEvent("opsu! is now up to date!"));
				enterNotification = true;
			}
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
		if (!musicPlay.contains(mouseX, mouseY))
			musicPlay.resetHover();
		if (!musicPause.contains(mouseX, mouseY))
			musicPause.resetHover();
		if (!musicNext.contains(mouseX, mouseY))
			musicNext.resetHover();
		if (!musicPrevious.contains(mouseX, mouseY))
			musicPrevious.resetHover();
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
		if (MusicController.isPlaying()) {
			if (musicPositionBarContains(x, y)) {
				lastMeasureProgress = 0f;
				float pos = (x - musicBarX) / musicBarWidth;
				MusicController.setPosition((int) (pos * MusicController.getDuration()));
				return true;
			}
		}

		// music button actions
		if (musicPlay.contains(x, y)) {
			if (MusicController.isPlaying()) {
				MusicController.pause();
				EventBus.post(new BarNotificationEvent("Pause"));
			} else if (!MusicController.isTrackLoading()) {
				MusicController.resume();
				EventBus.post(new BarNotificationEvent("Play"));
			}
			return true;
		} else if (musicNext.contains(x, y)) {
			nextTrack(true);
			EventBus.post(new BarNotificationEvent(">> Next"));
			return true;
		} else if (musicPrevious.contains(x, y)) {
			lastMeasureProgress = 0f;
			if (!previous.isEmpty()) {
				songMenuState.setFocus(BeatmapSetList.get().getBaseNode(previous.pop()), -1, true, false);
				if (OPTION_DYNAMIC_BACKGROUND.state) {
					bgAlpha.setTime(0);
				}
			} else {
				MusicController.setPosition(0);
			}
			EventBus.post(new BarNotificationEvent("<< Previous"));
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
				EventBus.post(new BarNotificationEvent("The repository web page could not be opened."));
			} catch (IOException e) {
				Log.error("could not browse to repo", e);
				EventBus.post(new BubbleNotificationEvent("Could not browse to repo", BubbleNotificationEvent.COLOR_ORANGE));
			}
			return true;
		}

		if (danceRepoButton != null && danceRepoButton.contains(x, y)) {
			try {
				Desktop.getDesktop().browse(Constants.DANCE_REPOSITORY_URI);
			} catch (UnsupportedOperationException e) {
				EventBus.post(new BarNotificationEvent("The repository web page could not be opened."));
			} catch (IOException e) {
				Log.error("could not browse to repo", e);
				EventBus.post(new BubbleNotificationEvent("Could not browse to repo", BubbleNotificationEvent.COLOR_ORANGE));
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
		case Input.KEY_ESCAPE:
		case Input.KEY_Q:
			if (logoTimer > 0) {
				logoState = LogoState.CLOSING;
				logoClose.setTime(0);
				logoTimer = 0;
				break;
			}
			buttonState.setMenuState(MenuState.EXIT);
			displayContainer.switchState(buttonState);
			return true;
		case Input.KEY_P:
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
		case Input.KEY_D:
			SoundController.playSound(SoundEffect.MENUHIT);
			displayContainer.switchState(downloadState);
			return true;
		case Input.KEY_R:
			nextTrack(true);
			return true;
		case Input.KEY_UP:
			UI.changeVolume(1);
			return true;
		case Input.KEY_DOWN:
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
}
