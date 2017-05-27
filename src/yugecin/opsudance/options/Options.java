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
package yugecin.opsudance.options;

import awlex.ospu.polymover.factory.PolyMoverFactory;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.ui.Fonts;
import org.lwjgl.input.Keyboard;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.openal.SoundStore;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.*;
import yugecin.opsudance.events.BarNotifListener;
import yugecin.opsudance.movers.factories.ExgonMoverFactory;
import yugecin.opsudance.movers.factories.QuadraticBezierMoverFactory;
import yugecin.opsudance.movers.slidermovers.DefaultSliderMoverController;
import yugecin.opsudance.utils.CachedVariable;
import yugecin.opsudance.utils.CachedVariable.Getter;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * @author itdelatrisu (https://github.com/itdelatrisu) most functions are copied from itdelatrisu.opsu.Options.java
 */
public class Options {

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
	public static final ToggleOption OPTION_FULLSCREEN = new ToggleOption("Fullscreen Mode", "Fullscreen", "Restart to apply changes.", false);
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

	public static final NumericOption OPTION_TARGET_UPS = new NumericOption("target UPS", "targetUPS", "Higher values result in less input lag and smoother cursor trail, but may cause high CPU usage.", 480, 20, 1000) {
		@Override
		public String getValueString () {
			return String.format("%dups", val);
		}

		@Override
		public void setValue ( int value){
			super.setValue(value);
			displayContainer.setUPS(value);
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
			displayContainer.setFPS(targetFPS[targetFPSIndex]);
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

	public static final ToggleOption OPTION_NEW_CURSOR = new ToggleOption("Enable New Cursor", "NewCursor", "Use the new cursor style (may cause higher CPU usage).", true);
	public static final ToggleOption OPTION_DYNAMIC_BACKGROUND = new ToggleOption("Enable Dynamic Backgrounds", "DynamicBackground", "The song background will be used as the main menu background.", true);
	public static final ToggleOption OPTION_LOAD_VERBOSE = new ToggleOption("Show Detailed Loading Progress", "LoadVerbose", "Display more specific loading information in the splash screen.", false);
	public static final ToggleOption OPTION_COLOR_MAIN_MENU_LOGO = new ToggleOption("Use cursor color as main menu logo tint", "ColorMainMenuLogo", "Colorful main menu logo", false);
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
	public static final NumericOption OPTION_MUSIC_OFFSET = new NumericOption("Music Offset", "Offset", "Adjust this value if hit objects are out of sync.", -75, -500, 500) {
		@Override
		public String getValueString () {
			return String.format("%dms", val);
		}
	};

	public static final ToggleOption OPTION_DISABLE_SOUNDS = new ToggleOption("Disable All Sound Effects", "DisableSound", "May resolve Linux sound driver issues.  Requires a restart.", (System.getProperty("os.name").toLowerCase().contains("linux")));
	public static final GenericOption OPTION_KEY_LEFT = new GenericOption("Left Game Key", "keyOsuLeft", "Select this option to input a key.", Keyboard.KEY_Z, null, false) {
		@Override
		public String getValueString () {
			return Keyboard.getKeyName(intval);
		}

		@Override
		public String write () {
			return Keyboard.getKeyName(intval);
		}

		@Override
		public void read(String s){
			intval = Keyboard.getKeyIndex(s);
			if (intval == Keyboard.KEY_NONE) {
				intval = Keyboard.KEY_Y;
			}
		}
	};

	public static final GenericOption OPTION_KEY_RIGHT = new GenericOption("Right Game Key", "keyOsuRight", "Select this option to input a key.", Keyboard.KEY_X, null, false) {
		@Override
		public String getValueString () {
			return Keyboard.getKeyName(intval);
		}

		@Override
		public String write () {
			return Keyboard.getKeyName(intval);
		}

		@Override
		public void read(String s){
			intval = Keyboard.getKeyIndex(s);
			if (intval == Keyboard.KEY_NONE) {
				intval = Keyboard.KEY_X;
			}
		}
	};

	public static final NumericOption OPTION_BACKGROUND_DIM = new NumericOption("Background Dim", "DimLevel", "Percentage to dim the background image during gameplay.", 50, 0, 100);
	public static final ToggleOption OPTION_DISABLE_MOUSE_WHEEL = new ToggleOption("Disable mouse wheel in play mode", "MouseDisableWheel", "During play, you can use the mouse wheel to adjust the volume and pause the game. This will disable that functionality.", false);
	public static final ToggleOption OPTION_DISABLE_MOUSE_BUTTONS = new ToggleOption("Disable mouse buttons in play mode", "MouseDisableButtons", "This option will disable all mouse buttons. Specifically for people who use their keyboard to click.", false) {
		@Override
		public void toggle() {
			BarNotifListener.EVENT.make().onBarNotif(state ?
				"Mouse buttons are disabled." : "Mouse buttons are enabled.");
		}
	};
	public static final ToggleOption OPTION_DISABLE_CURSOR = new ToggleOption("Disable Cursor", "DisableCursor", "Hide the cursor sprite.", false);
	public static final ToggleOption OPTION_DANCE_REMOVE_BG = new ToggleOption("Use black background instead of image", "RemoveBG", "Hello darkness my old friend", true);
	public static final ToggleOption OPTION_FORCE_DEFAULT_PLAYFIELD = new ToggleOption("Force Default Playfield", "ForceDefaultPlayfield", "Override the song background with the default playfield background.", false);
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

	public static final NumericOption OPTION_MERGING_SLIDERS_MIRROR_POOL = new NumericOption("Merging sliders mirror pool", "MergingSliderMirrorPool", "Amount of mirrors to calculate for merging sliders (impacts performance)", 2, 1, 5) {
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
	public static final ToggleOption OPTION_SHOW_REVERSEARROW_ANIMATIONS = new ToggleOption("Show reverse arrow animations", "ReverseArrowAnimations", "Fade out reverse arrows after passing.", true);
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
				BarNotifListener.EVENT.make().onBarNotif("This mover is disabled in the storyboard right now");
				return;
			}
			Dancer.instance.setMoverFactoryIndex(index);
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
	public static final ToggleOption OPTION_DANCE_DRAW_APPROACH = new ToggleOption("Draw approach circles", "DrawApproach", "Can get a bit busy when using mirror collage", true);
	public static final ListOption OPTION_DANCE_OBJECT_COLOR_OVERRIDE = new ListOption("Color", "ObjColorOverride", "Override object colors") {
		@Override
		public String getValueString () {
			return Dancer.colorOverride.toString();
		}

		@Override
		public Object[] getListItems () {
			return ObjectColorOverrides.values();
		}

		@Override
		public void clickListItem(int index){
			Dancer.colorOverride = ObjectColorOverrides.values()[index];
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

	public static final NumericOption OPTION_DANCE_RGB_OBJECT_INC = new NumericOption("RGB increment", "RGBInc", "Amount of hue to shift, used for rainbow object override", 70, -1800, 1800) {
		@Override
		public String getValueString () {
			return String.format("%.1f°", val / 10f);
		}
	};

	public static final ListOption OPTION_DANCE_CURSOR_COLOR_OVERRIDE = new ListOption("Color", "CursorColorOverride", "Override cursor color") {
		@Override
		public String getValueString () {
			return Dancer.cursorColorOverride.toString();
		}

		@Override
		public Object[] getListItems () {
			return CursorColorOverrides.values();
		}

		@Override
		public void clickListItem(int index){
			Dancer.cursorColorOverride = CursorColorOverrides.values()[index];
		}

		@Override
		public String write () {
			return "" + Dancer.cursorColorOverride.nr;
		}

		@Override
		public void read (String s){
			Dancer.cursorColorOverride = CursorColorOverrides.values()[Integer.parseInt(s)];
		}
	};

	public static final ListOption OPTION_DANCE_CURSOR_MIRROR_COLOR_OVERRIDE = new ListOption("Mirror color", "CursorMirrorColorOverride", "Override mirror cursor color") {
		@Override
		public String getValueString () {
			return Dancer.cursorColorMirrorOverride.toString();
		}

		@Override
		public Object[] getListItems () {
			return CursorColorOverrides.values();
		}

		@Override
		public void clickListItem(int index){
			Dancer.cursorColorMirrorOverride = CursorColorOverrides.values()[index];
		}

		@Override
		public String write () {
			return "" + Dancer.cursorColorMirrorOverride.nr;
		}

		@Override
		public void read (String s){
			Dancer.cursorColorMirrorOverride = CursorColorOverrides.values()[Integer.parseInt(s)];
		}
	};

	public static final ToggleOption OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL = new ToggleOption("Only color cursor trail", "OnlyColorTrail", "Don't color the cursor, only the trail", false);
	public static final NumericOption OPTION_DANCE_RGB_CURSOR_INC = new NumericOption("RGB cursor increment", "RGBCursorInc", "Amount of hue to shift, used for rainbow cursor override", 100, -2000, 2000) {
		@Override
		public String getValueString () {
			return String.format("%.2f°", val / 1000f);
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
	public static final ToggleOption OPTION_DANCE_HIDE_UI = new ToggleOption("Hide all UI", "HideUI", ".", true);
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
