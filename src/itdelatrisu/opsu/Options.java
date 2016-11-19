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
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.TimingPoint;
import itdelatrisu.opsu.skins.Skin;
import itdelatrisu.opsu.skins.SkinLoader;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.UI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.ClasspathLocation;
import org.newdawn.slick.util.FileSystemLocation;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import yugecin.opsudance.*;
import yugecin.opsudance.movers.factories.AutoMoverFactory;
import yugecin.opsudance.spinners.Spinner;
import yugecin.opsudance.ui.SBOverlay;

/**
 * Handles all user options.
 */
public class Options {
	/** Whether to use XDG directories. */
	public static final boolean USE_XDG = checkXDGFlag();

	/** The config directory. */
	private static final File CONFIG_DIR = getXDGBaseDir("XDG_CONFIG_HOME", ".config");

	/** The data directory. */
	private static final File DATA_DIR = getXDGBaseDir("XDG_DATA_HOME", ".local/share");

	/** The cache directory. */
	private static final File CACHE_DIR = getXDGBaseDir("XDG_CACHE_HOME", ".cache");

	/** File for logging errors. */
	public static final File LOG_FILE = new File(CONFIG_DIR, ".opsu.log");

	/** File for storing user options. */
	private static final File OPTIONS_FILE = new File(CONFIG_DIR, ".opsu.cfg");

	/** The default beatmap directory (unless an osu! installation is detected). */
	private static final File BEATMAP_DIR = new File(DATA_DIR, "Songs/");

	/** The default skin directory (unless an osu! installation is detected). */
	private static final File SKIN_ROOT_DIR = new File(DATA_DIR, "Skins/");

	/** Cached beatmap database name. */
	public static final File BEATMAP_DB = new File(DATA_DIR, ".opsu.db");

	/** Score database name. */
	public static final File SCORE_DB = new File(DATA_DIR, ".opsu_scores.db");

	/** Directory where natives are unpacked. */
	public static final File NATIVE_DIR = new File(CACHE_DIR, "Natives/");

	/** Font file name. */
	public static final String FONT_NAME = "DroidSansFallback.ttf";

	/** Version file name. */
	public static final String VERSION_FILE = "version";

	/** Repository address. */
	public static final URI REPOSITORY_URI = URI.create("https://github.com/itdelatrisu/opsu");

	/** Dance repository address. */
	public static final URI DANCE_REPOSITORY_URI = URI.create("https://github.com/yugecin/opsu-dance");

	/** Issue reporting address. */
	public static final String ISSUES_URL = "https://github.com/yugecin/opsu-dance/issues/new?title=%s&body=%s";

	/** Address containing the latest version file. */
	public static final String VERSION_REMOTE = "https://raw.githubusercontent.com/yugecin/opsu-dance/master/version";

	/** The beatmap directory. */
	private static File beatmapDir;

	/** The OSZ archive directory. */
	private static File oszDir;

	/** The screenshot directory (created when needed). */
	private static File screenshotDir;

	/** The replay directory (created when needed). */
	private static File replayDir;

	/** The replay import directory. */
	private static File replayImportDir;

	/** The root skin directory. */
	private static File skinRootDir;

	/** Port binding. */
	private static int port = 49250;

	private static boolean noSingleInstance;

	/**
	 * Returns whether the XDG flag in the manifest (if any) is set to "true".
	 * @return true if XDG directories are enabled, false otherwise
	 */
	private static boolean checkXDGFlag() {
		JarFile jarFile = Utils.getJarFile();
		if (jarFile == null)
			return false;
		try {
			Manifest manifest = jarFile.getManifest();
			if (manifest == null)
				return false;
			Attributes attributes = manifest.getMainAttributes();
			String value = attributes.getValue("Use-XDG");
			return (value != null && value.equalsIgnoreCase("true"));
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns the directory based on the XDG base directory specification for
	 * Unix-like operating systems, only if the "XDG" flag is enabled.
	 * @param env the environment variable to check (XDG_*_*)
	 * @param fallback the fallback directory relative to ~home
	 * @return the XDG base directory, or the working directory if unavailable
	 */
	private static File getXDGBaseDir(String env, String fallback) {
		if (!USE_XDG)
			return new File("./");

		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0) {
			String rootPath = System.getenv(env);
			if (rootPath == null) {
				String home = System.getProperty("user.home");
				if (home == null)
					return new File("./");
				rootPath = String.format("%s/%s", home, fallback);
			}
			File dir = new File(rootPath, "opsu");
			if (!dir.isDirectory() && !dir.mkdir())
				ErrorHandler.error(String.format("Failed to create configuration folder at '%s/opsu'.", rootPath), null, false);
			return dir;
		} else
			return new File("./");
	}

	/**
	 * Returns the osu! installation directory.
	 * @return the directory, or null if not found
	 */
	private static File getOsuInstallationDirectory() {
		if (!System.getProperty("os.name").startsWith("Win"))
			return null;  // only works on Windows

		// registry location
		final WinReg.HKEY rootKey = WinReg.HKEY_CLASSES_ROOT;
		final String regKey = "osu\\DefaultIcon";
		final String regValue = null; // default value
		final String regPathPattern = "\"(.+)\\\\[^\\/]+\\.exe\"";

		String value;
		try {
			value = Advapi32Util.registryGetStringValue(rootKey, regKey, regValue);
		} catch (Win32Exception e) {
			return null;  // key/value not found
		}
		Pattern pattern = Pattern.compile(regPathPattern);
		Matcher m = pattern.matcher(value);
		if (!m.find())
			return null;
		File dir = new File(m.group(1));
		return (dir.isDirectory()) ? dir : null;
	}

	/**
	 * The theme song string:
	 * {@code filename,title,artist,length(ms)}
	 */
	private static String themeString = "theme.ogg,On the Bach,Jingle Punks,66000";

	/** Game options. */
	public enum GameOption {
		// internal options (not displayed in-game)
		BEATMAP_DIRECTORY ("BeatmapDirectory") {
			@Override
			public String write() { return getBeatmapDir().getAbsolutePath(); }

			@Override
			public void read(String s) { beatmapDir = new File(s); }
		},
		OSZ_DIRECTORY ("OSZDirectory") {
			@Override
			public String write() { return getOSZDir().getAbsolutePath(); }

			@Override
			public void read(String s) { oszDir = new File(s); }
		},
		SCREENSHOT_DIRECTORY ("ScreenshotDirectory") {
			@Override
			public String write() { return getScreenshotDir().getAbsolutePath(); }

			@Override
			public void read(String s) { screenshotDir = new File(s); }
		},
		REPLAY_DIRECTORY ("ReplayDirectory") {
			@Override
			public String write() { return getReplayDir().getAbsolutePath(); }

			@Override
			public void read(String s) { replayDir = new File(s); }
		},
		REPLAY_IMPORT_DIRECTORY ("ReplayImportDirectory") {
			@Override
			public String write() { return getReplayImportDir().getAbsolutePath(); }

			@Override
			public void read(String s) { replayImportDir = new File(s); }
		},
		SKIN_DIRECTORY ("SkinDirectory") {
			@Override
			public String write() { return getSkinRootDir().getAbsolutePath(); }

			@Override
			public void read(String s) { skinRootDir = new File(s); }
		},
		THEME_SONG ("ThemeSong") {
			@Override
			public String write() { return themeString; }

			@Override
			public void read(String s) { themeString = s; }
		},
		PORT ("Port") {
			@Override
			public String write() { return Integer.toString(port); }

			@Override
			public void read(String s) {
				int i = Integer.parseInt(s);
				if (i > 0 && i <= 65535)
					port = i;
			}
		},
		NOSINGLEINSTANCE ("NoSingleInstance") {
			@Override
			public String write() { return noSingleInstance + ""; }

			@Override
			public void read(String s) {
				noSingleInstance = !"false".equals(s);
			}
		},

		// in-game options
		SCREEN_RESOLUTION ("Screen Resolution", "ScreenResolution", "Restart (Ctrl+Shift+F5) to apply resolution changes.") {
			@Override
			public String getValueString() {
				return resolutions[resolutionIdx];
			}

			@Override
			public Object[] getListItems() {
				return resolutions;
			}

			@Override
			public void clickListItem(int index) {
				resolutionIdx = index;
			}

			@Override
			public void read(String s) {
				try {
					resolutionIdx = Integer.parseInt(s);
				} catch (NumberFormatException ignored) { }
			}

			@Override
			public String write() {
				return resolutionIdx + "";
			}
		},
		ALLOW_LARGER_RESOLUTIONS ("Allow large resolutions", "AllowLargeRes", "Allow resolutions larger than the native resolution", false),
		FULLSCREEN ("Fullscreen Mode", "Fullscreen", "Restart to apply changes.", false),
		SKIN ("Skin", "Skin", "Restart (Ctrl+Shift+F5) to apply skin changes.") {
			@Override
			public String getValueString() { return skinName; }

			@Override
			public Object[] getListItems() {
				return skinDirs;
			}

			@Override
			public void clickListItem(int index) {
				skinName = skinDirs[index];
			}

			@Override
			public void read(String s) { skinName = s; }
		},
		TARGET_FPS ("Frame Limiter", "FrameSync", "Higher values may cause high CPU usage.") {
			@Override
			public String getValueString() {
				return String.format((getTargetFPS() == 60) ? "%dfps (vsync)" : "%dfps", getTargetFPS());
			}

			@Override
			public Object[] getListItems() {
				String[] list = new String[targetFPS.length];
				for (int i = 0; i < targetFPS.length; i++) {
					list[i] = String.format(targetFPS[i] == 60 ? "%dfps (vsync)" : "%dfps", targetFPS[i]);
				}
				return list;
			}

			@Override
			public void clickListItem(int index) {
				targetFPSindex = index;
				Container.instance.setTargetFrameRate(targetFPS[index]);
				Container.instance.setVSync(targetFPS[index] == 60);
			}

			@Override
			public String write() { return Integer.toString(targetFPS[targetFPSindex]); }

			@Override
			public void read(String s) {
				int i = Integer.parseInt(s);
				for (int j = 0; j < targetFPS.length; j++) {
					if (i == targetFPS[j]) {
						targetFPSindex = j;
						break;
					}
				}
			}
		},
		SHOW_FPS ("Show FPS Counter", "FpsCounter", "Show an FPS counter in the bottom-right hand corner.", true),
		SHOW_UNICODE ("Prefer Non-English Metadata", "ShowUnicode", "Where available, song titles will be shown in their native language.", false) {
			@Override
			public void click(GameContainer container) {
				super.click(container);
				if (bool) {
					try {
						Fonts.LARGE.loadGlyphs();
						Fonts.MEDIUM.loadGlyphs();
						Fonts.DEFAULT.loadGlyphs();
					} catch (SlickException e) {
						Log.warn("Failed to load glyphs.", e);
					}
				}
			}
		},
		SCREENSHOT_FORMAT ("Screenshot Format", "ScreenshotFormat", "Press F12 to take a screenshot.") {
			@Override
			public String getValueString() { return screenshotFormat[screenshotFormatIndex].toUpperCase(); }

			@Override
			public Object[] getListItems() {
				return screenshotFormat;
			}

			@Override
			public void clickListItem(int index) {
				screenshotFormatIndex = index;
			}

			@Override
			public String write() { return Integer.toString(screenshotFormatIndex); }

			@Override
			public void read(String s) {
				int i = Integer.parseInt(s);
				if (i >= 0 && i < screenshotFormat.length)
					screenshotFormatIndex = i;
			}
		},
		CURSOR_SIZE ("Cursor Size", "CursorSize", "Change the cursor scale.", 100, 50, 200) {
			@Override
			public String getValueString() { return String.format("%.2fx", val / 100f); }

			@Override
			public String write() { return String.format(Locale.US, "%.2f", val / 100f); }

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 100f);
				if (i >= 50 && i <= 200)
					val = i;
			}
		},
		NEW_CURSOR ("Enable New Cursor", "NewCursor", "Use the new cursor style (may cause higher CPU usage).", true) {
			@Override
			public void click(GameContainer container) {
				super.click(container);
				UI.getCursor().reset();
			}
		},
		DYNAMIC_BACKGROUND ("Enable Dynamic Backgrounds", "DynamicBackground", "The song background will be used as the main menu background.", true),
		LOAD_VERBOSE ("Show Detailed Loading Progress", "LoadVerbose", "Display more specific loading information in the splash screen.", false),
		MASTER_VOLUME ("Master Volume", "VolumeUniversal", "Global volume level.", 35, 0, 100) {
			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				container.setMusicVolume(getMasterVolume() * getMusicVolume());
			}
		},
		MUSIC_VOLUME ("Music Volume", "VolumeMusic", "Volume of music.", 80, 0, 100) {
			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				container.setMusicVolume(getMasterVolume() * getMusicVolume());
			}
		},
		SAMPLE_VOLUME_OVERRIDE ("Sample volume override", "BMSampleOverride", "Override beatmap hitsound volume", 100, 0, 100) {
			@Override
			public String getValueString() {
				if (val == 0) {
					return "Disabled";
				}
				return super.getValueString();
			}
		},
		EFFECT_VOLUME ("Effect Volume", "VolumeEffect", "Volume of menu and game sounds.", 70, 0, 100),
		HITSOUND_VOLUME ("Hit Sound Volume", "VolumeHitSound", "Volume of hit sounds.", 30, 0, 100),
		MUSIC_OFFSET ("Music Offset", "Offset", "Adjust this value if hit objects are out of sync.", -75, -500, 500) {
			@Override
			public String getValueString() { return String.format("%dms", val); }
		},
		DISABLE_SOUNDS ("Disable All Sound Effects", "DisableSound", "May resolve Linux sound driver issues.  Requires a restart.",
				(System.getProperty("os.name").toLowerCase().contains("linux"))),
		KEY_LEFT ("Left Game Key", "keyOsuLeft", "Select this option to input a key.") {
			@Override
			public String getValueString() { return Keyboard.getKeyName(getGameKeyLeft()); }

			@Override
			public String write() { return Keyboard.getKeyName(getGameKeyLeft()); }

			@Override
			public void read(String s) { setGameKeyLeft(Keyboard.getKeyIndex(s)); }
		},
		KEY_RIGHT ("Right Game Key", "keyOsuRight", "Select this option to input a key.") {
			@Override
			public String getValueString() { return Keyboard.getKeyName(getGameKeyRight()); }

			@Override
			public String write() { return Keyboard.getKeyName(getGameKeyRight()); }

			@Override
			public void read(String s) { setGameKeyRight(Keyboard.getKeyIndex(s)); }
		},
		DISABLE_MOUSE_WHEEL ("Disable mouse wheel in play mode", "MouseDisableWheel", "During play, you can use the mouse wheel to adjust the volume and pause the game. This will disable that functionality.", false),
		DISABLE_MOUSE_BUTTONS ("Disable mouse buttons in play mode", "MouseDisableButtons", "This option will disable all mouse buttons. Specifically for people who use their keyboard to click.", false),
		DISABLE_CURSOR ("Disable Cursor", "DisableCursor", "Hide the cursor sprite.", false),
		BACKGROUND_DIM ("Background Dim", "DimLevel", "Percentage to dim the background image during gameplay.", 50, 0, 100),
		FORCE_DEFAULT_PLAYFIELD ("Force Default Playfield", "ForceDefaultPlayfield", "Override the song background with the default playfield background.", false),
		IGNORE_BEATMAP_SKINS ("Ignore All Beatmap Skins", "IgnoreBeatmapSkins", "Never use skin element overrides provided by beatmaps.", false),
		SNAKING_SLIDERS ("Snaking sliders", "SnakingSliders", "Sliders gradually snake out from their starting point.", true),
		FALLBACK_SLIDERS ("Fallback sliders", "FallbackSliders", "Enable this if sliders won't render", false),
		SHOW_HIT_LIGHTING ("Show Hit Lighting", "HitLighting", "Adds an effect behind hit explosions.", true),
		SHOW_COMBO_BURSTS ("Show Combo Bursts", "ComboBurst", "A character image is displayed at combo milestones.", true),
		SHOW_PERFECT_HIT ("Show Perfect Hits", "PerfectHit", "Whether to show perfect hit result bursts (300s, slider ticks).", true),
		SHOW_FOLLOW_POINTS ("Show Follow Points", "FollowPoints", "Whether to show follow points between hit objects.", true),
		SHOW_HIT_ERROR_BAR ("Show Hit Error Bar", "ScoreMeter", "Shows precisely how accurate you were with each hit.", false),
		MAP_START_DELAY ("Map start delay", "StartDelay", "Have a fix amount of time to prepare your play/record", 20, 1, 50) {
			@Override
			public String getValueString() {
				return String.valueOf(val * 100);
			}
		},
		MAP_END_DELAY ("Map end delay", "EndDelay", "Have a fix amount of time at the and of the map for a smooth finish", 50, 1, 50) {
			@Override
			public String getValueString() {
				return String.valueOf(val * 100);
			}
		},
		EPILEPSY_WARNING ("Epilepsy warning image", "EpiWarn", "Show a little warning for flashing colours in the beginning", 20, 0, 20) {
			@Override
			public String getValueString() {
				if (val == 0) {
					return "Disabled";
				}
				return String.valueOf(val * 100);
			}
		},
		LOAD_HD_IMAGES ("Load HD Images", "LoadHDImages", String.format("Loads HD (%s) images when available. Increases memory usage and loading times.", GameImage.HD_SUFFIX), true),
		FIXED_CS ("Fixed Circle Size (CS)", "FixedCS", "Determines the size of circles and sliders.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }

			@Override
			public String write() { return String.format(Locale.US, "%.1f", val / 10f); }

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= 0 && i <= 100)
					val = i;
			}
		},
		FIXED_HP ("Fixed HP Drain Rate (HP)", "FixedHP", "Determines the rate at which health decreases.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }

			@Override
			public String write() { return String.format(Locale.US, "%.1f", val / 10f); }

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= 0 && i <= 100)
					val = i;
			}
		},
		FIXED_AR ("Fixed Approach Rate (AR)", "FixedAR", "Determines how long hit circles stay on the screen.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }

			@Override
			public String write() { return String.format(Locale.US, "%.1f", val / 10f); }

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= 0 && i <= 100)
					val = i;
			}
		},
		FIXED_OD ("Fixed Overall Difficulty (OD)", "FixedOD", "Determines the time window for hit results.", 0, 0, 100) {
			@Override
			public String getValueString() { return (val == 0) ? "Disabled" : String.format("%.1f", val / 10f); }

			@Override
			public String write() { return String.format(Locale.US, "%.1f", val / 10f); }

			@Override
			public void read(String s) {
				int i = (int) (Float.parseFloat(s) * 10f);
				if (i >= 0 && i <= 100)
					val = i;
			}
		},
		CHECKPOINT ("Track Checkpoint", "Checkpoint", "Press Ctrl+L while playing to load a checkpoint, and Ctrl+S to set one.", 0, 0, 3599) {
			@Override
			public String getValueString() {
				return (val == 0) ? "Disabled" : String.format("%02d:%02d",
						TimeUnit.SECONDS.toMinutes(val),
						val - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(val)));
			}
		},
		ENABLE_THEME_SONG ("Enable Theme Song", "MenuMusic", "Whether to play the theme song upon starting opsu!", true),
		REPLAY_SEEKING ("Replay Seeking", "ReplaySeeking", "Enable a seeking bar on the left side of the screen during replays.", false),
		DISABLE_UPDATER ("Disable Automatic Updates", "DisableUpdater", "Disable automatic checking for updates upon starting opsu!.", false),
		ENABLE_WATCH_SERVICE ("Enable Watch Service", "WatchService", "Watch the beatmap directory for changes. Requires a restart.", false),

		DANCE_MOVER_TYPE("Mover Type", "Mover type", "More Points", Dancer.multipoint) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.multipoint = bool;
			}
			
			@Override
			public void read(String s) {
				super.read(s);
				Dancer.multipoint = bool;
			}
		},
		
		DANCE_MOVER ("Mover algorithm", "Mover", "Algorithm that decides how to move from note to note" ) {
			@Override
			public Object[] getListItems() {
				return Dancer.multipoint ? Dancer.polyMoverFactories : Dancer.moverFactories;
			}

			@Override
			public void clickListItem(int index) {
				if (Dancer.multipoint)
					Dancer.instance.setMoverFactoryIndex(index);
				else
					Dancer.instance.setMoverFactoryIndex(index);
			}

			@Override
			public String getValueString() {
				return Dancer.multipoint ? Dancer.polyMoverFactories[Dancer.instance.getPolyMoverFactoryIndex()].toString() : Dancer.moverFactories[Dancer.instance.getMoverFactoryIndex()].toString();
			}

			@Override
			public String write() {
				return String.valueOf(Dancer.multipoint ? Dancer.instance.getPolyMoverFactoryIndex() : Dancer.instance.getMoverFactoryIndex());
			}

			@Override
			public void read(String s) {
				int i = Integer.parseInt(s);
				if (Dancer.multipoint)
					Dancer.instance.setPolyMoverFactoryIndex(i);
				else
					Dancer.instance.setMoverFactoryIndex(i);
			}
		},

		DANCE_MOVER_DIRECTION ("Mover direction", "MoverDirection", "The direction the mover goes" ) {
			@Override
			public String getValueString() {
				return Dancer.moverDirection.toString();
			}

			@Override
			public Object[] getListItems() {
				return MoverDirection.values();
			}

			@Override
			public void clickListItem(int index) {
				Dancer.moverDirection = MoverDirection.values()[index];
			}

			@Override
			public String write() {
				return "" + Dancer.moverDirection.nr;
			}

			@Override
			public void read(String s) {
				Dancer.moverDirection = MoverDirection.values()[Integer.parseInt(s)];
			}
		},

		DANCE_SLIDER_MOVER_TYPE ("Slider mover", "SliderMover", "How to move in sliders") {
			@Override
			public String getValueString() {
				return Dancer.sliderMoverController.toString();
			}

			@Override
			public Object[] getListItems() {
				return Dancer.sliderMovers;
			}

			@Override
			public void clickListItem(int index) {
				val = index;
				Dancer.sliderMoverController = Dancer.sliderMovers[index];
			}

			@Override
			public String write() {
				return String.valueOf(val);
			}

			@Override
			public void read(String s) {
				Dancer.sliderMoverController = Dancer.sliderMovers[val = Integer.parseInt(s)];
			}
		},

		DANCE_SPINNER ("Spinner", "Spinner", "Spinner style") {
			@Override
			public Object[] getListItems() {
				return Dancer.spinners;
			}

			@Override
			public void clickListItem(int index) {
				Dancer.instance.setSpinnerIndex(index);
			}

			@Override
			public String getValueString() {
				return Dancer.spinners[Dancer.instance.getSpinnerIndex()].toString();
			}

			@Override
			public String write() {
				return Dancer.instance.getSpinnerIndex() + "";
			}

			@Override
			public void read(String s) {
				Dancer.instance.setSpinnerIndex(Integer.parseInt(s));
			}
		},

		DANCE_SPINNER_DELAY ("Spinner delay", "SpinnerDelay", "Fiddle with this if spinner goes too fast.", Spinner.DELAY, 0, 200) {
			@Override
			public String getValueString() {
				return String.format("%dms", val / 10);
			}

			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				Spinner.DELAY = val / 10;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Spinner.DELAY = val / 10;
			}
		},

		DANCE_LAZY_SLIDERS ("Lazy sliders", "LazySliders", "Don't do short sliders", Dancer.LAZY_SLIDERS) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.LAZY_SLIDERS = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.LAZY_SLIDERS = bool;
			}
		},

		DANCE_ONLY_CIRCLE_STACKS ("Only circle stacks", "CircleStacks", "Only do circle movement on stacks", AutoMoverFactory.ONLY_CIRCLE_STACKS) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				AutoMoverFactory.ONLY_CIRCLE_STACKS = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				AutoMoverFactory.ONLY_CIRCLE_STACKS = bool;
			}
		},

		DANCE_CIRCLE_STREAMS ("Circle streams", "CircleStreams", "Make circles while streaming", AutoMoverFactory.CIRCLE_STREAM == 58) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				AutoMoverFactory.CIRCLE_STREAM = bool ? 58 : 85;
			}

			@Override
			public void read(String s) {
				super.read(s);
				AutoMoverFactory.CIRCLE_STREAM = bool ? 58 : 85;
			}
		},

		DANCE_MIRROR ("Mirror collage", "MirrorCollage", "Hypnotizing stuff. Toggle this ingame by pressing the M key.", Dancer.mirror) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.mirror = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.mirror = bool;
			}
		},

		DANCE_DRAW_APPROACH ("Draw approach circles", "DrawApproach", "Can get a bit busy when using mirror collage", Dancer.drawApproach) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.drawApproach = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.drawApproach = bool;
			}
		},

		DANCE_OBJECT_COLOR_OVERRIDE ("Object color override", "ObjColorOverride", "Override object colors") {
			@Override
			public String getValueString() {
				return Dancer.colorOverride.toString();
			}

			@Override
			public Object[] getListItems() {
				return ObjectColorOverrides.values();
			}

			@Override
			public void clickListItem(int index) {
				Dancer.colorOverride = ObjectColorOverrides.values()[index];
			}

			@Override
			public String write() {
				return "" + Dancer.colorOverride.nr;
			}

			@Override
			public void read(String s) {
				Dancer.colorOverride = ObjectColorOverrides.values()[Integer.parseInt(s)];
			}
		},

		DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED ("Collage object color override", "ObjColorMirroredOverride", "Override collage object colors") {
			@Override
			public String getValueString() {
				return Dancer.colorMirrorOverride.toString();
			}

			@Override
			public Object[] getListItems() {
				return ObjectColorOverrides.values();
			}

			@Override
			public void clickListItem(int index) {
				Dancer.colorMirrorOverride = ObjectColorOverrides.values()[index];
			}

			@Override
			public String write() {
				return "" + Dancer.colorMirrorOverride.nr;
			}

			@Override
			public void read(String s) {
				Dancer.colorMirrorOverride = ObjectColorOverrides.values()[Integer.parseInt(s)];
			}
		},

		DANCE_RGB_OBJECT_INC ("RGB objects increment", "RGBInc", "Amount of hue to shift, used for rainbow object override", Dancer.rgbhueinc, -1800, 1800) {
			@Override
			public String getValueString() {
				return String.format("%.1f°", val / 10f);
			}

			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				Dancer.rgbhueinc = val;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.rgbhueinc = val;
			}
		},

		DANCE_CURSOR_COLOR_OVERRIDE ("Cursor color override", "CursorColorOverride", "Override cursor color") {
			@Override
			public String getValueString() {
				return Dancer.cursorColorOverride.toString();
			}

			@Override
			public Object[] getListItems() {
				return CursorColorOverrides.values();
			}

			@Override
			public void clickListItem(int index) {
				Dancer.cursorColorOverride = CursorColorOverrides.values()[index];
			}

			@Override
			public String write() {
				return "" + Dancer.cursorColorOverride.nr;
			}

			@Override
			public void read(String s) {
				Dancer.cursorColorOverride = CursorColorOverrides.values()[Integer.parseInt(s)];
			}
		},

		DANCE_CURSOR_MIRROR_COLOR_OVERRIDE ("Cursor mirror color override", "CursorMirrorColorOverride", "Override mirror cursor color") {
			@Override
			public String getValueString() {
				return Dancer.cursorColorMirrorOverride.toString();
			}

			@Override
			public Object[] getListItems() {
				return CursorColorOverrides.values();
			}

			@Override
			public void clickListItem(int index) {
				Dancer.cursorColorMirrorOverride = CursorColorOverrides.values()[index];
			}

			@Override
			public String write() {
				return "" + Dancer.cursorColorMirrorOverride.nr;
			}

			@Override
			public void read(String s) {
				Dancer.cursorColorMirrorOverride = CursorColorOverrides.values()[Integer.parseInt(s)];
			}
		},

		DANCE_CURSOR_ONLY_COLOR_TRAIL ("Only color cursor trail", "OnlyColorTrail", "Don't color the cursor, only the trail", Dancer.onlycolortrail) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.onlycolortrail = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.onlycolortrail = bool;
			}
		},

		DANCE_RGB_CURSOR_INC ("RGB cursor increment", "RGBCursorInc", "Amount of hue to shift, used for rainbow cursor override", Dancer.rgbhueinc, -2000, 2000) {
			@Override
			public String getValueString() {
				return String.format("%.2f°", val / 1000f);
			}

			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				Dancer.rgbcursorhueinc = val;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.rgbcursorhueinc = val;
			}
		},

		DANCE_CURSOR_TRAIL_OVERRIDE ("Cursor trail length override", "CursorTrailOverride", "Override cursor trail length", Dancer.cursortraillength, 20, 400) {
			@Override
			public String getValueString() {
				if (val == 20) {
					return "Disabled";
				}
				return "" + val;
			}

			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				Dancer.cursortraillength = val;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.cursortraillength = val;
			}
		},

		DANCE_HIDE_OBJECTS ("Don't draw objects", "HideObj", "If you only want to see cursors :)", Dancer.hideobjects) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.hideobjects = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.hideobjects = bool;
			}
		},

		DANCE_REMOVE_BG ("Use black background instead of image", "RemoveBG", "Hello darkness my old friend", Dancer.removebg) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.removebg = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.removebg = bool;
			}
		},

		DANCE_CIRLCE_IN_SLOW_SLIDERS ("Do circles in slow sliders", "CircleInSlider", "Circle around sliderball in lazy & slow sliders", Pippi.circleSlowSliders) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Pippi.circleSlowSliders = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Pippi.circleSlowSliders = bool;
			}
		},

		DANCE_CIRLCE_IN_LAZY_SLIDERS ("Do circles in lazy sliders", "CircleInLazySlider", "Circle in hitcircle in lazy sliders", Pippi.circleLazySliders) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Pippi.circleLazySliders = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Pippi.circleLazySliders = bool;
			}
		},

		DANCE_HIDE_UI ("Hide all UI", "HideUI", ".", Dancer.hideui) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Dancer.hideui = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Dancer.hideui = bool;
			}
		},

		DANCE_ENABLE_SB ("Enable storyboard", "EnableStoryBoard", "Dance storyboard", false) {
			@Override
			public void click(GameContainer container) {
				super.click(container);
				SBOverlay.isActive = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				SBOverlay.isActive = bool;
			}
		},

		DANCE_HIDE_WATERMARK ("Hide watermark", "HideWaterMark", "Hide the githublink in the top left corner of the playfield", false) {
			@Override
			public String getValueString() {
				return Dancer.hidewatermark ? "Yes" : "No";
			}

			@Override
			public void click(GameContainer container) {
				Dancer.hidewatermark = false;
			}

			@Override
			public boolean showRWM() {
				return !Dancer.hidewatermark;
			}
		},

		PIPPI_ENABLE ("Pippi", "Pippi", "Move in circles like dancing pippi (osu! april fools joke 2016)", Pippi.enabled) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Pippi.enabled = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Pippi.enabled = bool;
			}
		},

		PIPPI_ANGLE_INC_MUL("Pippi angle increment multiplier", "PippiAngIncMul", "How fast pippi's angle increments", Pippi.angleInc, -200, 200) {
			@Override
			public String getValueString() {
				return String.format("x%.1f", val / 10f);
			}

			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				Pippi.angleInc = val;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Pippi.angleInc = val;
			}
		},

		PIPPI_ANGLE_INC_MUL_SLIDER ("Pippi angle increment multiplier slider", "PippiAngIncMulSlider", "Same as above, but in sliders", Pippi.angleSliderInc, -200, 200) {
			@Override
			public String getValueString() {
				return String.format("x%.1f", val / 10f);
			}

			@Override
			public void drag(GameContainer container, int d) {
				super.drag(container, d);
				Pippi.angleSliderInc = val;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Pippi.angleSliderInc = val;
			}
		},

		PIPPI_SLIDER_FOLLOW_EXPAND ("Followcircle expand", "PippiFollowExpand", "Increase radius in followcircles", Pippi.followcircleExpand) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Pippi.followcircleExpand = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Pippi.followcircleExpand = bool;
			}
		},

		PIPPI_PREVENT_WOBBLY_STREAMS ("Prevent wobbly streams", "PippiPreventWobblyStreams", "Force linear mover while doing streams to prevent wobbly pippi", Pippi.preventWobblyStreams) {
			@Override
			public void click(GameContainer container) {
				bool = !bool;
				Pippi.preventWobblyStreams = bool;
			}

			@Override
			public void read(String s) {
				super.read(s);
				Pippi.preventWobblyStreams = bool;
			}
		};


		/** Option name. */
		private final String name;

		/** Option name, as displayed in the configuration file. */
		private final String displayName;

		/** Option description. */
		private final String description;

		/** The boolean value for the option (if applicable). */
		protected boolean bool;

		/** The integer value for the option (if applicable). */
		protected int val;

		/** The upper and lower bounds on the integer value (if applicable). */
		private int max, min;

		/** Option types. */
		private enum OptionType { BOOLEAN, NUMERIC, OTHER };

		/** Whether or not this is a numeric option. */
		private OptionType type = OptionType.OTHER;

		/**
		 * Constructor for internal options (not displayed in-game).
		 * @param displayName the option name, as displayed in the configuration file
		 */
		GameOption(String displayName) {
			this(null, displayName, null);
		}

		/**
		 * Constructor for other option types.
		 * @param name the option name
		 * @param displayName the option name, as displayed in the configuration file
		 * @param description the option description
		 */
		GameOption(String name, String displayName, String description) {
			this.name = name;
			this.displayName = displayName;
			this.description = description;
		}

		/**
		 * Constructor for boolean options.
		 * @param name the option name
		 * @param displayName the option name, as displayed in the configuration file
		 * @param description the option description
		 * @param value the default boolean value
		 */
		GameOption(String name, String displayName, String description, boolean value) {
			this(name, displayName, description);
			this.bool = value;
			this.type = OptionType.BOOLEAN;
		}

		/**
		 * Constructor for numeric options.
		 * @param name the option name
		 * @param displayName the option name, as displayed in the configuration file
		 * @param description the option description
		 * @param value the default integer value
		 */
		GameOption(String name, String displayName, String description, int value, int min, int max) {
			this(name, displayName, description);
			this.val = value;
			this.min = min;
			this.max = max;
			this.type = OptionType.NUMERIC;
		}

		/**
		 * Returns the option name.
		 * @return the name string
		 */
		public String getName() { return name; }

		/**
		 * Returns the option name, as displayed in the configuration file.
		 * @return the display name string
		 */
		public String getDisplayName() { return displayName; }

		/**
		 * Returns the option description.
		 * @return the description string
		 */
		public String getDescription() { return description; }

		public boolean showRWM() { return false; } // this is probably a shitty way to implement this :)

		/**
		 * Returns the boolean value for the option, if applicable.
		 * @return the boolean value
		 */
		public boolean getBooleanValue() { return bool; }

		/**
		 * Returns the integer value for the option, if applicable.
		 * @return the integer value
		 */
		public int getIntegerValue() { return val; }

		/**
		 * Sets the boolean value for the option.
		 * @param value the new boolean value
		 */
		public void setValue(boolean value) { this.bool = value; }

		/**
		 * Sets the integer value for the option.
		 * @param value the new integer value
		 */
		public void setValue(int value) { this.val = value; }

		/**
		 * Returns the value of the option as a string (via override).
		 * <p>
		 * By default, this returns "{@code val}%" for numeric options,
		 * "Yes" or "No" based on the {@code bool} field for boolean options,
		 * and an empty string otherwise.
		 * @return the value string
		 */
		public String getValueString() {
			if (type == OptionType.NUMERIC)
				return String.format("%d%%", val);
			else if (type == OptionType.BOOLEAN)
				return (bool) ? "Yes" : "No";
			else
				return "";
		}

		/**
		 * Processes a mouse click action (via override).
		 * <p>
		 * By default, this inverts the current {@code bool} field.
		 * @param container the game container
		 */
		public void click(GameContainer container) { bool = !bool; }

		/**
		 * Get a list of values to choose from
		 * @return list with value string or null if no list should be shown
		 */
		public Object[] getListItems() { return null; }

		/**
		 * Fired when an item in the value list has been clicked
		 * @param index the itemindex which has been clicked
		 */
		public void clickListItem(int index) { }

		/**
		 * Processes a mouse drag action (via override).
		 * <p>
		 * By default, only if this is a numeric option, the {@code val} field
		 * will be shifted by {@code d} within the given bounds.
		 * @param container the game container
		 * @param d the dragged distance (modified by multiplier)
		 */
		public void drag(GameContainer container, int d) {
			if (type == OptionType.NUMERIC)
				val = Utils.clamp(val + d, min, max);
		}

		/**
		 * Returns the string to write to the configuration file (via override).
		 * <p>
		 * By default, this returns "{@code val}" for numeric options,
		 * "true" or "false" based on the {@code bool} field for boolean options,
		 * and {@link #getValueString()} otherwise.
		 * @return the string to write
		 */
		public String write() {
			if (type == OptionType.NUMERIC)
				return Integer.toString(val);
			else if (type == OptionType.BOOLEAN)
				return Boolean.toString(bool);
			else
				return getValueString();
		}

		/**
		 * Reads the value of the option from the configuration file (via override).
		 * <p>
		 * By default, this sets {@code val} for numeric options only if the
		 * value is between the min and max bounds, sets {@code bool} for
		 * boolean options, and does nothing otherwise.
		 * @param s the value string read from the configuration file
		 */
		public void read(String s) {
			if (type == OptionType.NUMERIC) {
				int i = Integer.parseInt(s);
				if (i >= min && i <= max)
					val = i;
			} else if (type == OptionType.BOOLEAN)
				bool = Boolean.parseBoolean(s);
		}
	};

	/** Map of option display names to GameOptions. */
	private static HashMap<String, GameOption> optionMap;

	private static String[] resolutions = {
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

	private static int resolutionIdx;

	public static int width;
	public static int height;

	/** The available skin directories. */
	private static String[] skinDirs;

	/** The index in the skinDirs array. */
	private static int skinDirIndex = 0;

	/** The name of the skin. */
	private static String skinName = "Default";

	/** The current skin. */
	private static Skin skin;

	/** Frame limiters. */
	private static final int[] targetFPS = { 60, 120, 240, 1000 };

	/** Index in targetFPS[] array. */
	private static int targetFPSindex = 0;

	/** Screenshot file formats. */
	private static String[] screenshotFormat = { "png", "jpg", "bmp" };

	/** Index in screenshotFormat[] array. */
	private static int screenshotFormatIndex = 0;

	/** Left and right game keys. */
	private static int
		keyLeft  = Keyboard.KEY_NONE,
		keyRight = Keyboard.KEY_NONE;

	// This class should not be instantiated.
	private Options() {}

	public static int getResolutionIdx() {
		return resolutionIdx;
	}

	public static boolean allowLargeResolutions() {
		return GameOption.ALLOW_LARGER_RESOLUTIONS.getBooleanValue();
	}

	/**
	 * Returns the target frame rate.
	 * @return the target FPS
	 */
	public static int getTargetFPS() { return targetFPS[targetFPSindex]; }

	/**
	 * Sets the target frame rate to the next available option, and sends a
	 * bar notification about the action.
	 * @param container the game container
	 */
	public static void setNextFPS(GameContainer container) {
		GameOption.TARGET_FPS.clickListItem((targetFPSindex + 1) % targetFPS.length);
		UI.sendBarNotification(String.format("Frame limiter: %s", GameOption.TARGET_FPS.getValueString()));
	}

	/**
	 * Returns the master volume level.
	 * @return the volume [0, 1]
	 */
	public static float getMasterVolume() { return GameOption.MASTER_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Sets the master volume level (if within valid range).
	 * @param container the game container
	 * @param volume the volume [0, 1]
	 */
	public static void setMasterVolume(GameContainer container, float volume) {
		if (volume >= 0f && volume <= 1f) {
			GameOption.MASTER_VOLUME.setValue((int) (volume * 100f));
			MusicController.setVolume(getMasterVolume() * getMusicVolume());
		}
	}

	/**
	 * Returns the default music volume.
	 * @return the volume [0, 1]
	 */
	public static float getMusicVolume() { return GameOption.MUSIC_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Returns the default sound effect volume.
	 * @return the sound volume [0, 1]
	 */
	public static float getEffectVolume() { return GameOption.EFFECT_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Returns the default hit sound volume.
	 * @return the hit sound volume [0, 1]
	 */
	public static float getHitSoundVolume() { return GameOption.HITSOUND_VOLUME.getIntegerValue() / 100f; }

	/**
	 * Returns the default hit sound volume.
	 * @return the hit sound volume [0, 1]
	 */
	public static float getSampleVolumeOverride() { return GameOption.SAMPLE_VOLUME_OVERRIDE.val / 100f; }

	/**
	 * Returns the music offset time.
	 * @return the offset (in milliseconds)
	 */
	public static int getMusicOffset() { return GameOption.MUSIC_OFFSET.getIntegerValue(); }

	/**
	 * Returns the screenshot file format.
	 * @return the file extension ("png", "jpg", "bmp")
	 */
	public static String getScreenshotFormat() { return screenshotFormat[screenshotFormatIndex]; }

	/**
	 * Sets the container size and makes the window borderless if the container
	 * size is identical to the screen resolution.
	 * <p>
	 * If the configured resolution is larger than the screen size, the smallest
	 * available resolution will be used.
	 * @param app the game container
	 */
	public static void setDisplayMode(Container app) {
		int screenWidth = app.getScreenWidth();
		int screenHeight = app.getScreenHeight();

		resolutions[0] = screenWidth + "x" + screenHeight;
		if (resolutionIdx < 0 || resolutionIdx > resolutions.length) {
			resolutionIdx = 0;
		}
		if (!resolutions[resolutionIdx].matches("^[0-9]+x[0-9]+$")) {
			resolutionIdx = 0;
		}
		String[] res = resolutions[resolutionIdx].split("x");
		width = Integer.parseInt(res[0]);
		height = Integer.parseInt(res[1]);

		// check for larger-than-screen dimensions
		if (!GameOption.ALLOW_LARGER_RESOLUTIONS.getBooleanValue() && (screenWidth < width || screenHeight < height)) {
			width = 800;
			height = 600;
		}

		try {
			app.setDisplayMode(width, height, isFullscreen());
		} catch (SlickException e) {
			ErrorHandler.error("Failed to set display mode.", e, true);
		}

		if (!isFullscreen()) {
			// set borderless window if dimensions match screen size
			boolean borderless = (screenWidth == width && screenHeight == height);
			System.setProperty("org.lwjgl.opengl.Window.undecorated", Boolean.toString(borderless));
		}
	}

	/**
	 * Returns whether or not fullscreen mode is enabled.
	 * @return true if enabled
	 */
	public static boolean isFullscreen() { return GameOption.FULLSCREEN.getBooleanValue(); }

	/**
	 * Returns whether or not the FPS counter display is enabled.
	 * @return true if enabled
	 */
	public static boolean isFPSCounterEnabled() { return GameOption.SHOW_FPS.getBooleanValue(); }

	/**
	 * Returns whether or not hit lighting effects are enabled.
	 * @return true if enabled
	 */
	public static boolean isHitLightingEnabled() { return GameOption.SHOW_HIT_LIGHTING.getBooleanValue(); }

	/**
	 * Returns whether or not combo burst effects are enabled.
	 * @return true if enabled
	 */
	public static boolean isComboBurstEnabled() { return GameOption.SHOW_COMBO_BURSTS.getBooleanValue(); }

	/**
	 * Returns the port number to bind to.
	 * @return the port
	 */
	public static int getPort() { return port; }

	public static boolean noSingleInstance() { return noSingleInstance; }

	/**
	 * Returns the cursor scale.
	 * @return the scale [0.5, 2]
	 */
	public static float getCursorScale() { return GameOption.CURSOR_SIZE.getIntegerValue() / 100f; }

	/**
	 * Returns whether or not the new cursor type is enabled.
	 * @return true if enabled
	 */
	public static boolean isNewCursorEnabled() { return GameOption.NEW_CURSOR.getBooleanValue(); }

	/**
	 * Returns whether or not the main menu background should be the current track image.
	 * @return true if enabled
	 */
	public static boolean isDynamicBackgroundEnabled() { return GameOption.DYNAMIC_BACKGROUND.getBooleanValue(); }

	/**
	 * Returns whether or not to show perfect hit result bursts.
	 * @return true if enabled
	 */
	public static boolean isPerfectHitBurstEnabled() { return GameOption.SHOW_PERFECT_HIT.getBooleanValue(); }

	/**
	 * Returns whether or not to show follow points.
	 * @return true if enabled
	 */
	public static boolean isFollowPointEnabled() { return GameOption.SHOW_FOLLOW_POINTS.getBooleanValue(); }

	/**
	 * Returns the background dim level.
	 * @return the alpha level [0, 1]
	 */
	public static float getBackgroundDim() { return (100 - GameOption.BACKGROUND_DIM.getIntegerValue()) / 100f; }

	/**
	 * Returns whether or not to override the song background with the default playfield background.
	 * @return true if forced
	 */
	public static boolean isDefaultPlayfieldForced() { return GameOption.FORCE_DEFAULT_PLAYFIELD.getBooleanValue(); }

	/**
	 * Returns whether or not beatmap skins are ignored.
	 * @return true if ignored
	 */
	public static boolean isBeatmapSkinIgnored() { return GameOption.IGNORE_BEATMAP_SKINS.getBooleanValue(); }

	/**
	 * Returns whether or not sliders should snake in or just appear fully at once.
	 * @return true if sliders should snake in
	 */
	public static boolean isSliderSnaking() { return GameOption.SNAKING_SLIDERS.getBooleanValue(); }

	public static boolean isFallbackSliders() { return GameOption.FALLBACK_SLIDERS.getBooleanValue(); }

	/**
	 * Returns the fixed circle size override, if any.
	 * @return the CS value (0, 10], 0f if disabled
	 */
	public static float getFixedCS() { return GameOption.FIXED_CS.getIntegerValue() / 10f; }

	/**
	 * Returns the fixed HP drain rate override, if any.
	 * @return the HP value (0, 10], 0f if disabled
	 */
	public static float getFixedHP() { return GameOption.FIXED_HP.getIntegerValue() / 10f; }

	/**
	 * Returns the fixed approach rate override, if any.
	 * @return the AR value (0, 10], 0f if disabled
	 */
	public static float getFixedAR() { return GameOption.FIXED_AR.getIntegerValue() / 10f; }

	/**
	 * Returns the fixed overall difficulty override, if any.
	 * @return the OD value (0, 10], 0f if disabled
	 */
	public static float getFixedOD() { return GameOption.FIXED_OD.getIntegerValue() / 10f; }

	/**
	 * Returns whether or not to render loading text in the splash screen.
	 * @return true if enabled
	 */
	public static boolean isLoadVerbose() { return GameOption.LOAD_VERBOSE.getBooleanValue(); }

	/**
	 * Returns the track checkpoint time.
	 * @return the checkpoint time (in ms)
	 */
	public static int getCheckpoint() { return GameOption.CHECKPOINT.getIntegerValue() * 1000; }

	/**
	 * Returns whether or not all sound effects are disabled.
	 * @return true if disabled
	 */
	public static boolean isSoundDisabled() { return GameOption.DISABLE_SOUNDS.getBooleanValue(); }

	/**
	 * Returns whether or not to use non-English metadata where available.
	 * @return true if Unicode preferred
	 */
	public static boolean useUnicodeMetadata() { return GameOption.SHOW_UNICODE.getBooleanValue(); }

	/**
	 * Returns whether or not to play the theme song.
	 * @return true if enabled
	 */
	public static boolean isThemeSongEnabled() { return GameOption.ENABLE_THEME_SONG.getBooleanValue(); }

	/**
	 * Returns whether or not replay seeking is enabled.
	 * @return true if enabled
	 */
	public static boolean isReplaySeekingEnabled() { return GameOption.REPLAY_SEEKING.getBooleanValue(); }

	/**
	 * Returns whether or not automatic checking for updates is disabled.
	 * @return true if disabled
	 */
	public static boolean isUpdaterDisabled() { return GameOption.DISABLE_UPDATER.getBooleanValue(); }

	/**
	 * Returns whether or not the beatmap watch service is enabled.
	 * @return true if enabled
	 */
	public static boolean isWatchServiceEnabled() { return GameOption.ENABLE_WATCH_SERVICE.getBooleanValue(); }

	/**
	 * Sets the track checkpoint time, if within bounds.
	 * @param time the track position (in ms)
	 * @return true if within bounds
	 */
	public static boolean setCheckpoint(int time) {
		if (time >= 0 && time < 3600) {
			GameOption.CHECKPOINT.setValue(time);
			return true;
		}
		return false;
	}

	/**
	 * Returns whether or not to show the hit error bar.
	 * @return true if enabled
	 */
	public static boolean isHitErrorBarEnabled() { return GameOption.SHOW_HIT_ERROR_BAR.getBooleanValue(); }

	public static int getMapStartDelay() { return GameOption.MAP_START_DELAY.getIntegerValue() * 100; }
	public static int getMapEndDelay() { return GameOption.MAP_END_DELAY.getIntegerValue() * 100; }
	public static int getEpilepsyWarningLength() { return GameOption.EPILEPSY_WARNING.getIntegerValue() * 100; }

	/**
	 * Returns whether or not to load HD (@2x) images.
	 * @return true if HD images are enabled, false if only SD images should be loaded
	 */
	public static boolean loadHDImages() { return GameOption.LOAD_HD_IMAGES.getBooleanValue(); }

	/**
	 * Returns whether or not the mouse wheel is disabled during gameplay.
	 * @return true if disabled
	 */
	public static boolean isMouseWheelDisabled() { return GameOption.DISABLE_MOUSE_WHEEL.getBooleanValue(); }

	/**
	 * Returns whether or not the mouse buttons are disabled during gameplay.
	 * @return true if disabled
	 */
	public static boolean isMouseDisabled() { return GameOption.DISABLE_MOUSE_BUTTONS.getBooleanValue(); }

	/**
	 * Toggles the mouse button enabled/disabled state during gameplay and
	 * sends a bar notification about the action.
	 */
	public static void toggleMouseDisabled() {
		GameOption.DISABLE_MOUSE_BUTTONS.click(null);
		UI.sendBarNotification((GameOption.DISABLE_MOUSE_BUTTONS.getBooleanValue()) ?
			"Mouse buttons are disabled." : "Mouse buttons are enabled.");
	}

	/**
	 * Returns whether or not the cursor sprite should be hidden.
	 * @return true if disabled
	 */
	public static boolean isCursorDisabled() { return GameOption.DISABLE_CURSOR.getBooleanValue(); }

	/**
	 * Returns the left game key.
	 * @return the left key code
	 */
	public static int getGameKeyLeft() {
		if (keyLeft == Keyboard.KEY_NONE)
			setGameKeyLeft(Input.KEY_Z);
		return keyLeft;
	}

	/**
	 * Returns the right game key.
	 * @return the right key code
	 */
	public static int getGameKeyRight() {
		if (keyRight == Keyboard.KEY_NONE)
			setGameKeyRight(Input.KEY_X);
		return keyRight;
	}

	/**
	 * Sets the left game key.
	 * This will not be set to the same key as the right game key, nor to any
	 * reserved keys (see {@link #isValidGameKey(int)}).
	 * @param key the keyboard key
	 * @return {@code true} if the key was set, {@code false} if it was rejected
	 */
	public static boolean setGameKeyLeft(int key) {
		if ((key == keyRight && key != Keyboard.KEY_NONE) || !isValidGameKey(key))
			return false;
		keyLeft = key;
		return true;
	}

	/**
	 * Sets the right game key.
	 * This will not be set to the same key as the left game key, nor to any
	 * reserved keys (see {@link #isValidGameKey(int)}).
	 * @param key the keyboard key
	 * @return {@code true} if the key was set, {@code false} if it was rejected
	 */
	public static boolean setGameKeyRight(int key) {
		if ((key == keyLeft && key != Keyboard.KEY_NONE) || !isValidGameKey(key))
			return false;
		keyRight = key;
		return true;
	}

	/**
	 * Checks if the given key is a valid game key.
	 * @param key the keyboard key
	 * @return {@code true} if valid, {@code false} otherwise
	 */
	private static boolean isValidGameKey(int key) {
		return (key != Keyboard.KEY_ESCAPE && key != Keyboard.KEY_SPACE &&
		        key != Keyboard.KEY_UP && key != Keyboard.KEY_DOWN &&
		        key != Keyboard.KEY_F7 && key != Keyboard.KEY_F10 && key != Keyboard.KEY_F12);
	}

	/**
	 * Returns the beatmap directory.
	 * If invalid, this will attempt to search for the directory,
	 * and if nothing found, will create one.
	 * @return the beatmap directory
	 */
	public static File getBeatmapDir() {
		if (beatmapDir != null && beatmapDir.isDirectory())
			return beatmapDir;

		// use osu! installation directory, if found
		File osuDir = getOsuInstallationDirectory();
		if (osuDir != null) {
			beatmapDir = new File(osuDir, BEATMAP_DIR.getName());
			if (beatmapDir.isDirectory())
				return beatmapDir;
		}

		// use default directory
		beatmapDir = BEATMAP_DIR;
		if (!beatmapDir.isDirectory() && !beatmapDir.mkdir())
			ErrorHandler.error(String.format("Failed to create beatmap directory at '%s'.", beatmapDir.getAbsolutePath()), null, false);
		return beatmapDir;
	}

	/**
	 * Returns the OSZ archive directory.
	 * If invalid, this will create and return a "SongPacks" directory.
	 * @return the OSZ archive directory
	 */
	public static File getOSZDir() {
		if (oszDir != null && oszDir.isDirectory())
			return oszDir;

		oszDir = new File(DATA_DIR, "SongPacks/");
		if (!oszDir.isDirectory() && !oszDir.mkdir())
			ErrorHandler.error(String.format("Failed to create song packs directory at '%s'.", oszDir.getAbsolutePath()), null, false);
		return oszDir;
	}

	/**
	 * Returns the replay import directory.
	 * If invalid, this will create and return a "ReplayImport" directory.
	 * @return the replay import directory
	 */
	public static File getReplayImportDir() {
		if (replayImportDir != null && replayImportDir.isDirectory())
			return replayImportDir;

		replayImportDir = new File(DATA_DIR, "ReplayImport/");
		if (!replayImportDir.isDirectory() && !replayImportDir.mkdir())
			ErrorHandler.error(String.format("Failed to create replay import directory at '%s'.", replayImportDir.getAbsolutePath()), null, false);
		return replayImportDir;
	}

	/**
	 * Returns the screenshot directory.
	 * If invalid, this will return a "Screenshot" directory.
	 * @return the screenshot directory
	 */
	public static File getScreenshotDir() {
		if (screenshotDir != null && screenshotDir.isDirectory())
			return screenshotDir;

		screenshotDir = new File(DATA_DIR, "Screenshots/");
		return screenshotDir;
	}

	/**
	 * Returns the replay directory.
	 * If invalid, this will return a "Replay" directory.
	 * @return the replay directory
	 */
	public static File getReplayDir() {
		if (replayDir != null && replayDir.isDirectory())
			return replayDir;

		replayDir = new File(DATA_DIR, "Replays/");
		return replayDir;
	}

	/**
	 * Returns the current skin directory.
	 * If invalid, this will create a "Skins" folder in the root directory.
	 * @return the skin directory
	 */
	public static File getSkinRootDir() {
		if (skinRootDir != null && skinRootDir.isDirectory())
			return skinRootDir;

		// use osu! installation directory, if found
		File osuDir = getOsuInstallationDirectory();
		if (osuDir != null) {
			skinRootDir = new File(osuDir, SKIN_ROOT_DIR.getName());
			if (skinRootDir.isDirectory())
				return skinRootDir;
		}

		// use default directory
		skinRootDir = SKIN_ROOT_DIR;
		if (!skinRootDir.isDirectory() && !skinRootDir.mkdir())
			ErrorHandler.error(String.format("Failed to create skins directory at '%s'.", skinRootDir.getAbsolutePath()), null, false);
		return skinRootDir;
	}

	/**
	 * Loads the skin given by the current skin directory.
	 * If the directory is invalid, the default skin will be loaded.
	 */
	public static void loadSkin() {
		File skinDir = getSkinDir();
		if (skinDir == null)  // invalid skin name
			skinName = Skin.DEFAULT_SKIN_NAME;

		// create available skins list
		File[] dirs = SkinLoader.getSkinDirectories(getSkinRootDir());
		skinDirs = new String[dirs.length + 1];
		skinDirs[0] = Skin.DEFAULT_SKIN_NAME;
		for (int i = 0; i < dirs.length; i++)
			skinDirs[i + 1] = dirs[i].getName();

		// set skin and modify resource locations
		ResourceLoader.removeAllResourceLocations();
		if (skinDir == null)
			skin = new Skin(null);
		else {
			// set skin index
			for (int i = 1; i < skinDirs.length; i++) {
				if (skinDirs[i].equals(skinName)) {
					skinDirIndex = i;
					break;
				}
			}

			// load the skin
			skin = SkinLoader.loadSkin(skinDir);
			ResourceLoader.addResourceLocation(new FileSystemLocation(skinDir));
		}
		ResourceLoader.addResourceLocation(new ClasspathLocation());
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File(".")));
		ResourceLoader.addResourceLocation(new FileSystemLocation(new File("./res/")));
	}

	/**
	 * Returns the current skin.
	 * @return the skin, or null if no skin is loaded (see {@link #loadSkin()})
	 */
	public static Skin getSkin() { return skin; }

	/**
	 * Returns the current skin directory.
	 * <p>
	 * NOTE: This directory will differ from that of the currently loaded skin
	 * if {@link #loadSkin()} has not been called after a directory change.
	 * Use {@link Skin#getDirectory()} to get the directory of the currently
	 * loaded skin.
	 * @return the skin directory, or null for the default skin
	 */
	public static File getSkinDir() {
		File root = getSkinRootDir();
		File dir = new File(root, skinName);
		return (dir.isDirectory()) ? dir : null;
	}

	/**
	 * Returns a dummy Beatmap containing the theme song.
	 * @return the theme song beatmap
	 */
	public static Beatmap getThemeBeatmap() {
		String[] tokens = themeString.split(",");
		if (tokens.length != 4) {
			ErrorHandler.error("Theme song string is malformed.", null, false);
			return null;
		}

		Beatmap beatmap = new Beatmap(null);
		beatmap.audioFilename = new File(tokens[0]);
		beatmap.title = tokens[1];
		beatmap.artist = tokens[2];
		beatmap.timingPoints = new ArrayList<>(1);
		beatmap.timingPoints.add(new TimingPoint("-44,631.578947368421,4,1,0,100,1,0"));
		try {
			beatmap.endTime = Integer.parseInt(tokens[3]);
		} catch (NumberFormatException e) {
			ErrorHandler.error("Theme song length is not a valid integer", e, false);
			return null;
		}

		return beatmap;
	}

	/**
	 * Reads user options from the options file, if it exists.
	 */
	public static void parseOptions() {
		// if no config file, use default settings
		if (!OPTIONS_FILE.isFile()) {
			saveOptions();
			return;
		}

		// create option map
		if (optionMap == null) {
			optionMap = new HashMap<String, GameOption>();
			for (GameOption option : GameOption.values())
				optionMap.put(option.getDisplayName(), option);
		}

		// read file
		try (BufferedReader in = new BufferedReader(new FileReader(OPTIONS_FILE))) {
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() < 2 || line.charAt(0) == '#')
					continue;
				int index = line.indexOf('=');
				if (index == -1)
					continue;

				// read option
				String name = line.substring(0, index).trim();
				GameOption option = optionMap.get(name);
				if (option != null) {
					try {
						String value = line.substring(index + 1).trim();
						option.read(value);
					} catch (NumberFormatException e) {
						Log.warn(String.format("Format error in options file for line: '%s'.", line), e);
					}
				}
			}
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to read file '%s'.", OPTIONS_FILE.getAbsolutePath()), e, false);
		}
	}

	/**
	 * (Over)writes user options to a file.
	 */
	public static void saveOptions() {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(OPTIONS_FILE), "utf-8"))) {
			// header
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
			String date = dateFormat.format(new Date());
			writer.write("# opsu! configuration");
			writer.newLine();
			writer.write("# last updated on ");
			writer.write(date);
			writer.newLine();
			writer.newLine();

			// options
			for (GameOption option : GameOption.values()) {
				writer.write(option.getDisplayName());
				writer.write(" = ");
				writer.write(option.write());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			ErrorHandler.error(String.format("Failed to write to file '%s'.", OPTIONS_FILE.getAbsolutePath()), e, false);
		}
	}
}
