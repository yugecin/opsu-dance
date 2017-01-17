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
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.BeatmapSetList;
import itdelatrisu.opsu.beatmap.BeatmapWatchService;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.replay.ReplayImporter;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import java.io.File;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.inject.InstanceContainer;
import yugecin.opsudance.core.state.BaseOpsuState;

/**
 * "Splash Screen" state.
 * <p>
 * Loads game resources and enters "Main Menu" state.
 */
public class Splash extends BaseOpsuState {

	private final InstanceContainer instanceContainer;

	/** Minimum time, in milliseconds, to display the splash screen (and fade in the logo). */
	private static final int MIN_SPLASH_TIME = 400;

	/** Whether or not loading has completed. */
	private boolean finished = false;

	/** Loading thread. */
	private Thread thread;

	/** Number of times the 'Esc' key has been pressed. */
	private int escapeCount = 0;

	/** Whether the skin being loaded is a new skin (for program restarts). */
	private boolean newSkin = false;

	/** Whether the watch service is newly enabled (for program restarts). */
	private boolean watchServiceChange = false;

	/** Logo alpha level. */
	private AnimatedValue logoAlpha;

	// game-related variables
	private boolean init = false;

	public Splash(DisplayContainer displayContainer, InstanceContainer instanceContainer) {
		super(displayContainer);
		this.instanceContainer = instanceContainer;
	}

	@Override
	protected void revalidate() {
		super.revalidate();

		// check if skin changed
		if (Options.getSkin() != null)
			this.newSkin = (Options.getSkin().getDirectory() != Options.getSkinDir());

		// check if watch service newly enabled
		this.watchServiceChange = Options.isWatchServiceEnabled() && BeatmapWatchService.get() == null;

		// load Utils class first (needed in other 'init' methods)
		Utils.init(displayContainer);

		// fade in logo
		this.logoAlpha = new AnimatedValue(MIN_SPLASH_TIME, 0f, 1f, AnimationEquation.LINEAR);
		GameImage.MENU_LOGO.getImage().setAlpha(0f);
	}

	@Override
	public void render(Graphics g) {
		g.setBackground(Color.black);
		GameImage.MENU_LOGO.getImage().drawCentered(displayContainer.width / 2, displayContainer.height / 2);
		UI.drawLoadingProgress(g);
	}

	@Override
	public void preRenderUpdate() {
		if (!init) {
			init = true;

			// resources already loaded (from application restart)
			if (BeatmapSetList.get() != null) {
				if (newSkin || watchServiceChange) {  // need to reload resources
					thread = new Thread() {
						@Override
						public void run() {
							// reload beatmaps if watch service newly enabled
							if (watchServiceChange)
								BeatmapParser.parseAllFiles(Options.getBeatmapDir());

							// reload sounds if skin changed
							// TODO: only reload each sound if actually needed?
							if (newSkin)
								SoundController.init();

							finished = true;
							thread = null;
						}
					};
					thread.start();
				} else  // don't reload anything
					finished = true;
			}

			// load all resources in a new thread
			else {
				thread = new Thread() {
					@Override
					public void run() {
						File beatmapDir = Options.getBeatmapDir();

						// unpack all OSZ archives
						OszUnpacker.unpackAllFiles(Options.getOSZDir(), beatmapDir);

						// parse song directory
						BeatmapParser.parseAllFiles(beatmapDir);

						// import replays
						ReplayImporter.importAllReplaysFromDir(Options.getReplayImportDir());

						// load sounds
						SoundController.init();

						finished = true;
						thread = null;
					}
				};
				thread.start();
			}
		}

		// fade in logo
		if (logoAlpha.update(displayContainer.renderDelta))
			GameImage.MENU_LOGO.getImage().setAlpha(logoAlpha.getValue());

		// change states when loading complete
		if (finished && logoAlpha.getValue() >= 1f) {
			// initialize song list
			if (BeatmapSetList.get().size() > 0) {
				BeatmapSetList.get().init();
				if (Options.isThemeSongEnabled()) {
					MusicController.playThemeSong();
				} else {
					instanceContainer.provide(SongMenu.class).setFocus(BeatmapSetList.get().getRandomNode(), -1, true, true);
					System.out.println(("todo"));
				}
			} else {
				MusicController.playThemeSong();
			}
			displayContainer.switchState(MainMenu.class);
		}
	}

	@Override
	public boolean keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			// close program
			if (++escapeCount >= 3) System.out.println("hi");
				//container.exit(); // TODO

			// stop parsing beatmaps by sending interrupt to BeatmapParser
			else if (thread != null)
				thread.interrupt();
			return true;
		}
		return false;
	}
}
