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
import org.lwjgl.opengl.Display;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.SlickException;

/**
 * based on itdelatrisu.opsu.Container
 */
public class Container extends AppGameContainer {

	@Inject
	public Container(Demux demux) throws SlickException {
		super(demux);
		setShowFPS(false);
	}

	@Override
	public void start() throws SlickException {
		try {
			setup();
			getDelta();
			while (running())
				gameLoop();
		} catch (Exception e) {
		}
		destroy();
	}

	@Override
	protected void gameLoop() throws SlickException {
		int delta = getDelta();
		if (!Display.isVisible() && updateOnlyOnVisible) {
			try { Thread.sleep(100); } catch (Exception e) {}
		} else {
			try {
				updateAndRender(delta);
			} catch (SlickException e) {
				running = false;
				return;
			}
		}
		updateFPS();
		Display.update();
		if (Display.isCloseRequested()) {
			if (game.closeRequested())
				running = false;
		}
	}
}
