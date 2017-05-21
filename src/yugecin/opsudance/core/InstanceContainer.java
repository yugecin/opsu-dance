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

import itdelatrisu.opsu.NativeLoader;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.replay.ReplayImporter;
import itdelatrisu.opsu.states.*;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.ResourceLoader;
import yugecin.opsudance.options.Configuration;
import yugecin.opsudance.options.OptionsService;
import yugecin.opsudance.render.GameObjectRenderer;
import yugecin.opsudance.skinning.SkinService;

import java.io.File;

public class InstanceContainer {

	public static Environment env;
	public static Configuration config;

	public static OptionsService optionservice;
	public static SkinService skinservice;
	public static OszUnpacker oszunpacker;
	public static ReplayImporter replayImporter;
	public static BeatmapParser beatmapParser;
	public static Updater updater;

	public static DisplayContainer displayContainer;

	public static GameObjectRenderer gameObjectRenderer;

	public static Splash splashState;
	public static MainMenu mainmenuState;
	public static ButtonMenu buttonState;
	public static SongMenu songMenuState;
	public static DownloadsMenu downloadState;
	public static Game gameState;
	public static GameRanking gameRankingState;
	public static GamePauseMenu pauseState;

	public static void kickstart() {
		updater = new Updater();
		env = new Environment();
		config = new Configuration();

		NativeLoader.loadNatives();
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));

		optionservice = new OptionsService();
		skinservice = new SkinService();
		oszunpacker = new OszUnpacker();
		replayImporter = new ReplayImporter();
		beatmapParser = new BeatmapParser();
		updater = new Updater();

		displayContainer = new DisplayContainer();

		gameObjectRenderer = new GameObjectRenderer();

		splashState = new Splash();
		mainmenuState = new MainMenu();
		buttonState = new ButtonMenu();
		songMenuState = new SongMenu();
		downloadState = new DownloadsMenu();
		gameState = new Game();
		gameRankingState = new GameRanking();
		pauseState = new GamePauseMenu();
	}

}
