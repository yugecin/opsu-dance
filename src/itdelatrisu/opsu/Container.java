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

package itdelatrisu.opsu;

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapWatchService;
import itdelatrisu.opsu.downloads.DownloadList;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.render.CurveRenderState;
import itdelatrisu.opsu.ui.UI;

import org.lwjgl.opengl.Display;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Game;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.InternalTextureLoader;

/**
 * AppGameContainer extension that sends critical errors to ErrorHandler.
 */
public class Container extends AppGameContainer {
	/** SlickException causing game failure. */
	protected SlickException e = null;

	private Exception anyException = null;

	public static Container instance;

	/**
	 * Create a new container wrapping a game
	 *
	 * @param game The game to be wrapped
	 * @throws SlickException Indicates a failure to initialise the display
	 */
	public Container(Game game) throws SlickException {
		super(game);
		instance = this;
		width = this.getWidth();
		height = this.getHeight();
	}

	@Override
	public void start() throws SlickException {
		try {
			setup();
			ErrorHandler.setGlString();
			getDelta();
			while (running())
				gameLoop();
		} catch(Exception e) {
			anyException = e;
		} finally {
			// destroy the game container
			close_sub();
			destroy();

			if (anyException != null) {
				ErrorHandler.error("Something bad happend while playing", anyException, true);
				anyException = null;
			} else if (e != null) {
				ErrorHandler.error(null, e, true);
				e = null;
			}
		}

		if (forceExit) {
			Opsu.close();
			System.exit(0);
		}
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
				this.e = e;  // store exception to display later
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

	/**
	 * Actions to perform before destroying the game container.
	 */
	private void close_sub() {
		// save user options
		Options.saveOptions();

		// reset cursor
		if (UI.getCursor() != null) {
			UI.getCursor().reset();
		}

		// destroy images
		InternalTextureLoader.get().clear();

		// reset image references
		GameImage.clearReferences();
		GameData.Grade.clearReferences();
		Beatmap.clearBackgroundImageCache();

		// prevent loading tracks from re-initializing OpenAL
		MusicController.reset();

		// stop any playing track
		SoundController.stopTrack();

		// reset BeatmapSetList data
		if (BeatmapSetList.get() != null)
			BeatmapSetList.get().reset();

		// delete OpenGL objects involved in the Curve rendering
		CurveRenderState.shutdown();

		// destroy watch service
		if (!Options.isWatchServiceEnabled())
			BeatmapWatchService.destroy();
		BeatmapWatchService.removeListeners();

		// delete temporary directory
		Utils.deleteDirectory(Options.TEMP_DIR);
	}

	@Override
	public void exit() {
		// show confirmation dialog if any downloads are active
		if (forceExit) {
			if (DownloadList.get().hasActiveDownloads() &&
			    UI.showExitConfirmation(DownloadList.EXIT_CONFIRMATION))
				return;
			if (Updater.get().getStatus() == Updater.Status.UPDATE_DOWNLOADING &&
			    UI.showExitConfirmation(Updater.EXIT_CONFIRMATION))
				return;
		}

		super.exit();
	}
}
