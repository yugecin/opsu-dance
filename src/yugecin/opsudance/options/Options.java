// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

import awlex.ospu.polymover.factory.PolyMoverFactory;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.states.game.Game;
import itdelatrisu.opsu.ui.Fonts;
import org.lwjgl.input.Keyboard;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.*;
import yugecin.opsudance.movers.factories.ExgonMoverFactory;
import yugecin.opsudance.movers.factories.QuadraticBezierMoverFactory;
import yugecin.opsudance.movers.slidermovers.DefaultSliderMoverController;
import yugecin.opsudance.ui.OptionsOverlay;
import yugecin.opsudance.ui.cursor.colors.CursorColorManager;
import yugecin.opsudance.utils.CachedVariable;
import yugecin.opsudance.utils.CachedVariable.Getter;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * @author itdelatrisu (https://github.com/itdelatrisu) most functions are copied from
 *                     itdelatrisu/opsu/Options.java
 */
public class Options
{
	// internal options (not displayed in-game)
	static {
		new Option("BeatmapDirectory") {
			@Override
			public String write() {
				return config.BEATMAP_DIR.getAbsolutePath();
			}

			@Override
			public void read(String s) {
				config.beatmapDir = new File(s);
			}
		};

		new Option("OSZDirectory") {
			@Override
			public String write() {
				return config.oszDir.getAbsolutePath();
			}

			@Override
			public void read(String s) {
				config.oszDir = new File(s);
			}
		};

		new Option("ScreenshotDirectory") {
			@Override
			public String write() {
				return config.screenshotDir.getAbsolutePath();
			}

			@Override
			public void read(String s) {
				config.screenshotDir = new File(s);
			}
		};

		new Option("ReplayDirectory") {
			@Override
			public String write() {
				return config.replayDir.getAbsolutePath();
			}

			@Override
			public void read(String s) {
				config.replayDir = new File(s);
			}
		};

		new Option("ReplayImportDirectory") {
			@Override
			public String write() {
				return config.replayImportDir.getAbsolutePath();
			}

			@Override
			public void read(String s) {
				config.replayImportDir = new File(s);
			}
		};

		new Option("SkinDirectory") {
			@Override
			public String write() {
				return config.skinRootDir.getAbsolutePath();
			}

			@Override
			public void read(String s) {
				config.skinRootDir = new File(s);
			}
		};
	}

	public static final NumericOption OPTION_PORT = new NumericOption("-", "Port", "-", 49250, 1024, 65535) {
		@Override
		public void read (String s){
			super.read(s);
		}
	};

	public static final ToggleOption OPTION_NOSINGLEINSTANCE = new ToggleOption("-", "NoSingleInstance", "-", false);

	public static final ToggleOption OPTION_STARFOUNTAINS = new ToggleOption("Star fountains in main menu", "StarFountains", "Show star bursts in main menu", true);

	// in-game options
	public static final ListOption OPTION_SCREEN_RESOLUTION = new ListOption("Screen Resolution", "ScreenResolution", "Change the size of the game.") {
		private final String[] resolutions = {
			null,
			"800x600",
			"1024x600",
			"1024x768",
			"1280x720",
			"1280x800",
			"1280x960",
			"1280x1024",
			"1366x768",
			"1440x900",
			"1600x900",
			"1600x1200",
			"1680x1050",
			"1920x1080",
			"1920x1200",
			"2560x1440",
			"2560x1600",
			"3840x2160"
		};

		private int idx;

		@Override
		public String getValueString () {
			return resolutions[idx]; // do not change (see DisplayContainer#setup)
		}

		@Override
		public Object[] getListItems () {
			return resolutions;
		}

		@Override
		public void clickListItem(int index){
			idx = index;
			displayContainer.updateDisplayMode(resolutions[idx]);
			this.notifyListeners();
		}

		@Override
		public void read (String s){
			try {
				idx = Integer.parseInt(s);
			} catch (Exception ignored) {
			}
			idx = Utils.clamp(idx, 0, resolutions.length);
		}

		@Override
		public String write () {
			return String.valueOf(idx);
		}
	};

	public static final ToggleOption OPTION_ALLOW_LARGER_RESOLUTIONS = new ToggleOption("Allow large resolutions", "AllowLargeRes", "Allow resolutions larger than the native resolution", false);
	public static final ToggleOption OPTION_FULLSCREEN = new ToggleOption("Fullscreen Mode", "Fullscreen", "Fullscreen mode", false) {
		@Override
		public void toggle()
		{
			super.toggle();
			displayContainer.updateDisplayMode(width, height);
		}
	};
	public static final ListOption OPTION_SKIN = new ListOption("Skin", "Skin", "Change how the game looks.") {

		@Override
		public String getValueString () {
			return skinservice.usedSkinName;
		}

		@Override
		public Object[] getListItems () {
			return skinservice.availableSkinDirectories;
		}

		@Override
		public void clickListItem(int index){
			skinservice.usedSkinName = skinservice.availableSkinDirectories[index];
			skinservice.reloadSkin();
			this.notifyListeners();
		}

		@Override
		public void read (String s){
			skinservice.usedSkinName = s;
		}

		@Override
		public String write() {
			return skinservice.usedSkinName;
		}
	};

	public static final int[] targetUPS = { 60, 120, 240, 480, 960, 1000, -1 };

	public static final NumericOption OPTION_TARGET_UPS = new NumericOption("target UPS", "targetUPS", "Higher values result in less input lag and smoother cursor trail, but may cause high CPU usage.", 2, 0, targetUPS.length - 1) {
		@Override
		public String getValueString () {
			if (targetUPS[val] == -1) {
				return "unlimited";
			}
			return String.valueOf(targetUPS[val]);
		}

		@Override
		public void setValue(int value) {
			if (value < 0 || targetUPS.length <= value) {
				return;
			}
			final int ups = targetUPS[value];
			final int fps = targetFPS[targetFPSIndex];
			super.setValue(value);
			displayContainer.setUPS(ups);
			if (ups != -1 && fps > ups) {
				for (int i = targetFPSIndex - 1; i >= 0; i--) {
					if (targetFPS[i] >= ups) {
						OPTION_TARGET_FPS.clickListItem(i);
						break;
					}
				}
			}
		}
	};

	public static final int[] targetFPS = {60, 120, 240, 1000};
	public static int targetFPSIndex = 0;

	public static final ListOption OPTION_TARGET_FPS = new ListOption("FPS limit", "FPSlimit", "Higher values may cause high CPU usage. A value higher than the UPS has no effect.") {
		private CachedVariable<String[]> $_getListItems = new CachedVariable<>(new Getter<String[]>() {
			@Override
			public String[] get() {
				String[] list = new String[targetFPS.length];
				for (int i = 0; i < targetFPS.length; i++) {
					list[i] = String.format("%dfps", targetFPS[i]);
				}
				return list;
			}
		});

		@Override
		public String getValueString () {
			return $_getListItems.get()[targetFPSIndex];
		}

		@Override
		public Object[] getListItems () {
			return $_getListItems.get();
		}

		@Override
		public void clickListItem(int index){
			targetFPSIndex = index;
			int fps = targetFPS[targetFPSIndex];
			displayContainer.setFPS(fps);
			if (targetUPS[OPTION_TARGET_UPS.val] < fps) {
				for (int i = 0; i < targetUPS.length; i++) {
					if (targetUPS[i] >= fps) {
						OPTION_TARGET_UPS.setValue(i);
						break;
					}
				}
			}
			this.notifyListeners();
		}

		@Override
		public String write () {
			return Integer.toString(targetFPS[targetFPSIndex]);
		}

		@Override
		public void read (String s){
			int i = Integer.parseInt(s);
			for (int j = 0; j < targetFPS.length; j++) {
				if (i == targetFPS[j]) {
					targetFPSIndex = j;
					break;
				}
			}
		}
	};

	public static final ToggleOption OPTION_SHOW_FPS = new ToggleOption("Show FPS Counters", "FpsCounter", "Show FPS and UPS counters in the bottom-right hand corner.", true);
	public static final ToggleOption OPTION_USE_FPS_DELTAS = new ToggleOption("Use deltas for FPS counters", "FpsCounterDeltas", "Show time between updates instead of updates per second.", false) {
		@Override
		public boolean showCondition () {
			return OPTION_SHOW_FPS.state;
		}
	};

	public static final ToggleOption OPTION_SHOW_UNICODE = new ToggleOption("Prefer Non-English Metadata", "ShowUnicode", "Where available, song titles will be shown in their native language.", false) {
		@Override
		public void toggle () {
			super.toggle();
			if (!state) {
				return;
			}
			try {
				Fonts.LARGE.loadGlyphs();
				Fonts.MEDIUM.loadGlyphs();
				Fonts.DEFAULT.loadGlyphs();
			} catch (SlickException e) {
				Log.warn("Failed to load glyphs.", e);
			}
		}
	};

	public static final ListOption OPTION_SCREENSHOT_FORMAT = new ListOption("Screenshot Format", "ScreenshotFormat", "Press F12 to take a screenshot.") {
		private String[] formats = { "PNG", "JPG", "BMP" };
		private int index = 0;

		@Override
		public String getValueString () {
			return formats[index];
		}

		@Override
		public Object[] getListItems () {
			return formats;
		}

		@Override
		public void clickListItem(int index){
			this.index = index;
			this.notifyListeners();
		}

		@Override
		public String write () {
			return Integer.toString(index);
		}

		@Override
		public void read (String s){
			int i = Integer.parseInt(s);
			if (0 <= i && i < formats.length) {
				index = i;
			}
		}
	};

	public static final NumericOption OPTION_CURSOR_SIZE = new NumericOption("Size", "CursorSize", "Change the cursor scale.", 100, 50, 200) {
		@Override
		public String getValueString () {
			return String.format("%.2fx", val / 100f);
		}

		@Override
		public String write () {
			return String.format("%.2f", val / 100f);
		}

		@Override
		public void read (String s){
			int i = (int) (Float.parseFloat(s.replace(',', '.')) * 100f);
			if (i >= 50 && i <= 200)
				val = i;
		}
	};

	public static final ToggleOption OPTION_NEW_CURSOR = new ToggleOption("Enable New Cursor", "NewCursor", "Use the new cursor style (may cause higher CPU usage).", true)
	{
		@Override
		public boolean showCondition()
		{
			return !OPTION_NEWEST_CURSOR.state;
		}
	};
	public static final ToggleOption OPTION_NEWEST_CURSOR = new ToggleOption("Enable Newest Cursor", "NewestCursor", "Completely smooth cursortrail,  maybe more intensive", true)
	{
		@Override
		public void toggle()
		{
			super.toggle();
			displayContainer.reinitCursor();
		}
	};
	public static final ToggleOption OPTION_DYNAMIC_BACKGROUND = new ToggleOption("Enable Dynamic Backgrounds", "DynamicBackground", "The song background will be used as the main menu background.", true);
	public static final ToggleOption OPTION_LOAD_VERBOSE = new ToggleOption("Show Detailed Loading Progress", "LoadVerbose", "Display more specific loading information in the splash screen.", false);
	public static final ToggleOption OPTION_COLOR_MAIN_MENU_LOGO = new ToggleOption("Use cursor color as main menu logo tint", "ColorMainMenuLogo", "Colorful main menu logo", false);
	public static final ToggleOption OPTION_FORCE_FALLBACK_VOLUMECONTROL = new ToggleOption("Alternative volume indicator", "FallbackVolumeControl", "Use a simpeler volume control", false);
	public static final NumericOption OPTION_MASTER_VOLUME = new NumericOption("Master", "VolumeUniversal", "Global volume level.", 35, 0, 100) {
		@Override
		public void setValue(int value){
			super.setValue(value);
			// changing mastervolume, so music volume should change too
			OPTION_MUSIC_VOLUME.setValue(OPTION_MUSIC_VOLUME.val);
		}
	};

	public static final NumericOption OPTION_MUSIC_VOLUME = new NumericOption("Music", "VolumeMusic", "Volume of music.", 80, 0, 100) {
		@Override
		public void setValue(int value){
			super.setValue(value);
			SoundStore.get().setMusicVolume(OPTION_MASTER_VOLUME.val * OPTION_MUSIC_VOLUME.val / 10000f);
		}
	};

	public static final NumericOption OPTION_SAMPLE_VOLUME_OVERRIDE = new NumericOption("Sample override", "BMSampleOverride", "Override beatmap hitsound volume", 100, 0, 100) {
		@Override
		public String getValueString () {
			if (val == 0) {
				return "Disabled";
			}
			return super.getValueString();
		}
	};

	public static final NumericOption OPTION_EFFECT_VOLUME = new NumericOption("Effects", "VolumeEffect", "Volume of menu and game sounds.", 70, 0, 100);
	public static final NumericOption OPTION_HITSOUND_VOLUME = new NumericOption("Hit Sounds", "VolumeHitSound", "Volume of hit sounds.", 30, 0, 100);
	public static final NumericOption OPTION_MUSIC_OFFSET = new NumericOption("Global Music Offset", "Offset", "Adjust this value if hit objects are out of sync.", -75, -500, 500) {
		@Override
		public String getValueString () {
			return String.format("%dms", val);
		}
	};

	public static final ToggleOption OPTION_DISABLE_SOUNDS = new ToggleOption("Disable All Sound Effects", "DisableSound", "May resolve Linux sound driver issues.  Requires a restart.", (System.getProperty("os.name").toLowerCase().contains("linux")));

	public static final KeyOption
		OPTION_KEY_LEFT = new KeyOption("Left Game Key", "keyOsuLeft", "Select this option to input a key.", Keyboard.KEY_Z),
		OPTION_KEY_RIGHT = new KeyOption("Right Game Key", "keyOsuRight", "Select this option to input a key.", Keyboard.KEY_X);

	public static final NumericOption OPTION_BACKGROUND_DIM = new NumericOption("Background Dim", "DimLevel", "Percentage to dim the background image during gameplay.", 50, 0, 100);
	public static final ToggleOption OPTION_DISABLE_MOUSE_WHEEL = new ToggleOption("Disable mouse wheel in play mode", "MouseDisableWheel", "During play, you can use the mouse wheel to adjust the volume and pause the game. This will disable that functionality.", false);
	public static final ToggleOption OPTION_DISABLE_MOUSE_BUTTONS = new ToggleOption("Disable mouse buttons in play mode", "MouseDisableButtons", "This option will disable all mouse buttons. Specifically for people who use their keyboard to click.", false) {
		@Override
		public void toggle() {
			barNotifs.send(state ?
				"Mouse buttons are disabled." : "Mouse buttons are enabled.");
		}
	};
	public static final ToggleOption OPTION_DISABLE_CURSOR = new ToggleOption("Disable Cursor", "DisableCursor", "Hide the cursor sprite.", false);
	public static final ToggleOption OPTION_DANCE_REMOVE_BG = new ToggleOption("Use black background instead of image", "RemoveBG", "Hello darkness my old friend", true);
	public static final ToggleOption OPTION_FORCE_DEFAULT_PLAYFIELD =
		new ToggleOption(
			"Force Default Background Image",
			"ForceDefaultPlayfield",
			"Override the song background with the default playfield background.",
			false
		);
	public static final ToggleOption OPTION_IGNORE_BEATMAP_SKINS = new ToggleOption("Ignore All Beatmap Skins", "IgnoreBeatmapSkins", "Never use skin element overrides provided by beatmaps.", false);
	public static final ToggleOption OPTION_SNAKING_SLIDERS = new ToggleOption("Snaking sliders", "SnakingSliders", "Sliders gradually snake out from their starting point.", true);
	public static final ToggleOption OPTION_SHRINKING_SLIDERS = new ToggleOption("Shrinking sliders", "ShrinkingSliders", "Sliders shrinks when sliderball passes (aka knorkesliders)", true);
	public static final ToggleOption OPTION_FALLBACK_SLIDERS = new ToggleOption("Fallback sliders", "FallbackSliders", "Enable this if sliders won't render", false);
	public static final ToggleOption OPTION_MERGING_SLIDERS = new ToggleOption("Merging sliders", "MergingSliders", "Merge sliders (aka knorkesliders)", true) {
		@Override
		public boolean showCondition () {
			return !OPTION_FALLBACK_SLIDERS.state;
		}
	};

	public static final NumericOption OPTION_MERGING_SLIDERS_MIRROR_POOL = new NumericOption("Mirrors", "MergingSliderMirrorPool", "Amount of mirrors to calculate for merging sliders (impacts performance)", 2, 2, 6) {
		@Override
		public String getValueString () {
			return String.valueOf(val);
		}

		@Override
		public boolean showCondition () {
			return OPTION_MERGING_SLIDERS.showCondition() && OPTION_MERGING_SLIDERS.state;
		}
	};

	public static final ToggleOption OPTION_DRAW_SLIDER_ENDCIRCLES = new ToggleOption("Draw endcircles", "DrawSliderEndCircles", "Old slider style", false);
	public static final ToggleOption OPTION_DANCING_CIRCLES = new ToggleOption("Enable", "DancingHitcircles", "Make hitcircles dance to the beat", false);
	public static final NumericOption OPTION_DANCING_CIRCLES_MULTIPLIER = new NumericOption("Multiplier", "DancingHitcirclesMP", "Multiplier to expand the hitcircles when dancing to the beat", 50, 1, 200) {
		@Override
		public String getValueString () {
			return String.format("%.1f%%", val / 10f);
		}
	};

	public static final ToggleOption OPTION_SHOW_HIT_LIGHTING = new ToggleOption("Show Hit Lighting", "HitLighting", "Adds an effect behind hit explosions.", true);
	public static final ToggleOption OPTION_SHOW_HIT_ANIMATIONS = new ToggleOption("Show Hit Animations", "HitAnimations", "Fade out circles and curves.", true);
	public static final ToggleOption OPTION_SHOW_COMBO_BURSTS = new ToggleOption("Show Combo Bursts", "ComboBurst", "A character image is displayed at combo milestones.", true);
	public static final ToggleOption OPTION_SHOW_PERFECT_HIT = new ToggleOption("Show Perfect Hits", "PerfectHit", "Whether to show perfect hit result bursts (300s, slider ticks).", true);
	public static final ToggleOption OPTION_SHOW_FOLLOW_POINTS = new ToggleOption("Show Follow Points", "FollowPoints", "Whether to show follow points between hit objects.", true);
	public static final ToggleOption OPTION_SHOW_HIT_ERROR_BAR = new ToggleOption("Show Hit Error Bar", "ScoreMeter", "Shows precisely how accurate you were with each hit.", false);
	public static final NumericOption OPTION_MAP_START_DELAY = new NumericOption("Map start delay", "StartDelay", "Have a fix amount of time to prepare your play/record", 20, 1, 50) {
		@Override
		public String getValueString () {
			return (val * 100) + "ms";
		}
	};

	public static final NumericOption OPTION_MAP_END_DELAY = new NumericOption("Map end delay", "EndDelay", "Have a fix amount of time at the and of the map for a smooth finish", 50, 1, 150) {
		@Override
		public String getValueString () {
			return (val * 100) + "ms";
		}
	};

	public static final NumericOption OPTION_EPILEPSY_WARNING = new NumericOption("Epilepsy warning image", "EpiWarn", "Show a little warning for flashing colours in the beginning", 0, 0, 20) {
		@Override
		public String getValueString () {
			if (val == 0) {
				return "Disabled";
			}
			return (val * 100) + "ms";
		}
	};

	public static final ToggleOption OPTION_LOAD_HD_IMAGES = new ToggleOption("Load HD Images", "LoadHDImages", String.format("Loads HD (%s) images when available. Increases memory usage and loading times.", GameImage.HD_SUFFIX), true);
	public static final NumericOption OPTION_FIXED_CS = new NumericOption("Fixed CS", "FixedCS", "Determines the size of circles and sliders.", 0, 0, 100) {
		@Override
		public String getValueString () {
			return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f);
		}

		@Override
		public String write () {
			return String.format("%.1f", val / 10f);
		}

		@Override
		public void read (String s){
			int i = (int) (Float.parseFloat(s.replace(',', '.')) * 10f);
			if (i >= 0 && i <= 100)
				val = i;
		}
	};

	public static final NumericOption OPTION_FIXED_HP = new NumericOption("Fixed HP", "FixedHP", "Determines the rate at which health decreases.", 0, 0, 100) {
		@Override
		public String getValueString () {
			return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f);
		}

		@Override
		public String write () {
			return String.format("%.1f", val / 10f);
		}

		@Override
		public void read (String s){
			int i = (int) (Float.parseFloat(s.replace(',', '.')) * 10f);
			if (i >= 0 && i <= 100)
				val = i;
		}
	};

	public static final NumericOption OPTION_FIXED_AR = new NumericOption("Fixed AR", "FixedAR", "Determines how long hit circles stay on the screen.", 0, 0, 100) {
		@Override
		public String getValueString () {
			return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f);
		}

		@Override
		public String write () {
			return String.format("%.1f", val / 10f);
		}

		@Override
		public void read (String s){
			int i = (int) (Float.parseFloat(s.replace(',', '.')) * 10f);
			if (i >= 0 && i <= 100)
				val = i;
		}
	};

	public static final NumericOption OPTION_FIXED_OD = new NumericOption("Fixed OD", "FixedOD", "Determines the time window for hit results.", 0, 0, 100) {
		@Override
		public String getValueString () {
			return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f);
		}

		@Override
		public String write () {
			return String.format("%.1f", val / 10f);
		}

		@Override
		public void read (String s){
			int i = (int) (Float.parseFloat(s.replace(',', '.')) * 10f);
			if (i >= 0 && i <= 100)
				val = i;
		}
	};

	public static final NumericOption OPTION_CHECKPOINT = new NumericOption("Track Checkpoint", "Checkpoint", "Press Ctrl+L while playing to load a checkpoint, and Ctrl+S to set one.", 0, 0, 3599) {
		@Override
		public String getValueString () {
			return (val == 0) ? "Disabled" : String.format("%02d:%02d",
				TimeUnit.SECONDS.toMinutes(val),
				val - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(val)));
		}
	};

	public static final ToggleOption OPTION_ENABLE_THEME_SONG = new ToggleOption("Enable Theme Song", "MenuMusic", "Whether to play the theme song upon starting opsu!", true);
	public static final ToggleOption OPTION_REPLAY_SEEKING = new ToggleOption("Replay Seeking", "ReplaySeeking", "Enable a seeking bar on the left side of the screen during replays.", false);
	public static final ToggleOption OPTION_DISABLE_UPDATER = new ToggleOption("Disable Automatic Updates", "DisableUpdater", "Disable automatic checking for updates upon starting opsu!.", false);
	public static final ToggleOption OPTION_ENABLE_WATCH_SERVICE = new ToggleOption("Enable Watch Service", "WatchService", "Watch the beatmap directory for changes. Requires a restart.", false);
	public static final ListOption OPTION_DANCE_MOVER = new ListOption("Algorithm", "Mover", "Algorithm that decides how to move from note to note" ) {
		@Override
		public Object[] getListItems () {
			return Dancer.moverFactories;
		}

		@Override
		public void clickListItem(int index){
			if (Game.isInGame && Dancer.moverFactories[index] instanceof PolyMoverFactory) {
				// TODO remove this when #79 is fixed
				barNotifs.send("This mover is disabled in the storyboard right now");
				return;
			}
			Dancer.instance.setMoverFactoryIndex(index);
			this.notifyListeners();
		}

		@Override
		public String getValueString () {
			return Dancer.moverFactories[Dancer.instance.getMoverFactoryIndex()].toString();
		}

		@Override
		public String write () {
			return String.valueOf(Dancer.instance.getMoverFactoryIndex());
		}

		@Override
		public void read (String s){
			int i = Integer.parseInt(s);
			Dancer.instance.setMoverFactoryIndex(i);
		}
	};

	public static final NumericOption OPTION_DANCE_EXGON_DELAY = new NumericOption("ExGon delay", "ExGonDelay", "Delay between moves for the ExGon mover", 25, 2, 750) {
		@Override
		public String getValueString () {
			return String.valueOf(val);
		}
		@Override
		public boolean showCondition () {
			return Dancer.moverFactories[Dancer.instance.getMoverFactoryIndex()] instanceof ExgonMoverFactory;
		}
	};

	public static final NumericOption OPTION_DANCE_QUAD_BEZ_AGGRESSIVENESS = new NumericOption("Bezier aggressiveness", "QuadBezAgr", "AKA initial D factor", 50, 0, 200) {
		@Override
		public String getValueString () {
			return String.valueOf(val);
		}

		@Override
		public boolean showCondition () {
			return Dancer.moverFactories[Dancer.instance.getMoverFactoryIndex()] instanceof QuadraticBezierMoverFactory;
		}
	};

	public static final NumericOption OPTION_DANCE_QUAD_BEZ_SLIDER_AGGRESSIVENESS_FACTOR = new NumericOption("Exit aggressiveness", "CubBezSliderExitAgr", "AKA initial D factor for sliderexits", 4, 1, 6) {
		@Override
		public String getValueString () {
			return String.valueOf(val);
		}

		@Override
		public boolean showCondition () {
			return OPTION_DANCE_QUAD_BEZ_AGGRESSIVENESS.showCondition()
				&& Dancer.sliderMoverController instanceof DefaultSliderMoverController;
		}
	};

	public static final ToggleOption OPTION_DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS = new ToggleOption("Use cubic bezier before sliders", "QuadBezCubicSliders", "Slider entry looks better using this", true) {
		@Override
		public boolean showCondition () {
			return OPTION_DANCE_QUAD_BEZ_SLIDER_AGGRESSIVENESS_FACTOR.showCondition();
		}
	};

	public static final NumericOption OPTION_DANCE_QUAD_BEZ_CUBIC_AGGRESSIVENESS_FACTOR = new NumericOption("Entry aggressiveness", "CubBezSliderEntryAgr", "AKA initial D factor for sliderentries", 4, 1, 6) {
		@Override
		public String getValueString () {
			return String.valueOf(val);
		}

		@Override
		public boolean showCondition () {
			return OPTION_DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS.showCondition()
				&& OPTION_DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS.state;
		}
	};

	public static final ListOption OPTION_DANCE_MOVER_DIRECTION = new ListOption("Direction", "MoverDirection", "The direction the mover goes" ) {
		@Override
		public String getValueString () {
			return Dancer.moverDirection.toString();
		}

		@Override
		public Object[] getListItems () {
			return MoverDirection.values();
		}

		@Override
		public void clickListItem(int index){
			Dancer.moverDirection = MoverDirection.values()[index];
			this.notifyListeners();
		}

		@Override
		public String write () {
			return "" + Dancer.moverDirection.nr;
		}

		@Override
		public void read (String s){
			Dancer.moverDirection = MoverDirection.values()[Integer.parseInt(s)];
		}
	};

	public static final ListOption OPTION_DANCE_SLIDER_MOVER_TYPE = new ListOption("Slider mover", "SliderMover", "How to move in sliders") {
		private int val;

		@Override
		public String getValueString () {
			return Dancer.sliderMoverController.toString();
		}

		@Override
		public Object[] getListItems () {
			return Dancer.sliderMovers;
		}

		@Override
		public void clickListItem(int index){
			val = index;
			Dancer.sliderMoverController = Dancer.sliderMovers[index];
			this.notifyListeners();
		}

		@Override
		public String write () {
			return String.valueOf(val);
		}

		@Override
		public void read (String s){
			Dancer.sliderMoverController = Dancer.sliderMovers[val = Integer.parseInt(s)];
		}
	};

	public static final ListOption OPTION_DANCE_SPINNER = new ListOption("Algorithm", "Spinner", "Spinner style") {
		@Override
		public Object[] getListItems () {
			return Dancer.spinners;
		}

		@Override
		public void clickListItem(int index){
			Dancer.instance.setSpinnerIndex(index);
			this.notifyListeners();
		}

		@Override
		public String getValueString () {
			return Dancer.spinners[Dancer.instance.getSpinnerIndex()].toString();
		}

		@Override
		public String write () {
			return Dancer.instance.getSpinnerIndex() + "";
		}

		@Override
		public void read (String s){
			Dancer.instance.setSpinnerIndex(Integer.parseInt(s));
		}
	};

	public static final NumericOption OPTION_DANCE_SPINNER_DELAY = new NumericOption("Delay", "SpinnerDelay", "Fiddle with this if spinner goes too fast.", 3, 0, 20) {
		@Override
		public String getValueString () {
			return String.format("%dms", val);
		}
	};

	public static final ToggleOption OPTION_DANCE_LAZY_SLIDERS = new ToggleOption("Lazy sliders", "LazySliders", "Don't do short sliders", false);
	public static final ToggleOption OPTION_DANCE_ONLY_CIRCLE_STACKS = new ToggleOption("Only circle stacks", "CircleStacks", "Only do circle movement on stacks", false);
	public static final ToggleOption OPTION_DANCE_CIRCLE_STREAMS = new ToggleOption("Circle streams", "CircleStreams", "Make circles while streaming", false);
	public static final ToggleOption OPTION_DANCE_MIRROR = new ToggleOption("Mirror collage", "MirrorCollage", "Hypnotizing stuff. Toggle this ingame by pressing the M key.", false);
	public static final ToggleOption OPTION_WINDOW_TOOLWINDOW = new ToggleOption("Use utility windows", "WindowUtility", "Use windows that don't fill the task bar", false);
	public static final ToggleOption OPTION_WINDOW_CURSOR_RESIZE = new ToggleOption("Resize with cursor trail", "WindowCursorResize", "Resize the cursor window based on the trail", false);
	public static final ToggleOption OPTION_WINDOW_SLIDER_RESIZE = new ToggleOption("Resize with snaking sliders", "WindowApproachResize", "Resize the window based on snaking slider size", false);
	public static final ToggleOption OPTION_WINDOW_APPROACH_RESIZE = new ToggleOption("Resize with approach circles", "WindowApproachResize", "Let the approach circle dictate the size of the windows", false);
	public static final NumericOption OPTION_WINDOW_STATIC_APPROACH_SIZE = new NumericOption("Static size percentage", "WindowStaticSize", "Extra size of the window, based on approach circle's biggest size", 50, 0, 100) {
		@Override
		public boolean showCondition()
		{
			return !OPTION_WINDOW_APPROACH_RESIZE.state;
		}
	};
	public static final ToggleOption OPTION_DANCE_DRAW_APPROACH = new ToggleOption("Draw approach circles", "DrawApproach", "Can get a bit busy when using mirror collage", true);
	public static final ListOption OPTION_DANCE_OBJECT_COLOR_OVERRIDE = new ListOption("Color", "ObjColorOverride", "Override object colors") {
		@Override
		public String getValueString () {
			return Dancer.colorOverride.toString();
		}

		@Override
		public ObjectColorOverrides[] getListItems () {
			return ObjectColorOverrides.values();
		}

		@Override
		public void clickListItem(int index){
			Dancer.colorOverride = ObjectColorOverrides.values()[index];
			this.notifyListeners();
		}

		@Override
		public String write () {
			return "" + Dancer.colorOverride.nr;
		}

		@Override
		public void read (String s){
			Dancer.colorOverride = ObjectColorOverrides.values()[Integer.parseInt(s)];
		}
	};

	public static final ListOption OPTION_DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED = new ListOption("Mirror color", "ObjColorMirroredOverride", "Override collage object colors") {
		@Override
		public String getValueString () {
			return Dancer.colorMirrorOverride.toString();
		}

		@Override
		public Object[] getListItems () {
			return ObjectColorOverrides.values();
		}

		@Override
		public void clickListItem(int index){
			Dancer.colorMirrorOverride = ObjectColorOverrides.values()[index];
			this.notifyListeners();
		}

		@Override
		public String write () {
			return "" + Dancer.colorMirrorOverride.nr;
		}

		@Override
		public void read (String s){
			Dancer.colorMirrorOverride = ObjectColorOverrides.values()[Integer.parseInt(s)];
		}
	};

	public static final NumericOption OPTION_DANCE_RGB_OBJECT_INC = new NumericOption(
		"RGB object increment",
		"RGBInc",
		"Amount of hue to shift, used for rainbow object override",
		800,
		-6000,
		6000)
	{
		@Override
		public String getValueString () {
			return String.format("%.1f°/object", val / 100f);
		}

		@Override
		public boolean showCondition()
		{
			Object val;
			return
				(val = Dancer.colorOverride) == ObjectColorOverrides.RAINBOW ||
				val == ObjectColorOverrides.RAINBOWSHIFT ||
				(val = Dancer.colorMirrorOverride)
					== ObjectColorOverrides.RAINBOW ||
				val == ObjectColorOverrides.RAINBOWSHIFT;
		}
	};

	public static final ListOption OPTION_DANCE_CURSOR_COLOR_OVERRIDE = new ListOption(
		"Trail color",
		"CursorColorOverride",
		"Override cursor color")
	{
		@Override
		public String getValueString()
		{
			return cursorColor.name;
		}

		@Override
		public Object[] getListItems()
		{
			return CursorColorManager.impls;
		}

		@Override
		public void clickListItem(int index)
		{
			cursorColor = CursorColorManager.impls[index];
			this.notifyListeners();
		}

		@Override
		public String write ()
		{
			for (int i = CursorColorManager.impls.length; i > 0;) {
				if (CursorColorManager.impls[--i] == cursorColor) {
					return String.valueOf(i);
				}
			}
			return "0";
		}

		@Override
		public void read(String s)
		{
			final int idx = Integer.parseInt(s);
			if (0 <= idx && idx < CursorColorManager.impls.length) {
				cursorColor = CursorColorManager.impls[idx];
			}
		}
	};

	public static final NumericOption OPTION_RAINBOWTRAIL_SATURATION = new NumericOption(
		"Rainbow colors saturation",
		"DistanceRainbowSaturation",
		"Saturation for the rainbow trail color",
		100,
		0,
		100)
	{
		@Override
		public String getValueString ()
		{
			return String.valueOf(val);
		}

		@Override
		public boolean showCondition()
		{
			return CursorColorManager.shouldShowCursorHueIncSaturationOption();
		}
	};

	public static final Option WARNING_DISTANCE_RAINBOW_COLOR = new CustomRenderedOption(
		"",
		null,
		null)
	{
		@Override
		protected void registerOption()
		{
			// nulled
		}

		@Override
		public boolean showCondition()
		{
			return
				!OPTION_NEWEST_CURSOR.state &&
				CursorColorManager.needsNewestCursor();
		}

		@Override
		public int getHeight(int baseHeight)
		{
			return baseHeight * 2;
		}

		@Override
		public void render(int baseHeight, int x, int y, int textOffsetY, int width)
		{
			final String line1 = "Distance-based rainbow cursor color only";
			final String line2 = "works with newest cursor enabled!";
			int _y, _x;
			_y = y + textOffsetY + textOffsetY / 2;
			_x = x + (width - Fonts.MEDIUM.getWidth(line1)) / 2;
			Fonts.MEDIUM.drawString(_x , _y, line1, OptionsOverlay.COL_PINK);
			_y += baseHeight - textOffsetY;
			_x = x + (width - Fonts.MEDIUM.getWidth(line2)) / 2;
			Fonts.MEDIUM.drawString(_x , _y, line2, OptionsOverlay.COL_PINK);
		}
	};

	public static final ToggleOption
		OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL = new ToggleOption(
			"Only color cursor trail",
			"OnlyColorTrail",
			"Don't color the cursor, only the trail",
			true
		),
		OPTION_TRAIL_COLOR_PARTS = new ToggleOption(
			"Color trail segments individually",
			"TrailSegmentColors",
			"Give each trail segment a color instead of one color for the entire trail",
			true)
		{
			@Override
			public boolean showCondition()
			{
				return OPTION_NEWEST_CURSOR.state;
			}
		},
		OPTION_BLEND_TRAIL = new ToggleOption(
			"Additively blend trail",
			"cursor.blend.trail",
			"Add trail color so an orange trail over a cyan trail becomes white.",
			true)
		{
			@Override
			public boolean showCondition()
			{
				return OPTION_NEWEST_CURSOR.state;
			}
		},
		OPTION_BLEND_CURSOR = new ToggleOption(
			"Additively blend cursor",
			"cursor.blend.cursor",
			null,
			false)
		{
			@Override
			public boolean showCondition()
			{
				return OPTION_NEWEST_CURSOR.state;
			}
		};

	public static final NumericOption OPTION_DANCE_RGB_CURSOR_INC = new NumericOption(
		"RGB cursor increment",
		"RGBCursorInc",
		"Amount of hue to shift, used for rainbow cursor override",
		180,
		-360,
		360)
	{
		@Override
		public String getValueString () {
			return String.format("%d°/s", val);
		}

		@Override
		public boolean showCondition()
		{
			return CursorColorManager.shouldShowCursorHueIncSaturationOption();
		}
	};

	public static final NumericOption OPTION_DANCE_CURSOR_TRAIL_OVERRIDE = new NumericOption("Trail length", "CursorTrailOverride", "Override cursor trail length", 20, 20, 600) {
		@Override
		public String getValueString () {
			if (val == 20) {
				return "Disabled";
			}
			return "" + val;
		}
	};

	public static final ToggleOption OPTION_DANCE_HIDE_OBJECTS = new ToggleOption("Don't draw objects", "HideObj", "If you only want to see cursors :)", false);
	public static final ToggleOption OPTION_DANCE_CIRLCE_IN_SLOW_SLIDERS = new ToggleOption("Do circles in slow sliders", "CircleInSlider", "Circle around sliderball in lazy & slow sliders", false);
	public static final ToggleOption OPTION_DANCE_CIRLCE_IN_LAZY_SLIDERS = new ToggleOption("Do circles in lazy sliders", "CircleInLazySlider", "Circle in hitcircle in lazy sliders", false);
	public static final ToggleOption OPTION_DANCE_HIDE_UI = new ToggleOption("Hide all UI", "HideUI", null, true);
	public static final ToggleOption OPTION_DANCE_ENABLE_SB = new ToggleOption("Enable storyboard editor", "EnableStoryBoard", "Dance storyboard", false);
	public static final ToggleOption OPTION_PIPPI_ENABLE = new ToggleOption("Enable", "Pippi", "Move in circles like dancing pippi (osu! april fools joke 2016)", false);
	public static final NumericOption OPTION_PIPPI_RADIUS_PERCENT = new NumericOption("Radius", "PippiRad", "Radius of pippi, percentage of circle radius", 100, 0, 100) {
		@Override
		public String getValueString () {
			return val + "%";
		}
		@Override
		public void setValue ( int value){
			super.setValue(value);
			Pippi.setRadiusPercent(value);
		}
	};

	public static final NumericOption OPTION_PIPPI_ANGLE_INC_MUL = new NumericOption("Normal", "PippiAngIncMul", "How fast pippi's angle increments", 10, -200, 200) {
		@Override
		public String getValueString () {
			return String.format("x%.1f", val / 10f);
		}
	};

	public static final NumericOption OPTION_PIPPI_ANGLE_INC_MUL_SLIDER = new NumericOption("In slider", "PippiAngIncMulSlider", "Same as above, but in sliders", 50, -200, 200) {
		@Override
		public String getValueString () {
			return String.format("x%.1f", val / 10f);
		}
	};

	public static final ToggleOption OPTION_PIPPI_SLIDER_FOLLOW_EXPAND = new ToggleOption("Followcircle expand", "PippiFollowExpand", "Increase radius in followcircles", false);
	public static final ToggleOption OPTION_PIPPI_PREVENT_WOBBLY_STREAMS = new ToggleOption("Prevent wobbly streams", "PippiPreventWobblyStreams", "Force linear mover while doing streams to prevent wobbly pippi", true);
}
