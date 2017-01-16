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

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.ui.Fonts;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.errorhandling.ErrorDumpable;
import yugecin.opsudance.events.ResolutionChangedEvent;
import yugecin.opsudance.utils.GLHelper;

import java.io.StringWriter;

import static yugecin.opsudance.core.Entrypoint.sout;

/**
 * based on org.newdawn.slick.AppGameContainer
 */
public class DisplayContainer implements ErrorDumpable {

	private static SGL GL = Renderer.get();

	public final EventBus eventBus;
	public final Demux demux;

	private final DisplayMode nativeDisplayMode;

	private Graphics graphics;
	private Input input;

	public int width;
	public int height;

	public int targetRenderInterval;
	public int targetBackgroundRenderInterval;

	public int realRenderInterval;
	public int delta;

	public int timeSinceLastRender;

	private long lastFrame;

	private String glVersion;
	private String glVendor;

	public DisplayContainer(Demux demux, EventBus eventBus) {
		this.demux = demux;
		this.eventBus = eventBus;
		this.nativeDisplayMode = Display.getDisplayMode();
		targetRenderInterval = 16; // ~60 fps
		targetBackgroundRenderInterval = 41; // ~24 fps
		lastFrame = getTime();
		delta = 1;
		realRenderInterval = 1;
	}

	public void run() throws LWJGLException {
		while(!(Display.isCloseRequested() && demux.onCloseRequest())) {
			delta = getDelta();

			timeSinceLastRender += delta;

			input.poll(width, height);
			demux.update(delta);

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

				demux.preRenderUpdate(timeSinceLastRender);
				demux.render(graphics);

				realRenderInterval = timeSinceLastRender;
				timeSinceLastRender = 0;

				Display.update(false);
			}

			Display.processMessages();
			Display.sync(1000); // TODO add option to change this, to not eat CPUs
		}
		teardown();
	}

	public void setup() throws Exception {
		width = height = -1;
		Input.disableControllers();
		Display.setTitle("opsu!dance");
		// temp displaymode to not flash the screen with a 1ms black window
		Display.setDisplayMode(new DisplayMode(100, 100));
		Display.create();
		GLHelper.setIcons(new String[] { "icon16.png", "icon32.png" });
		setDisplayMode(800, 600, false);
		sout("GL ready");
		glVersion = GL11.glGetString(GL11.GL_VERSION);
		glVendor = GL11.glGetString(GL11.GL_VENDOR);
	}

	public void teardown() {
		Display.destroy();
		AL.destroy();
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

		eventBus.post(new ResolutionChangedEvent(this.width, this.height));

		if (displayMode.getBitsPerPixel() == 16) {
			InternalTextureLoader.get().set16BitMode();
		}
	}

	private void initGL() throws Exception {
		GL.initDisplay(width, height);
		GL.enterOrtho(width, height);

		graphics = new Graphics(width, height);
		graphics.setAntiAlias(false);

		input = new Input(height);
		input.addKeyListener(demux);
		input.addMouseListener(demux);

		GameImage.init(width, height);
		Fonts.init();
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
		demux.writeErrorDump(dump);
	}

}
