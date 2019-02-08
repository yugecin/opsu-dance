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
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.db.ScoreDB;
import itdelatrisu.opsu.ui.UI;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.util.Log;

import yugecin.opsudance.core.input.KeyEvent;
import yugecin.opsudance.core.state.BaseOpsuState;

import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * "Splash Screen" state.
 * <p>
 * Loads game resources and enters "Main Menu" state.
 */
public class Splash extends BaseOpsuState {

	/** Whether or not loading has completed. */
	private boolean finished;

	/** Loading thread. */
	private Thread thread;

	/** Number of times the escape key has been pressed, to exit. */
	private int escapeCount = 0;

	/** Wether the loading progress was inited. */
	private boolean inited;

	@Override
	protected void revalidate() {
		super.revalidate();

		// pre-revalidate some states to reduce lag between switching
		songMenuState.revalidate();

		if (inited) {
			return;
		}

		inited = true;
		thread = new Thread() {
			@Override
			public void run() {
				oszunpacker.unpackAll();
				beatmapParser.parseAll();
				ScoreDB.loadTopGrades();
				beatmapList.activeGroupChanged();
				replayImporter.importAll();

				SoundController.init();

				finished = true;
				thread = null;
			}
		};
		thread.start();
	}

	@Override
	public void preRenderUpdate() {
		// change states when loading complete
		if (!finished) {
			return;
		}

		nodeList.recreate();
		if (OPTION_ENABLE_THEME_SONG.state || beatmapList.getBeatmapSetCount() == 0) {
			MusicController.playThemeSong();
		} else {
			mainmenuState.playRandomNextTrack();
		}
		displayContainer.switchStateInstantly(mainmenuState);
	}

	@Override
	public void render(Graphics g) {
		g.setBackground(Color.black);
		final Color col;
		if (OPTION_COLOR_MAIN_MENU_LOGO.state) {
			col = new Color(0xFF000000 | cursorColor.getCurrentColor());
		} else {
			col = Color.white;
		}
		GameImage.MENU_LOGO.getImage().drawCentered(width2, height2, col);
		UI.drawLoadingProgress(g);
	}


	@Override
	public boolean onCloseRequest() {
		if (thread == null || !thread.isAlive()) {
			return true;
		}

		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
			Log.warn("InterruptedException while waiting for splash thread to die", e);
		}
		return true;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.keyCode != Keyboard.KEY_ESCAPE) {
			return;
		}
		if (++escapeCount >= 3) {
			displayContainer.exitRequested = true;
		} else if (thread != null) {
			thread.interrupt();
		}
	}

}
