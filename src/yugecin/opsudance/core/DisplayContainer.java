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
import org.newdawn.slick.KeyListener;
import org.newdawn.slick.MouseListener;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.renderer.Renderer;
import org.newdawn.slick.opengl.renderer.SGL;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.errorhandling.ErrorDumpable;
import yugecin.opsudance.core.inject.InstanceContainer;
import yugecin.opsudance.core.state.OpsuState;
import yugecin.opsudance.core.state.specialstates.BarNotificationState;
import yugecin.opsudance.core.state.specialstates.BubbleNotificationState;
import yugecin.opsudance.core.state.specialstates.FpsRenderState;
import yugecin.opsudance.core.state.transitions.*;
import yugecin.opsudance.events.ResolutionChangedEvent;
import yugecin.opsudance.utils.GLHelper;

import java.io.StringWriter;

import static yugecin.opsudance.core.Entrypoint.sout;

/**
 * based on org.newdawn.slick.AppGameContainer
 */
public class DisplayContainer implements ErrorDumpable, KeyListener, MouseListener {

	private static SGL GL = Renderer.get();

	public final EventBus eventBus;
	private final InstanceContainer instanceContainer;

	private FpsRenderState fpsState;
	private BarNotificationState barNotifState;
	private BubbleNotificationState bubNotifState;

	private TransitionState outTransitionState;
	private TransitionState inTransitionState;

	private final TransitionFinishedListener outTransitionListener;
	private final TransitionFinishedListener inTransitionListener;

	private OpsuState state;

	private final DisplayMode nativeDisplayMode;

	private Graphics graphics;
	private Input input;

	public int width;
	public int height;

	public int mouseX;
	public int mouseY;

	public int targetRenderInterval;
	public int targetBackgroundRenderInterval;

	public int realRenderInterval;
	public int delta;

	public int timeSinceLastRender;

	private long lastFrame;

	private String glVersion;
	private String glVendor;

	public DisplayContainer(InstanceContainer instanceContainer, EventBus eventBus) {
		this.instanceContainer = instanceContainer;
		this.eventBus = eventBus;

		outTransitionListener = new TransitionFinishedListener() {
			@Override
			public void onFinish() {
				state.leave();
				outTransitionState.getApplicableState().leave();
				state = inTransitionState;
				state.enter();
				inTransitionState.getApplicableState().enter();
			}
		};

		inTransitionListener = new TransitionFinishedListener() {
			@Override
			public void onFinish() {
				state.leave();
				state = inTransitionState.getApplicableState();
			}
		};

		this.nativeDisplayMode = Display.getDisplayMode();
		targetRenderInterval = 16; // ~60 fps
		targetBackgroundRenderInterval = 41; // ~24 fps
		lastFrame = getTime();
		delta = 1;
		realRenderInterval = 1;
	}

	public void init(Class<? extends OpsuState> startingState) {
		state = instanceContainer.provide(startingState);
		state.enter();

		fpsState = instanceContainer.provide(FpsRenderState.class);
		bubNotifState = instanceContainer.provide(BubbleNotificationState.class);
		barNotifState = instanceContainer.provide(BarNotificationState.class);
	}


	public void run() throws LWJGLException {
		while(!(Display.isCloseRequested() && state.onCloseRequest())) {
			delta = getDelta();

			timeSinceLastRender += delta;

			input.poll(width, height);
			mouseX = input.getMouseX();
			mouseY = input.getMouseY();
			state.update(delta);

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

				state.preRenderUpdate(timeSinceLastRender);
				state.render(graphics);
				fpsState.render(graphics);
				bubNotifState.render(graphics, timeSinceLastRender);
				barNotifState.render(graphics, timeSinceLastRender);

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
		input.addKeyListener(this);
		input.addMouseListener(this);

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
		if (isTransitioning()) {
			dump.append("doing a transition\n");
			dump.append("using out transition ").append(outTransitionState.getClass().getSimpleName()).append('\n');
			dump.append("using in  transition ").append(inTransitionState.getClass().getSimpleName()).append('\n');
			if (state == inTransitionState) {
				dump.append("currently doing the in transition\n");
			} else {
				dump.append("currently doing the out transition\n");
			}
		}
		state.writeErrorDump(dump);
	}

	public boolean isTransitioning() {
		return state instanceof TransitionState;
	}

	public void switchState(Class<? extends OpsuState> newState) {
		switchState(newState, FadeOutTransitionState.class, 200, FadeInTransitionState.class, 300);
	}

	public void switchStateNow(Class<? extends OpsuState> newState) {
		switchState(newState, EmptyTransitionState.class, 0, EmptyTransitionState.class, 0);
	}

	public void switchState(Class<? extends OpsuState> newState, Class<? extends TransitionState> outTransition, int outTime, Class<? extends TransitionState> inTransition, int inTime) {
		if (isTransitioning()) {
			return;
		}
		outTransitionState = instanceContainer.provide(outTransition).set(state, outTime, outTransitionListener);
		inTransitionState = instanceContainer.provide(inTransition).set(instanceContainer.provide(newState), inTime, inTransitionListener);
		state = outTransitionState;
		state.enter();
	}

	/*
	 * input events below, see org.newdawn.slick.KeyListener & org.newdawn.slick.MouseListener
	 */

	@Override
	public void keyPressed(int key, char c) {
		state.keyPressed(key, c);
	}

	@Override
	public void keyReleased(int key, char c) {
		state.keyReleased(key, c);
	}

	@Override
	public void mouseWheelMoved(int change) {
		state.mouseWheelMoved(change);
	}

	@Override
	public void mouseClicked(int button, int x, int y, int clickCount) { }

	@Override
	public void mousePressed(int button, int x, int y) {
		state.mousePressed(button, x, y);
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		if (bubNotifState.mouseReleased(x, y)) {
			return;
		}
		state.mouseReleased(button, x, y);
	}

	@Override
	public void mouseMoved(int oldx, int oldy, int newx, int newy) { }

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) { }

	@Override
	public void setInput(Input input) { }

	@Override
	public boolean isAcceptingInput() {
		return true;
	}

	@Override
	public void inputEnded() { }

	@Override
	public void inputStarted() { }

}
