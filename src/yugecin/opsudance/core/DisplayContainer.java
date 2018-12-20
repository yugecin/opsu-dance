/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017-2018 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.core;

import itdelatrisu.opsu.*;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.downloads.DownloadList;
import itdelatrisu.opsu.downloads.DownloadNode;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.render.CurveRenderState;
import itdelatrisu.opsu.render.FrameBufferCache;
import itdelatrisu.opsu.replay.PlaybackSpeed;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.cursor.CursorImpl;

import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.errorhandling.ErrorDumpable;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.events.SkinChangedListener;
import yugecin.opsudance.ui.BackButton;
import yugecin.opsudance.ui.VolumeControl;
import yugecin.opsudance.ui.cursor.Cursor;
import yugecin.opsudance.ui.cursor.NewestCursor;
import yugecin.opsudance.utils.GLHelper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static itdelatrisu.opsu.ui.Colors.*;
import static yugecin.opsudance.core.Entrypoint.sout;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * based on org.newdawn.slick.AppGameContainer
 */
public class DisplayContainer implements ErrorDumpable, SkinChangedListener {

	private static SGL GL = Renderer.get();

	private OpsuState state;

	public final DisplayMode nativeDisplayMode;

	private Graphics graphics;

	private int targetUpdatesPerSecond;
	public int targetUpdateInterval;
	private int targetRendersPerSecond;
	public int targetRenderInterval;
	public int targetBackgroundRenderInterval;

	private boolean rendering;
	public int delta;

	public boolean exitRequested;

	public int timeSinceLastRender;

	private long lastFrame;

	private boolean wasMusicPlaying;

	private String glVersion;
	private String glVendor;

	private long exitconfirmation;

	public Cursor cursor;
	public boolean drawCursor;
	
	private final List<ResolutionChangedListener> resolutionChangedListeners;

	private int tIn;
	private int tOut;
	private int tProgress = -1;
	private OpsuState tNextState;
	private final Color tOVERLAY = new Color(Color.black);

	public DisplayContainer()
	{
		this.resolutionChangedListeners = new ArrayList<>();
		drawCursor = true;

		skinservice.addSkinChangedListener(this);

		this.nativeDisplayMode = Display.getDisplayMode();
		targetBackgroundRenderInterval = 41; // ~24 fps
		lastFrame = getTime();
		delta = 1;
		renderDelta = 1;
	}

	public void addResolutionChangedListener(ResolutionChangedListener l)
	{
		this.resolutionChangedListeners.add(l);
	}

	@Override
	public void onSkinChanged(String stringName) {
		destroyImages();
		reinit();
	}

	private void reinit() {
		// this used to be in Utils.init
		// TODO find a better place for this?
		setFPS(targetFPS[targetFPSIndex]);
		MusicController.setMusicVolume(OPTION_MUSIC_VOLUME.val / 100f * OPTION_MASTER_VOLUME.val / 100f);

		skinservice.loadSkin();

		// initialize game images
		for (GameImage img : GameImage.values()) {
			if (img.isPreload()) {
				img.setDefaultImage();
			}
		}

		backButton = new BackButton();
		this.reinitCursor();

		// TODO clean this up
		GameMod.init(width, height);
		PlaybackSpeed.init(width, height);
		HitObject.init(width, height);
		DownloadNode.init(width, height);
	}
	
	public void reinitCursor()
	{
		if (this.cursor != null) {
			this.cursor.destroy();
		}

		if (OPTION_NEWEST_CURSOR.state) {
			this.cursor = new NewestCursor();
		} else {
			this.cursor = new CursorImpl();
		}
	}

	public void setUPS(int ups) {
		targetUpdatesPerSecond = ups;
		targetUpdateInterval = 1000 / targetUpdatesPerSecond;
	}

	public void setFPS(int fps) {
		targetRendersPerSecond = fps;
		targetRenderInterval = 1000 / targetRendersPerSecond;
	}

	public void init(OpsuState startingState) {
		setUPS(targetUPS[OPTION_TARGET_UPS.val]);
		setFPS(targetFPS[targetFPSIndex]);

		state = startingState;
		state.enter();
	}


	public void run() throws Exception {
		input.poll(width, height);
		mouseX = input.getMouseX();
		mouseY = input.getMouseY();
		this.cursor.reset();

		while(!exitRequested && !(Display.isCloseRequested() && state.onCloseRequest()) || !confirmExit()) {
			delta = getDelta();

			timeSinceLastRender += delta;

			input.poll(width, height);
			Music.poll(delta);
			mouseX = input.getMouseX();
			mouseY = input.getMouseY();

			// state transition
			if (tProgress != -1) {
				tProgress += delta;
				if (tProgress > tOut && tNextState != null) {
					switchStateInstantly(tNextState);
					tNextState = null;
				}
				if (tProgress > tIn + tOut) {
					tProgress = -1;
				}
			}
			fpsDisplay.update();

			volumeControl.updateHover();
			state.update();
			if (drawCursor) {
				cursor.setCursorPosition(mouseX, mouseY);
			}

			int maxRenderInterval;
			if (Display.isVisible() && Display.isActive()) {
				maxRenderInterval = targetRenderInterval;
			} else {
				maxRenderInterval = targetBackgroundRenderInterval;
			}

			if (timeSinceLastRender >= maxRenderInterval) {
				rendering = true;
				GL.glClear(SGL.GL_COLOR_BUFFER_BIT);

				/*
				graphics.resetTransform();
				graphics.resetFont();
				graphics.resetLineWidth();
				graphics.resetTransform();
				*/

				renderDelta = timeSinceLastRender;

				state.preRenderUpdate();
				state.render(graphics);

				volumeControl.draw();
				fpsDisplay.render(graphics);

				bubNotifs.render(graphics);
				barNotifs.render(graphics);

				if (drawCursor) {
					cursor.draw(Mouse.isButtonDown(Input.MOUSE_LEFT_BUTTON) ||
						Mouse.isButtonDown(Input.MOUSE_RIGHT_BUTTON));
				}
				UI.drawTooltip(graphics);

				// transition
				if (tProgress != -1) {
					if (tProgress > tOut) {
						tOVERLAY.a = 1f - (tProgress - tOut) / (float) tIn;
					} else {
						tOVERLAY.a = tProgress / (float) tOut;
					}
					graphics.setColor(tOVERLAY);
					graphics.fillRect(0, 0, width, height);
				}

				timeSinceLastRender = 0;

				Display.update(false);
				GL11.glFlush();
				rendering = false;
			}

			Display.processMessages();
			if (targetUpdatesPerSecond >= 60) {
				Display.sync(targetUpdatesPerSecond);
			}
		}
	}

	public void setup() throws Exception {
		width = height = width2 = height2 = -1;
		Display.setTitle("opsu!dance");
		setupResolutionOptionlist(nativeDisplayMode.getWidth(), nativeDisplayMode.getHeight());
		updateDisplayMode(OPTION_SCREEN_RESOLUTION.getValueString());
		Display.create();
		GLHelper.setIcons(new String[] { "icon16.png", "icon32.png" });
		postInitGL();
		glVersion = GL11.glGetString(GL11.GL_VERSION);
		glVendor = GL11.glGetString(GL11.GL_VENDOR);
		GLHelper.hideNativeCursor();
	}

	// TODO: move this elsewhere
	private void setupResolutionOptionlist(int width, int height) {
		final Object[] resolutions = OPTION_SCREEN_RESOLUTION.getListItems();
		final String nativeRes = width + "x" + height;
		resolutions[0] = nativeRes;
		for (int i = 0; i < resolutions.length; i++) {
			if (nativeRes.equals(resolutions[i].toString())) {
				resolutions[i] = resolutions[i] + " (borderless)";
			}
		}

	}

	public void teardown() {
		destroyImages();
		CurveRenderState.shutdown();
		VolumeControl.destroyProgram();
		Display.destroy();
	}

	public void destroyImages() {
		InternalTextureLoader.get().clear();
		GameImage.destroyImages();
		GameData.Grade.destroyImages();
		Beatmap.destroyBackgroundImageCache();
		FrameBufferCache.shutdown();
	}

	public void teardownAL() {
		AL.destroy();
	}

	public void pause() {
		wasMusicPlaying = MusicController.isPlaying();
		if (wasMusicPlaying) {
			MusicController.pause();
		}
	}

	public void resume() {
		if (wasMusicPlaying) {
			MusicController.resume();
		}
	}

	private boolean confirmExit() {
		if (System.currentTimeMillis() - exitconfirmation < 10000) {
			return true;
		}
		if (DownloadList.get().hasActiveDownloads()) {
			bubNotifs.send(BUB_RED, DownloadList.EXIT_CONFIRMATION);
			exitRequested = false;
			exitconfirmation = System.currentTimeMillis();
			return false;
		}
		if (updater.getStatus() == Updater.Status.UPDATE_DOWNLOADING) {
			bubNotifs.send(BUB_PURPLE, Updater.EXIT_CONFIRMATION);
			exitRequested = false;
			exitconfirmation = System.currentTimeMillis();
			return false;
		}
		return true;
	}

	public void updateDisplayMode(String resolutionString) {
		int screenWidth = nativeDisplayMode.getWidth();
		int screenHeight = nativeDisplayMode.getHeight();

		int eos = resolutionString.indexOf(' ');
		if (eos > -1) {
			resolutionString = resolutionString.substring(0, eos);
		}

		int width = screenWidth;
		int height = screenHeight;
		if (resolutionString.matches("^[0-9]+x[0-9]+$")) {
			String[] res = resolutionString.split("x");
			width = Integer.parseInt(res[0]);
			height = Integer.parseInt(res[1]);
		}
		
		updateDisplayMode(width, height);
	}
	
	public void updateDisplayMode(int width, int height) {
		int screenWidth = nativeDisplayMode.getWidth();
		int screenHeight = nativeDisplayMode.getHeight();

		// check for larger-than-screen dimensions
		if (!OPTION_ALLOW_LARGER_RESOLUTIONS.state && (screenWidth < width || screenHeight < height)) {
			width = 800;
			height = 600;
		}

		if (!OPTION_FULLSCREEN.state) {
			boolean borderless = (screenWidth == width && screenHeight == height);
			System.setProperty("org.lwjgl.opengl.Window.undecorated", Boolean.toString(borderless));
		}

		try {
			setDisplayMode(width, height, OPTION_FULLSCREEN.state);
		} catch (Exception e) {
			bubNotifs.send(BUB_RED, "Failed to change display mode");
			Log.error("Failed to set display mode.", e);
		}
	}

	public void setDisplayMode(int w, int h, boolean fullscreen) throws Exception {
		DisplayMode displayMode = null;
		if (fullscreen) {
			final int bpp = this.nativeDisplayMode.getBitsPerPixel();
			final int freq = this.nativeDisplayMode.getFrequency();
			displayMode = GLHelper.findFullscreenDisplayMode(bpp, freq, w, h);
		}

		if (displayMode == null) {
			displayMode = new DisplayMode(w, h);
			if (fullscreen) {
				fullscreen = false;
				String msg = "Fullscreen mode is not supported for %sx%s";
				msg = String.format(msg, w, h);
				Log.warn(msg);
				bubNotifs.send(BUB_ORANGE, msg);
			}
		}

		width = displayMode.getWidth();
		height = displayMode.getHeight();
		width2 = width / 2;
		height2 = height / 2;
		isWidescreen =  width * 1000 / height > 1500; // 1777 = 16:9, 1333 = 4:3

		Display.setDisplayMode(displayMode);
		Display.setFullscreen(fullscreen);

		if (Display.isCreated()) {
			postInitGL();
		}

		if (displayMode.getBitsPerPixel() == 16) {
			InternalTextureLoader.get().set16BitMode();
		}
	}

	private void postInitGL() throws Exception
	{
		GL.initDisplay(width, height);
		GL.enterOrtho(width, height);

		graphics = new Graphics(width, height);
		graphics.setAntiAlias(false);

		if (input == null) {
			input = new Input(height);
			input.enableKeyRepeat();
			input.addListener(new GlobalInputListener());
			input.addMouseListener(bubNotifs);
		}
		input.addListener(state);

		sout("GL ready");

		GameImage.init(width, height);
		Fonts.init();

		if (volumeControl == null) {
			volumeControl = new VolumeControl();
		}

		destroyImages();
		reinit();
		VolumeControl.createProgram();

		barNotifs.onResolutionChanged(width, height);
		bubNotifs.onResolutionChanged(width, height);
		fpsDisplay.onResolutionChanged(width, height);
		for (ResolutionChangedListener l : this.resolutionChangedListeners) {
			l.onResolutionChanged(width, height);
		}
	}

	private int getDelta() {
		long time = getTime();
		int delta = (int) (time - lastFrame);
		lastFrame = time;
		return delta;
	}

	public long getTime() {
		return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}

	@Override
	public void writeErrorDump(StringWriter dump) {
		dump.append("> DisplayContainer dump\n");
		dump.append("OpenGL version: ").append(glVersion).append( "(").append(glVendor).append(")\n");
		if (state == null) {
			dump.append("state is null!\n");
			return;
		}
		state.writeErrorDump(dump);
	}

	// TODO change this
	public boolean isInState(Class<? extends OpsuState> state) {
		return state.isInstance(state);
	}

	public void switchState(OpsuState state) {
		switchState(state, 150, 250);
	}

	public void switchState(OpsuState newstate, int outtime, int intime) {
		if (tProgress != -1 && tProgress <= tOut) {
			return;
		}

		if (outtime == 0) {
			switchStateInstantly(newstate);
			newstate = null;
		} else {
			input.removeListener(this.state);
		}

		if (tProgress == -1) {
			tProgress = 0;
		} else {
			// we were in a transition (out state), so start from the time
			// that was already spent transitioning in
			tProgress = (int) (((1f - (tProgress - tOut) / (float) tIn)) * outtime);
		}

		tNextState = newstate;
		tIn = intime;
		tOut = outtime;
	}

	public void switchStateInstantly(OpsuState state) {
		this.state.leave();
		input.removeListener(this.state);
		this.state = state;
		this.state.enter();
		input.addListener(this.state);
		backButton.resetHover();
		if (this.rendering) {
			// state might be changed in preRenderUpdate,
			// in that case the new state will be rendered without having
			// preRenderUpdate being called first, so do that now
			this.state.preRenderUpdate();
		}
	}

}
