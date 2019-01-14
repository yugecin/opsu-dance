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
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.states.ButtonMenu.MenuState;
import itdelatrisu.opsu.ui.*;
import itdelatrisu.opsu.ui.MenuButton.Expand;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.Constants;
import yugecin.opsudance.core.Entrypoint;
import yugecin.opsudance.core.input.*;
import yugecin.opsudance.core.state.BaseOpsuState;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.ui.ImagePosition;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.ui.Colors.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.*;
import static java.awt.Desktop.Action.*;
import static java.lang.Math.*;
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
	private static final short LOGO_IDLE_DELAY = 6000;

	/** Max alpha level of the menu background. */
	private static final float BG_MAX_ALPHA = 0.9f;
	
	private float barHeight;

	private ImagePosition logo;
	private AnimatedValue logoHover;

	/** Logo states. */
	private enum LogoState { DEFAULT, OPENING, OPEN, CLOSING }

	/** Current logo state. */
	private LogoState logoState = LogoState.DEFAULT;

	/** Delay timer, in milliseconds, before starting to move the logo back to the center. */
	private int logoTimer = 0;

	/** Logo horizontal offset for opening and closing actions. */
	private AnimatedValue logoPosition;
	private float logoPositionOffsetX;
	
	private int lastMouseX;
	private int lastMouseY;
	
	private AnimatedValue logoClickScale;
	private AnimatedValue buttonAnimation;
	private int buttonsX;
	private AnimatedValue[] buttonAnimations;
	private ImagePosition[] buttonPositions;

	/** Logo button alpha levels. */
	private AnimatedValue logoButtonAlpha;
	
	/** Now playing position vlaue. */
	private final AnimatedValue nowPlayingPosition;

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
	private int textLineHeight;

	/** Background alpha level (for fade-in effect). */
	private AnimatedValue bgAlpha = new AnimatedValue(1100, 0f, BG_MAX_ALPHA, AnimationEquation.LINEAR);
	private File currentBackgroundFile;

	/** Whether or not a notification was already sent upon entering. */
	private boolean enterNotification = false;

	/** Music position bar coordinates and dimensions. */
	private int musicBarX, musicBarY, musicBarWidth, musicBarHeight;

	/** Last measure progress value. */
	private float lastMeasureProgress = 0f;

	/** The star fountain. */
	private StarFountain starFountain;
	
	/** Time format used to show running time. */
	private final SimpleDateFormat timeFormat;
	
	private LinkedList<PulseData> pulseData = new LinkedList<>();
	private float lastPulseProgress;
	
	public MainMenu()
	{
		this.nowPlayingPosition = new AnimatedValue(1000, 0, 0, OUT_QUART);
		this.logoClickScale = new AnimatedValue(300, .9f, 1f, OUT_QUAD);
		this.logoHover = new AnimatedValue(350, 1f, 1.096f, IN_OUT_EXPO);
		this.logoPosition = new AnimatedValue(1, 0, 1, AnimationEquation.OUT_QUAD);
		this.logoButtonAlpha = new AnimatedValue(200, 0f, 1f, AnimationEquation.LINEAR);
		this.buttonAnimation = new AnimatedValue(1, 0f, 1f, OUT_QUAD);
		this.buttonAnimations = new AnimatedValue[3];
		for (int i = 0; i < 3; i++) {
			this.buttonAnimations[i] = new AnimatedValue(1, 0f, 1f, LINEAR);
		}
		this.buttonPositions = new ImagePosition[3];
		this.timeFormat = new SimpleDateFormat("HH:mm");
		OPTION_DYNAMIC_BACKGROUND.addListener(this::updateBackground);
	}

	@Override
	protected void revalidate() {

		this.barHeight = height * 0.1125f;

		this.textMarginX = (int) (width * 0.015f);
		this.textTopMarginY = (int) (height * 0.01f);
		this.textLineHeight = (int) (Fonts.MEDIUM.getLineHeight() * 0.925f);

		// initialize music buttons
		final int musicSize = (int) (this.textLineHeight * 0.8f);
		final float musicScale = (float) musicSize / MUSIC_STOP.getWidth();
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
		downloadsButton = new MenuButton(dlImg, width - dlImg.getWidth() / 2f, height2);
		downloadsButton.setHoverAnimationDuration(350);
		downloadsButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_BACK);
		downloadsButton.setHoverExpand(1.03f, Expand.LEFT);

		// initialize repository button (only if a webpage can be opened)
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(BROWSE)) {
			final Image repoImg = GameImage.REPOSITORY.getImage();
			float repoX = this.textMarginX + repoImg.getWidth() / 2;
			final float repoY = height - this.barHeight / 2;
			repoButton = new MenuButton(repoImg, repoX, repoY);
			repoButton.setHoverAnimationDuration(100);
			repoButton.setHoverExpand(1.1f);
			repoX += repoImg.getWidth() * 1.5f;
			danceRepoButton = new MenuButton(repoImg, repoX, repoY);
			danceRepoButton.setHoverAnimationDuration(100);
			danceRepoButton.setHoverExpand(1.1f);
		}

		// initialize update buttons
		final float updateY = height * 17 / 18f;
		final Image downloadImg = GameImage.DOWNLOAD.getImage();
		updateButton = new MenuButton(downloadImg, width2, updateY);
		updateButton.setHoverAnimationDuration(400);
		updateButton.setHoverAnimationEquation(AnimationEquation.IN_OUT_QUAD);
		updateButton.setHoverExpand(1.1f);
		final Image updateImg = GameImage.UPDATE.getImage();
		restartButton = new MenuButton(updateImg, width2, updateY);
		restartButton.setHoverAnimationDuration(2000);
		restartButton.setHoverAnimationEquation(AnimationEquation.LINEAR);
		restartButton.setHoverRotate(360);

		// initialize star fountain
		starFountain = new StarFountain(width, height);

		// logo & buttons
		this.logo = new ImagePosition(MENU_LOGO.getImage());
		this.logoPositionOffsetX = 0.35f * MENU_LOGO.getHeight();
		this.logoPosition.setValues(0,  logoPositionOffsetX);
		this.buttonsX = width2 - MENU_OPTIONS.getWidth() / 2;
		this.buttonPositions[0] = new ImagePosition(MENU_PLAY.getImage());
		this.buttonPositions[1] = new ImagePosition(MENU_OPTIONS.getImage());
		this.buttonPositions[2] = new ImagePosition(MENU_EXIT.getImage());
		final int basey = height2 - MENU_OPTIONS.getHeight() / 2;
		final float yoffset = MENU_LOGO.getHeight() * 0.196378f;
		for (int i = 0; i < 3; i++) {
			this.buttonPositions[i].width = MENU_OPTIONS.getWidth();
			this.buttonPositions[i].y = (int) (basey + (i - 1f) * yoffset);
			this.buttonPositions[i].height = MENU_OPTIONS.getHeight();
		}
	}

	@Override
	public void render(Graphics g) {
		// draw background
		Beatmap beatmap = MusicController.getBeatmap();
		if (!OPTION_DYNAMIC_BACKGROUND.state ||
			beatmap == null ||
			!beatmap.drawBackground(width, height, bgAlpha.getValue(), true))
		{
			Image bg = GameImage.MENU_BG.getImage();
			bg.setAlpha(bgAlpha.getValue());
			bg.draw();
		}

		// top/bottom horizontal bars
		float oldAlpha = Colors.BLACK_ALPHA.a;
		Colors.BLACK_ALPHA.a = 0.4f;
		g.setColor(Colors.BLACK_ALPHA);
		g.fillRect(0, 0, width, this.barHeight);
		g.fillRect(0, height - this.barHeight, width, this.barHeight);
		Colors.BLACK_ALPHA.a = oldAlpha;

		// draw star fountain
		if (OPTION_STARFOUNTAINS.state) {
			starFountain.draw();
		}

		// draw downloads button
		downloadsButton.draw();
		
		// calculate scale stuff for logo
		final float clickScale = this.logoClickScale.getValue();
		final Float boxedBeatPosition = MusicController.getBeatProgress();
		final Float boxedBeatLength = MusicController.getBeatLength();
		final boolean renderPiece = boxedBeatPosition != null;
		final float beatPosition, beatLength;
		if (boxedBeatPosition == null || boxedBeatLength == null) {
			beatPosition = System.currentTimeMillis() % 1000 / 1000f;
			beatLength = 1000f;
		} else {
			beatPosition = (float) boxedBeatPosition;
			beatLength = (float) boxedBeatLength;
		}
		final float hoverScale = this.logoHover.getValue();
		if (beatPosition < this.lastPulseProgress) {
			this.pulseData.add(new PulseData((int) (beatPosition*beatLength), hoverScale));
		}
		this.lastPulseProgress = beatPosition;
		final float smoothExpandProgress;
		if (beatPosition < 0.05f) {
			smoothExpandProgress = 1f - IN_CUBIC.calc(beatPosition / 0.05f);
		} else {
			smoothExpandProgress = (beatPosition - 0.05f) / 0.95f;
		}
		final float logoScale = (0.9726f + smoothExpandProgress * 0.0274f) * clickScale;
		final float totalLogoScale = hoverScale * logoScale;
		
		// pulse ripples
		final Color logoColor;
		if (OPTION_COLOR_MAIN_MENU_LOGO.state) {
			logoColor = new Color(0xFF000000 | cursorColor.getCurrentColor());
		} else {
			logoColor = Color.white;
		}
		for (PulseData pd : this.pulseData) {
			final float progress = OUT_CUBIC.calc(pd.position / 1000f);
			final float scale = (pd.initialScale + (0.432f * progress)) * clickScale;
			final Image p = MENU_LOGO_PULSE.getScaledImage(scale);
			p.setAlpha(0.15f * (1f - IN_QUAD.calc(progress)));
			p.drawCentered(this.logo.middleX(), this.logo.middleY(), logoColor);
		}

		// draw buttons
		final float buttonProgress = this.buttonAnimation.getValue();
		if (this.logoState != LogoState.DEFAULT && buttonProgress > 0f) {
			final int btnwidth = MENU_OPTIONS.getWidth();
			final float btnhalfheight = MENU_OPTIONS.getHeight() / 2f;
			final int basey = height2;
			final int x = (int) (this.buttonsX + btnwidth * 0.375f * buttonProgress);
			final Color col = new Color(logoColor);
			final Image[] imgs = {
				MENU_PLAY.getImage(),
				MENU_OPTIONS.getImage(),
				MENU_EXIT.getImage()
			};
			final float circleradius = MENU_LOGO.getHeight() * 0.44498f;
			final float yoffset = MENU_LOGO.getHeight() * 0.196378f;
			final float cr = circleradius * totalLogoScale;
			for (int i = 0; i < 3; i++) {
				final float hoverprogress = this.buttonAnimations[i].getValue();
				final int bx = x + (int) (btnwidth * 0.075f * hoverprogress);
				this.buttonPositions[i].x = bx;
				final float yoff = (i - 1f) * yoffset;
				final double cliptop = cr * cos(asin((yoff - btnhalfheight) / cr));
				final double clipbot = cr * cos(asin((yoff + btnhalfheight) / cr));
				final float clipxstart = bx - this.logo.middleX();
				final int ct = (int) (cliptop - clipxstart);
				final int cb = (int) (clipbot - clipxstart);
				final int y = (int) (basey + yoff);
				col.a = buttonProgress * 0.85f + hoverprogress * 0.15f;
				this.drawMenuButton(imgs[i], bx, y, ct, cb, col);
			}
		}

		// draw logo
		this.logo.scale(logoScale);
		this.logo.draw(logoColor);
		if (renderPiece) {
			final Image piece = MENU_LOGO_PIECE.getScaledImage(hoverScale * logoScale);
			piece.rotate(beatPosition * 360f);
			piece.drawCentered(this.logo.middleX(), this.logo.middleY(), logoColor);
		}
		final float ghostScale = hoverScale * 1.0186f - smoothExpandProgress * 0.0186f;
		Image ghostLogo = MENU_LOGO.getScaledImage(ghostScale * clickScale);
		ghostLogo.setAlpha(0.25f);
		ghostLogo.drawCentered(this.logo.middleX(), this.logo.middleY(), logoColor);
		
		// now playing
		final Image np = GameImage.MUSIC_NOWPLAYING.getImage();
		final String trackText;
		if (beatmap != null) {
			if (OPTION_SHOW_UNICODE.state) {
				Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.titleUnicode);
				Fonts.loadGlyphs(Fonts.MEDIUM, beatmap.artistUnicode);
			}
			trackText = beatmap.getArtist() + ": " + beatmap.getTitle();
		} else {
			trackText = "Loading...";
		}
		final float textWidth = Fonts.MEDIUM.getWidth(trackText);
		final float npheight = Fonts.MEDIUM.getLineHeight() * 1.15f;
		final float npscale = npheight / np.getHeight();
		final float npwidth = np.getWidth() * npscale;
		float totalWidth = textMarginX + textWidth + npwidth;
		if (this.nowPlayingPosition.getMax() != totalWidth) {
			final float current = this.nowPlayingPosition.getValue();
			this.nowPlayingPosition.setValues(current, totalWidth);
			this.nowPlayingPosition.setTime(0);
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
			float x, y;
			repoButton.draw();
			text = "opsu!";
			fheight = Fonts.SMALL.getLineHeight();
			fwidth = Fonts.SMALL.getWidth(text);
			x = repoButton.getX() - fwidth / 2;
			y = repoButton.getY() - repoButton.getImage().getHeight() / 2 - fheight;
			Fonts.SMALL.drawString(x, y, text, Color.white);
			danceRepoButton.draw();
			text = "opsu!dance";
			fheight = Fonts.SMALL.getLineHeight();
			fwidth = Fonts.SMALL.getWidth(text);
			x = danceRepoButton.getX() - fwidth / 2;
			y = danceRepoButton.getY() - repoButton.getImage().getHeight() / 2 - fheight;
			Fonts.SMALL.drawString(x, y, text, Color.white);
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
		g.setColor(Color.white);
		String txt = String.format(
			"You have %d beatmaps (%d songs) available!",
			beatmapList.getBeatmapCount(),
			beatmapList.getBeatmapSetCount()
		);
		g.drawString(txt, textMarginX, textTopMarginY);
		txt = String.format(
			"%s has been running for %s.",
			Constants.PROJECT_NAME,
			Utils.getTimeString((int) (Entrypoint.runtime() / 1000L))
		);
		g.drawString(txt, textMarginX, textTopMarginY + textLineHeight);
		txt = String.format(
			"It is currently %s.",
			this.timeFormat.format(new Date())
		);
		g.drawString(txt, textMarginX, textTopMarginY + textLineHeight * 2);
	}

	@Override
	public void preRenderUpdate() {
		int delta = renderDelta;
		
		final Iterator<PulseData> pulseDataIter = this.pulseData.iterator();
		while (pulseDataIter.hasNext()) {
			final PulseData pd = pulseDataIter.next();
			pd.position += delta;
			if (pd.position > 1000) {
				pulseDataIter.remove();
			}
		}

		UI.update(delta);
		if (MusicController.trackEnded()) {
			this.playRandomNextTrack();
		}
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
		if (OPTION_STARFOUNTAINS.state) {
			starFountain.update(delta);
		}

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
			if (OPTION_STARFOUNTAINS.state && measureProgress < lastMeasureProgress)
				starFountain.burst(true);
			lastMeasureProgress = measureProgress;
		}

		// buttons
		this.logo.width = MENU_LOGO.getWidth();
		this.logo.height = MENU_LOGO.getHeight();
		this.logo.x = width2 - this.logo.width / 2;
		this.logo.y = height2 - this.logo.height / 2;
		if (this.logoState != LogoState.DEFAULT) {
			this.logo.x -= (int) this.logoPosition.getValue();
		}
		switch (logoState) {
		case DEFAULT:
			break;
		case OPENING:
			if (logoPosition.update(delta)) {
				this.buttonAnimation.update(delta);
			} else {
				this.buttonAnimation.setTime(this.buttonAnimation.getDuration());
				logoState = LogoState.OPEN;
				logoTimer = 0;
				logoButtonAlpha.setTime(0);
			}
			break;
		case OPEN:
			logoButtonAlpha.update(delta);
			if (this.lastMouseX != mouseX || this.lastMouseY != mouseY) {
				this.logoTimer = 0;
				this.lastMouseX = mouseX;
				this.lastMouseY = mouseY;
			} else {
				this.logoTimer += delta;
				if (this.logoTimer >= LOGO_IDLE_DELAY) {
					this.closeLogo();
				}
			}
			break;
		case CLOSING:
			logoButtonAlpha.update(-delta);
			if (logoPosition.update(-delta)) {
				this.buttonAnimation.update(-delta);
			} else {
				this.logoState = LogoState.DEFAULT;
				this.buttonAnimation.setTime(0);
			}
			break;
		}
		this.logoClickScale.update(delta);
		final boolean logoHovered = this.logo.contains(mouseX, mouseY, 0.25f);
		if (logoHovered && !displayContainer.suppressHover) {
			this.logoHover.update(delta);
		} else {
			this.logoHover.update(-delta);
		}
		final float hoverScale = this.logoHover.getValue();
		if (hoverScale != 1f) {
			this.logo.scale(hoverScale);
		}
		for (int i = 0; i < 3; i++) {
			final ImagePosition pos = this.buttonPositions[i];
			final AnimatedValue anim = this.buttonAnimations[i];
			if (!logoHovered && pos.contains(mouseX, mouseY, 0.25f)) {
				if (anim.getDuration() != 500) {
					anim.change(500, 0f, 1f, OUT_ELASTIC);
					continue;
				}
				anim.update(delta);
				continue;
			}

			if (anim.getDuration() != 350) {
				anim.change(350, 0f, 1f, IN_QUAD);
				continue;
			}
			anim.update(-delta);
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

		logoPosition.setTime(0);
		logoButtonAlpha.setTime(0);
		nowPlayingPosition.setTime(0);
		logoState = LogoState.DEFAULT;
		this.logoClickScale.setTime(this.logoClickScale.getDuration());
		this.buttonAnimation.setTime(0);

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
	public void leave()
	{
		optionsOverlay.hide();
		super.leave();
		if (MusicController.isTrackDimmed())
			MusicController.toggleTrackDimmed(1f);
	}

	@Override
	public void mousePressed(MouseEvent evt)
	{
		if (evt.button == Input.MMB) {
			return;
		}

		final int x = evt.x;
		final int y = evt.y;

		// music position bar
		if (MusicController.isPlaying() && musicPositionBarContains(x, y)) {
			this.lastMeasureProgress = 0f;
			float pos = (float) (x - this.musicBarX) / this.musicBarWidth;
			MusicController.setPosition((int) (pos * MusicController.getDuration()));
			return;
		}

		// music button actions
		if (musicPrev.contains(x, y)) {
			lastMeasureProgress = 0f;
			if (!songHistory.isEmpty()) {
				// songHistory will be popped by MusicController#play
				this.playNextTrack(songHistory.peek());
			} else {
				MusicController.setPosition(0);
			}
			barNotifs.send("<< Previous");
			return;
		} else if (musicPlay.contains(x, y)) {
			if (MusicController.isPlaying()) {
				lastMeasureProgress = 0f;
				MusicController.setPosition(0);
			} else if (!MusicController.isTrackLoading()) {
				MusicController.resume();
			}
			barNotifs.send("Play");
			return;
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
			if (!nextSongs.isEmpty()) {
				// nextSongs is popped by MusicController#play
				this.playNextTrack(nextSongs.peek());
			} else {
				this.playRandomNextTrack();
			}
			barNotifs.send(">> Next");
			return;
		}

		// downloads button actions
		if (downloadsButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			displayContainer.switchState(downloadState);
			return;
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
			return;
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
			return;
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
			} else if (restartButton.contains(x, y) && status == Updater.Status.UPDATE_DOWNLOADED) {
				SoundController.playSound(SoundEffect.MENUHIT);
				updater.prepareUpdate();
				displayContainer.exitRequested = true;
			}
			return;
		}

		final boolean logoHovered = this.logo.contains(x, y, 0.25f);
		if (logoState == LogoState.DEFAULT || logoState == LogoState.CLOSING) {
			if (logoHovered) {
				this.openLogo();
				SoundController.playSound(SoundEffect.MENUHIT);
				this.logoClickScale.setTime(0);
				return;
			}
		} else {
			if (logoHovered || this.buttonPositions[0].contains(x, y, 0.25f)) {
				this.logoClickScale.setTime(0);
				SoundController.playSound(SoundEffect.MENUHIT);
				enterSongMenu();
				return;
			}

			if (this.buttonPositions[1].contains(x, y, 0.25f)) {
				if (!optionsOverlay.isActive()) {
					SoundController.playSound(SoundEffect.MENUHIT);
					optionsOverlay.show();
				}
				return;
			}

			if (this.buttonPositions[2].contains(x, y, 0.25f)) {
				displayContainer.exitRequested = true;
				return;
			}
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		volumeControl.changeVolume(e.direction);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		switch (e.keyCode) {
		case KEY_ESCAPE:
		case KEY_Q:
			if (logoState == LogoState.OPEN || logoState == LogoState.OPENING) {
				this.closeLogo();
				break;
			}
			buttonState.setMenuState(MenuState.EXIT);
			displayContainer.switchState(buttonState);
			return;
		case KEY_P:
			SoundController.playSound(SoundEffect.MENUHIT);
			if (logoState == LogoState.DEFAULT || logoState == LogoState.CLOSING) {
				this.openLogo();
			} else {
				enterSongMenu();
			}
			return;
		case KEY_D:
			SoundController.playSound(SoundEffect.MENUHIT);
			displayContainer.switchState(downloadState);
			return;
		case KEY_R:
			this.playRandomNextTrack();
			return;
		case KEY_UP:
			volumeControl.changeVolume(1);
			return;
		case KEY_DOWN:
			volumeControl.changeVolume(-1);
			return;
		case KEY_O:
			if (input.isControlDown()) {
				optionsOverlay.show();
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		optionsOverlay.mouseReleased(e);
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
		optionsOverlay.mouseDragged(e);
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

	public void playRandomNextTrack()
	{
		this.playNextTrack(beatmapList.getRandom());
	}

	private void playNextTrack(Beatmap next)
	{
		if (!nodeList.attemptFocusMap(next, /*playAtPreviewTime*/ false)) {
			MusicController.play(next, /*loop*/ false, /*playAtPreviewTime*/ false);
		}
		lastMeasureProgress = 0f;
		this.updateBackground();
	}

	private void updateBackground()
	{
		File newBackgroundFile = null;
		if (OPTION_DYNAMIC_BACKGROUND.state) {
			newBackgroundFile = MusicController.getBeatmap().bg;
		}
		if (!Objects.equals(this.currentBackgroundFile, newBackgroundFile)) {
			this.currentBackgroundFile = newBackgroundFile;
			bgAlpha.setTime(0);
		}
	}

	/**
	 * Enters the song menu, or the downloads menu if no beatmaps are loaded.
	 */
	private void enterSongMenu()
	{
		OpsuState state = songMenuState;
		if (beatmapList.getBeatmapCount() == 0) {
			barNotifs.send("Download some beatmaps to get started!");
			state = downloadState;
		}
		displayContainer.switchState(state);
	}
	
	private void openLogo() {
		buttonAnimation.change(300, 0f, 1f, OUT_QUAD);
		logoPosition.change(300, 0, logoPositionOffsetX, OUT_CUBIC);
		logoState = LogoState.OPENING;
	}
	
	private void closeLogo() {
		buttonAnimation.change(500, 0f, 1f, OUT_QUAD);
		logoPosition.change(1800, 0, logoPositionOffsetX, IN_QUAD);
		logoState = LogoState.CLOSING;
	}
	
	private void drawMenuButton(
		Image img,
		int x,
		int y,
		int clipxtop,
		int clipxbot,
		Color col)
	{
		col.bind();
		final Texture t = img.getTexture();
		t.bind(); 
		
		final int width = img.getWidth();
		final int height = img.getHeight();
		final float twidth = t.getWidth();
		final float theight = t.getHeight();
		y -= height / 2;
		
		final float texXtop = clipxtop > 0 ? (float) clipxtop / width * twidth : 0f;
		final float texXbot = clipxbot > 0 ? (float) clipxbot / width * twidth : 0f;

		GL11.glBegin(SGL.GL_QUADS); 
		GL11.glTexCoord2f(texXtop, 0);
		GL11.glVertex3i(x + clipxtop, y, 0);
		GL11.glTexCoord2f(twidth, 0);
		GL11.glVertex3i(x + width, y, 0);
		GL11.glTexCoord2f(twidth, theight);
		GL11.glVertex3i(x + width, y + height, 0);
		GL11.glTexCoord2f(texXbot, theight);
		GL11.glVertex3i(x + clipxbot, y + height, 0);
		GL11.glEnd(); 
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
