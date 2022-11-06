// Copyright 2017-2020 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import itdelatrisu.opsu.*;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.render.CurveRenderState;
import itdelatrisu.opsu.render.FrameBufferCache;
import itdelatrisu.opsu.replay.PlaybackSpeed;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.cursor.CursorImpl;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;
import org.newdawn.slick.*;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;

import yugecin.opsudance.core.errorhandling.ErrorDumpable;
import yugecin.opsudance.core.input.Input;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.core.state.Renderable;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.events.SkinChangedListener;
import yugecin.opsudance.ui.BackButton;
import yugecin.opsudance.ui.VolumeControl;
import yugecin.opsudance.ui.cursor.Cursor;
import yugecin.opsudance.ui.cursor.NewestCursor;
import yugecin.opsudance.utils.GLHelper;
import yugecin.opsudance.windows.WindowManager;

import java.awt.image.BufferedImage;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static itdelatrisu.opsu.ui.Colors.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * based on org.newdawn.slick.AppGameContainer
 */
public class DisplayContainer implements ErrorDumpable, SkinChangedListener
{
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

	public boolean glReady;
	private String glVersion;
	private String glVendor;

	private long exitconfirmation;

	private final ArrayList<Renderable> overlays;

	private final LinkedList<BackButton.Listener> backButtonListeners;
	/**
	 * set to {@code false} to disable back button next update
	 * has to be set every rendered frame to be effective
	 * for states: in either {@link OpsuState#render} or {@link OpsuState#preRenderUpdate}
	 * for overlays: only effective in {@link Renderable#preRenderUpdate()}
	 */
	public boolean disableBackButton;

	/**
	 * set to {@code true} if something is hovered and none other thing should be marked as such
	 */
	public boolean suppressHover;

	/**
	 * Timestamp when the skin changed, used to prevent a huge update delta after the skin
	 * just changed. Should be {@code 0} when no skin changed last update.
	 */
	public long skinChangeTimestamp;

	public Cursor cursor;
	public boolean drawCursor;

	private final List<ResolutionChangedListener> resolutionChangedListeners;

	private int tIn;
	private int tOut;
	private int tProgress = -1;
	private OpsuState tNextState;
	private float tOVERLAY = 1f;

	public DisplayContainer()
	{
		this.overlays = new ArrayList<>();
		this.resolutionChangedListeners = new ArrayList<>();
		this.backButtonListeners = new LinkedList<>();
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
	public void onSkinChanged(String stringName)
	{
		this.skinChangeTimestamp = System.currentTimeMillis();
		destroyImages();
		reinit();
		UI.updateTooltipLocation();
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

		backButton.revalidate();
		this.reinitCursor();

		// TODO clean this up
		GameMod.init(width, height);
		PlaybackSpeed.init(width, height);
		HitObject.init(width, height);
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
		input.poll();
		this.cursor.reset();

		final int width = Display.getWidth();
		final int height = Display.getHeight();
		ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 32);
		IntBuffer intview = buffer.asIntBuffer();
		IntBuffer buf = IntBuffer.allocate(width * height * 32);
		WindowManager.a = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		WindowManager.b = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] array = new int[width * height];

		while(!exitRequested && !(Display.isCloseRequested() && state.onCloseRequest()) || !confirmExit()) {
			delta = getDelta();

			if (this.skinChangeTimestamp != 0) {
				delta = this.targetUpdateInterval;
				this.skinChangeTimestamp = 0;
			}

			timeSinceLastRender += delta;

			input.poll();
			Music.poll(delta);

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
			cursorColor.update();

			this.suppressHover = false; // put here for volume control

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

				renderDelta = timeSinceLastRender;

				this.disableBackButton = this.backButtonListeners.isEmpty();
				boolean overlayNeedsBackButton = false;
				boolean stateNeedsBackButton = false;

				// clone overlays to have a consistent list in this block
				Renderable[] overlays = Renderable.EMPTY_ARRAY;
				if (!this.overlays.isEmpty()) {
					overlays = this.overlays.toArray(Renderable.EMPTY_ARRAY);
				}

				if (!this.disableBackButton) {
					for (BackButton.Listener l : this.backButtonListeners) {
						overlayNeedsBackButton |= l.isFromOverlay;
						stateNeedsBackButton |= !l.isFromOverlay;
					}
					backButton.preRenderUpdate(!overlayNeedsBackButton);
				}

				for (Renderable overlay : overlays) {
					overlay.preRenderUpdate();
				}

				state.preRenderUpdate();
				state.render(graphics);

				if (stateNeedsBackButton && backButton.hasSkinnedVariant()) {
					backButton.drawSkinned();
				}

				for (Renderable overlay : overlays) {
					overlay.render(graphics);
				}

				if (overlayNeedsBackButton ||
					(stateNeedsBackButton && !backButton.hasSkinnedVariant()))
				{
					backButton.drawDefault(graphics);
				}

				volumeControl.draw();
				fpsDisplay.render(graphics);

				bubNotifs.render(graphics);
				barNotifs.render(graphics);

				if (drawCursor) {
					cursor.draw(Mouse.isButtonDown(Input.LMB) || Mouse.isButtonDown(Input.RMB));
				}
				UI.drawTooltip(graphics);

				// transition
				if (tProgress != -1) {
					if (tProgress > tOut) {
						tOVERLAY = 1f - (tProgress - tOut) / (float) tIn;
					} else {
						tOVERLAY = tProgress / (float) tOut;
					}
					glColor4f(0f, 0f, 0f, tOVERLAY);
					glDisable(GL_TEXTURE_2D);
					glBegin(GL_QUADS);
					glVertex2i(0, 0);
					glVertex2i(width, 0);
					glVertex2i(width, height);
					glVertex2i(0, height);
					glEnd();
				}

				timeSinceLastRender = 0;
				Display.update(false);

		buffer.rewind();
		intview.rewind();
		GL11.glReadBuffer(GL11.GL_FRONT);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
		buf.rewind();

		//buf.put(intview);
		//WindowManager.b.setRGB(0, 0, width, height, buf.array(), 0, width);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int v = intview.get(y * width + x) >>> 8;
				WindowManager.b.setRGB(x, height - (y + 1), 0xFF000000 | v);
			}
		}

		WindowManager.swapBuffers();
		WindowManager.lastGLRender = System.currentTimeMillis();
		WindowManager.updateNow();

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
		Display.setTitle(Constants.PROJECT_NAME);
		setupResolutionOptionlist(nativeDisplayMode.getWidth(), nativeDisplayMode.getHeight());
		updateDisplayMode(OPTION_SCREEN_RESOLUTION.getValueString());
		Display.create();
		GLHelper.setIcons(new String[] { "icon16.png", "icon32.png" });
		postInitGL();
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
		this.glReady = false;
	}

	public void destroyImages() {
		InternalTextureLoader.get().clear();
		GameImage.destroyImages();
		GameData.Grade.destroyImages();
		Beatmap.destroyBackgroundImageCache();
		FrameBufferCache.shutdown();
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

		this.glReady = true;
		this.glVersion = glGetString(GL_VERSION);
		this.glVendor = glGetString(GL_VENDOR);

		graphics = new Graphics(width, height);
		graphics.setAntiAlias(false);

		Keyboard.enableRepeatEvents(true);

		Log.info("GL ready");

		GameImage.onResolutionChanged();
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
		dump.append("state is ");
		if (state != null) {
			dump.append(state.getClass().getSimpleName());
			dump.append("\n");
			state.writeErrorDump(dump);
			dump.append("< DisplayContainer\n");
		} else {
			dump.append("null\n");
		}
		dump.append("tNextState is ");
		if (tNextState != null) {
			dump.append(tNextState.getClass().getSimpleName());
			dump.append("\n");
		} else {
			dump.append("null\n");
		}
		for (Renderable r : overlays) {
			dump.append("overlay ");
			dump.append(state.getClass().getSimpleName());
			dump.append("\n");
			r.writeErrorDump(dump);
			dump.append("< DisplayContainer");
		}
		dump.append("loading sound: ");
		String loadingsound = SoundController.getCurrentFileName();
		if (loadingsound != null) {
			dump.append(loadingsound);
			dump.append(", progress: ");
			dump.append(String.valueOf(SoundController.getLoadingProgress()));
			dump.append("\n");
		} else {
			dump.append("none\n");
		}

		dump.append("playing map: ");
		Beatmap map = MusicController.getBeatmap();
		if (map != null) {
			dump.write(" setid ");
			dump.write(String.valueOf(map.beatmapSetID));
			dump.write(" beatmapid ");
			dump.write(String.valueOf(map.beatmapID));
			dump.write(" name ");
			dump.write(map.artist);
			dump.write(" - ");
			dump.write(map.title);
			dump.write(" [");
			dump.write(map.version);
			dump.write("]\n");
			dump.write(" at time " + MusicController.getPosition() + " \n");
		} else {
			dump.write("null\n");
		}
	}

	public boolean isIn(OpsuState state)
	{
		return this.state == state;
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
		if (this.rendering) {
			// state might be changed in preRenderUpdate,
			// in that case the new state will be rendered without having
			// preRenderUpdate being called first, so do that now
			this.state.preRenderUpdate();
		}
	}

	public void addBackButtonListener(BackButton.Listener listener)
	{
		this.backButtonListeners.add(listener);
		backButton.activeListener = listener;
	}

	public void removeBackButtonListener(BackButton.Listener listener)
	{
		if (!this.backButtonListeners.remove(listener)) {
			return;
		}
		if (this.backButtonListeners.isEmpty()) {
			backButton.resetHover();
			this.disableBackButton = true;
		} else {
			backButton.activeListener = this.backButtonListeners.getLast();
		}
	}

	public void addOverlay(Renderable overlay)
	{
		this.overlays.add(overlay);
	}

	public void removeOverlay(Renderable overlay)
	{
		this.overlays.remove(overlay);
	}

	public boolean hasActiveOverlays()
	{
		return !this.overlays.isEmpty();
	}
}
