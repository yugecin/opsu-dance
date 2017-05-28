/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
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
import itdelatrisu.opsu.replay.PlaybackSpeed;
import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Cursor;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
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
import yugecin.opsudance.core.state.specialstates.BarNotificationState;
import yugecin.opsudance.core.state.specialstates.BubNotifState;
import yugecin.opsudance.core.state.specialstates.FpsRenderState;
import yugecin.opsudance.events.BubNotifListener;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.events.SkinChangedListener;
import yugecin.opsudance.utils.GLHelper;

import java.io.StringWriter;

import static yugecin.opsudance.core.Entrypoint.sout;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * based on org.newdawn.slick.AppGameContainer
 */
public class DisplayContainer implements ErrorDumpable, ResolutionChangedListener, SkinChangedListener {

	private static SGL GL = Renderer.get();

	private FpsRenderState fpsState;
	private BarNotificationState barNotifState;
	private BubNotifState bubNotifState;

	private OpsuState state;

	public final DisplayMode nativeDisplayMode;

	private Graphics graphics;

	public int width;
	public int height;

	public int mouseX;
	public int mouseY;

	private int targetUpdatesPerSecond;
	public int targetUpdateInterval;
	private int targetRendersPerSecond;
	public int targetRenderInterval;
	public int targetBackgroundRenderInterval;

	public int renderDelta;
	public int delta;

	public boolean exitRequested;

	public int timeSinceLastRender;

	private long lastFrame;

	private boolean wasMusicPlaying;

	private String glVersion;
	private String glVendor;

	private long exitconfirmation;

	public final Cursor cursor;
	public boolean drawCursor;

	class Transition {
		int in;
		int out;
		int total;
		int progress = -1;
		OpsuState nextstate;
		Color OVERLAY = new Color(Color.black);

		public void update() {
			if (progress == -1) {
				return;
			}
			progress += delta;
			if (progress > out && nextstate != null) {
				switchStateInstantly(nextstate);
				nextstate = null;
			}
			if (progress > total) {
				progress = -1;
			}
		}

		public void render(Graphics graphics) {
			if (progress == -1) {
				return;
			}
			int relprogress = progress;
			int reltotal = out;
			if (progress > out) {
				reltotal = in;
				relprogress = total - progress;
			}
			OVERLAY.a = (float) relprogress / reltotal;
			graphics.setColor(OVERLAY);
			graphics.fillRect(0, 0, width, height);
		}
	}

	private final Transition transition = new Transition();

	public DisplayContainer() {
		this.cursor = new Cursor();
		drawCursor = true;

		ResolutionChangedListener.EVENT.addListener(this);
		SkinChangedListener.EVENT.addListener(this);

		this.nativeDisplayMode = Display.getDisplayMode();
		targetBackgroundRenderInterval = 41; // ~24 fps
		lastFrame = getTime();
		delta = 1;
		renderDelta = 1;
	}

	@Override
	public void onResolutionChanged(int w, int h) {
		destroyImages();
		reinit();
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

		// TODO clean this up
		GameMod.init(width, height);
		PlaybackSpeed.init(width, height);
		HitObject.init(width, height);
		DownloadNode.init(width, height);
		UI.init(this);
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
		setUPS(OPTION_TARGET_UPS.val);
		setFPS(targetFPS[targetFPSIndex]);

		fpsState = new FpsRenderState();
		bubNotifState = new BubNotifState();
		barNotifState = new BarNotificationState();

		state = startingState;
		state.enter();
	}


	public void run() throws Exception {
		while(!exitRequested && !(Display.isCloseRequested() && state.onCloseRequest()) || !confirmExit()) {
			delta = getDelta();

			timeSinceLastRender += delta;

			input.poll(width, height);
			Music.poll(delta);
			mouseX = input.getMouseX();
			mouseY = input.getMouseY();

			transition.update();
			fpsState.update();

			state.update();
			if (drawCursor) {
				cursor.setCursorPosition(delta, mouseX, mouseY);
			}

			int maxRenderInterval;
			if (Display.isVisible() && Display.isActive()) {
				maxRenderInterval = targetRenderInterval;
			} else {
				maxRenderInterval = targetBackgroundRenderInterval;
			}

			if (timeSinceLastRender >= maxRenderInterval) {
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
				fpsState.render(graphics);
				bubNotifState.render(graphics);
				barNotifState.render(graphics);

				cursor.updateAngle(renderDelta);
				if (drawCursor) {
					cursor.draw(Mouse.isButtonDown(Input.MOUSE_LEFT_BUTTON) ||
						Mouse.isButtonDown(Input.MOUSE_RIGHT_BUTTON));
				}
				UI.drawTooltip(graphics);

				transition.render(graphics);

				timeSinceLastRender = 0;

				Display.update(false);
			}

			Display.processMessages();
			Display.sync(targetUpdatesPerSecond);
		}
	}

	public void setup() throws Exception {
		width = height = -1;
		Display.setTitle("opsu!dance");
		setupResolutionOptionlist(nativeDisplayMode.getWidth(), nativeDisplayMode.getHeight());
		updateDisplayMode(OPTION_SCREEN_RESOLUTION.getValueString());
		Display.create();
		GLHelper.setIcons(new String[] { "icon16.png", "icon32.png" });
		initGL();
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
		Display.destroy();
	}

	public void destroyImages() {
		InternalTextureLoader.get().clear();
		GameImage.destroyImages();
		GameData.Grade.destroyImages();
		Beatmap.destroyBackgroundImageCache();
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
			BubNotifListener.EVENT.make().onBubNotif(DownloadList.EXIT_CONFIRMATION, Colors.BUB_RED);
			exitRequested = false;
			exitconfirmation = System.currentTimeMillis();
			return false;
		}
		if (updater.getStatus() == Updater.Status.UPDATE_DOWNLOADING) {
			BubNotifListener.EVENT.make().onBubNotif(Updater.EXIT_CONFIRMATION, Colors.BUB_PURPLE);
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
			BubNotifListener.EVENT.make().onBubNotif("Failed to change resolution", Colors.BUB_RED);
			Log.error("Failed to set display mode.", e);
		}
	}

	public void setDisplayMode(int width, int height, boolean fullscreen) throws Exception {
		if (this.width == width && this.height == height) {
			Display.setFullscreen(fullscreen);
			return;
		}

		DisplayMode displayMode = null;
		if (fullscreen) {
			displayMode = GLHelper.findFullscreenDisplayMode(nativeDisplayMode.getBitsPerPixel(), nativeDisplayMode.getFrequency(), width, height);
		}

		if (displayMode == null) {
			displayMode = new DisplayMode(width, height);
			if (fullscreen) {
				fullscreen = false;
				String msg = String.format("Fullscreen mode is not supported for %sx%s", width, height);
				Log.warn(msg);
				BubNotifListener.EVENT.make().onBubNotif(msg, Colors.BUB_ORANGE);
			}
		}

		this.width = displayMode.getWidth();
		this.height = displayMode.getHeight();

		Display.setDisplayMode(displayMode);
		Display.setFullscreen(fullscreen);

		if (Display.isCreated()) {
			initGL();
		}

		if (displayMode.getBitsPerPixel() == 16) {
			InternalTextureLoader.get().set16BitMode();
		}
	}

	private void initGL() throws Exception {
		GL.initDisplay(width, height);
		GL.enterOrtho(width, height);

		graphics = new Graphics(width, height);
		graphics.setAntiAlias(false);

		if (input == null) {
			input = new Input(height);
			input.enableKeyRepeat();
			input.addListener(new GlobalInputListener());
			input.addMouseListener(bubNotifState);
		}
		input.addListener(state);

		sout("GL ready");

		GameImage.init(width, height);
		Fonts.init();

		ResolutionChangedListener.EVENT.make().onResolutionChanged(width, height);
	}

	public void resetCursor() {
		cursor.reset(mouseX, mouseY);
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
		switchState(state, 200, 300);
	}

	public void switchState(OpsuState newstate, int outtime, int intime) {
		if (transition.progress != -1) {
			return;
		}
		if (outtime == 0) {
			switchStateInstantly(newstate);
			newstate = null;
		}
		transition.nextstate = newstate;
		transition.total = transition.in = intime;
		transition.out = outtime;
		transition.total += outtime;
		transition.progress = 0;
	}

	public void switchStateInstantly(OpsuState state) {
		this.state.leave();
		input.removeListener(this.state);
		this.state = state;
		this.state.enter();
		input.addListener(this.state);
	}

}
