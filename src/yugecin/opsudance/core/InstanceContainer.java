// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import itdelatrisu.opsu.NativeLoader;
import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.replay.ReplayImporter;
import itdelatrisu.opsu.states.*;

import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.ResourceLoader;

import yugecin.opsudance.core.input.Input;
import yugecin.opsudance.core.state.specialstates.BarNotificationState;
import yugecin.opsudance.core.state.specialstates.BubNotifState;
import yugecin.opsudance.core.state.specialstates.FpsRenderState;
import yugecin.opsudance.options.Configuration;
import yugecin.opsudance.options.OptionGroups;
import yugecin.opsudance.options.OptionsService;
import yugecin.opsudance.render.GameObjectRenderer;
import yugecin.opsudance.skinning.SkinService;
import yugecin.opsudance.ui.OptionsOverlay;
import yugecin.opsudance.ui.VolumeControl;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static yugecin.opsudance.utils.SyntacticSugar.closeAndSwallow;

public class InstanceContainer {

	public static Environment env;
	public static Configuration config;

	public static OptionsService optionservice;
	public static SkinService skinservice;
	public static OszUnpacker oszunpacker;
	public static ReplayImporter replayImporter;
	public static BeatmapParser beatmapParser;
	public static Updater updater;

	public static VolumeControl volumeControl;
	public static DisplayContainer displayContainer;
	public static Input input;

	public static GameObjectRenderer gameObjectRenderer;
	
	public static BarNotificationState barNotifs;
	public static BubNotifState bubNotifs;
	public static FpsRenderState fpsDisplay;
	
	public static OptionsOverlay optionsOverlay;

	public static Splash splashState;
	public static MainMenu mainmenuState;
	public static ButtonMenu buttonState;
	public static SongMenu songMenuState;
	public static DownloadsMenu downloadState;
	public static Game gameState;
	public static GameRanking gameRankingState;
	public static GamePauseMenu pauseState;
	
	public static int width, width2, height, height2;
	public static boolean isWidescreen;
	public static int mouseX, mouseY;
	public static int renderDelta;

	public static void kickstart() {
		updater = new Updater();
		env = new Environment();

		JarFile jarfile = getJarfile();
		config = new Configuration();
		if (jarfile != null) {
			try {
				NativeLoader.loadNatives(jarfile);
			} catch (IOException e) {
				String msg = String.format("Could not unpack native(s): %s", e.getMessage());
				throw new RuntimeException(msg, e);
			} finally {
				closeAndSwallow(jarfile);
			}
		}
		NativeLoader.setNativePath();

		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));

		input = new Input();

		optionservice = new OptionsService();
		skinservice = new SkinService();
		oszunpacker = new OszUnpacker();
		replayImporter = new ReplayImporter();
		beatmapParser = new BeatmapParser();
		updater = new Updater();

		displayContainer = new DisplayContainer();
		
		barNotifs = new BarNotificationState();
		bubNotifs = new BubNotifState();
		fpsDisplay = new FpsRenderState();

		gameObjectRenderer = new GameObjectRenderer();

		optionsOverlay = new OptionsOverlay(OptionGroups.normalOptions);

		splashState = new Splash();
		mainmenuState = new MainMenu();
		buttonState = new ButtonMenu();
		songMenuState = new SongMenu();
		downloadState = new DownloadsMenu();
		gameState = new Game();
		gameRankingState = new GameRanking();
		pauseState = new GamePauseMenu();

	}

	@Nullable
	private static JarFile getJarfile() {
		if (env.jarfile == null) {
			return null;
		}
		try {
			return new JarFile(env.jarfile);
		} catch (IOException e) {
			String msg = String.format("Cannot read from jarfile (%s): %s", env.jarfile.getAbsolutePath(),
				e.getMessage());
			throw new RuntimeException(msg, e);
		}
	}

	@Nullable
	private static Manifest getJarManifest(@Nullable JarFile jarfile) {
		if (jarfile == null) {
			return null;
		}
		try {
			return jarfile.getManifest();
		} catch (IOException e) {
			String msg = String.format("Cannot read manifest from jarfile: %s", e.getMessage());
			throw new RuntimeException(msg, e);
		}
	}

}
