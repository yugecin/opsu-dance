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

import com.google.inject.Inject;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.states.EmptyRedState;
import yugecin.opsudance.utils.GLHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * based on org.newdawn.slick.AppGameContainer
 */
public class DisplayContainer {

	private static SGL GL = Renderer.get();

	private final Demux demux;
	private final DisplayMode nativeDisplayMode;
	private final List<ResolutionChangeListener> resolutionChangeListeners;

	private Graphics graphics;

	public int width;
	public int height;

	private long lastFrame;

	@Inject
	public DisplayContainer(Demux demux) {
		this.demux = demux;
		this.nativeDisplayMode = Display.getDisplayMode();
		this.resolutionChangeListeners = new LinkedList<>();
		lastFrame = getTime();
	}

	public void addResolutionChangeListener(ResolutionChangeListener listener) {
		resolutionChangeListeners.add(listener);
	}

	public void run() throws LWJGLException {
		demux.init();
		demux.switchStateNow(new EmptyRedState(null, null));
		setup();
		while(!(Display.isCloseRequested() && demux.onCloseRequest())) {
			// TODO: lower fps when not visible Display.isVisible
			int delta = getDelta();
			GL.glClear(SGL.GL_COLOR_BUFFER_BIT);
			/*
			graphics.resetTransform();
			graphics.resetFont();
			graphics.resetLineWidth();
			graphics.resetTransform();
			*/
			demux.update(delta);
			demux.render(graphics);
			Display.update(true);
			Display.sync(60);
		}
		teardown();
	}

	private void setup() {
		Display.setTitle("opsu!dance");
		try {
			// temp displaymode to not flash the screen with a 1ms black window
			Display.setDisplayMode(new DisplayMode(100, 100));
			Display.create();
			GLHelper.setIcons(new String[] { "icon16.png", "icon32.png" });
			setDisplayMode(640, 480, false);
		} catch (LWJGLException e) {
			e.printStackTrace();
			// TODO errorhandler dialog here
			Log.error("could not initialize GL", e);
		}
	}

	private void teardown() {
		Display.destroy();
		AL.destroy();
	}

	public void setDisplayMode(int width, int height, boolean fullscreen) throws LWJGLException {
		if (this.width == width && this.height == height) {
			Display.setFullscreen(fullscreen);
			return;
		}

		DisplayMode displayMode = null;
		if (fullscreen) {
			displayMode = GLHelper.findFullscreenDisplayMode(nativeDisplayMode.getBitsPerPixel(), nativeDisplayMode.getFrequency(), width, height);
		}

		if (displayMode == null) {
			displayMode = new DisplayMode(width,height);
			if (fullscreen) {
				fullscreen = false;
				Log.warn("could not find fullscreen displaymode for " + width + "x" + height);
			}
		}

		this.width = displayMode.getWidth();
		this.height = displayMode.getHeight();

		Display.setDisplayMode(displayMode);
		Display.setFullscreen(fullscreen);

		initGL();

		for (ResolutionChangeListener resolutionChangeListener : resolutionChangeListeners) {
			resolutionChangeListener.onDisplayResolutionChanged(width, height);
		}

		if (displayMode.getBitsPerPixel() == 16) {
			InternalTextureLoader.get().set16BitMode();
		}

		getDelta();
	}

	private void initGL() {
		GL.initDisplay(width, height);
		GL.enterOrtho(width, height);

		graphics = new Graphics(width, height);
		graphics.setAntiAlias(false);

		/*
		if (input == null) {
			input = new Input(height);
		}
		input.init(height);
		// no need to remove listeners?
		//input.removeAllListeners();
		if (game instanceof InputListener) {
			input.removeListener((InputListener) game);
			input.addListener((InputListener) game);
		}
		*/

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

}
