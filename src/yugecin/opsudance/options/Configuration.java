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

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.TimingPoint;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import yugecin.opsudance.core.errorhandling.ErrorHandler;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.inject.Inject;
import yugecin.opsudance.events.BubbleNotificationEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static yugecin.opsudance.options.Options.*;

public class Configuration {

	public final boolean USE_XDG;
	public final File CONFIG_DIR;
	public final File DATA_DIR;
	public final File CACHE_DIR;
	public final File BEATMAP_DIR;
	public final File SKIN_ROOT_DIR;
	public final File BEATMAP_DB;
	public final File SCORE_DB;
	public final File NATIVE_DIR;
	public final File TEMP_DIR;

	public final File LOG_FILE;
	public final File OPTIONS_FILE;

	public final String FONT_NAME;
	public final String VERSION_FILE;
	public final URI REPOSITORY_URI;
	public final URI DANCE_REPOSITORY_URI;
	public final String ISSUES_URL;
	public final String VERSION_REMOTE;

	public final File osuInstallationDirectory;

	public final Beatmap themeBeatmap;

	public File beatmapDir;
	public File oszDir;
	public File screenshotDir;
	public File replayDir;
	public File replayImportDir;
	public File skinRootDir;

	@Inject
	public Configuration() {
		USE_XDG = areXDGDirectoriesEnabled();

		CONFIG_DIR = getXDGBaseDir("XDG_CONFIG_HOME", ".config");
		DATA_DIR = getXDGBaseDir("XDG_DATA_HOME", ".local/share");
		CACHE_DIR = getXDGBaseDir("XDG_CACHE_HOME", ".cache");

		BEATMAP_DIR = new File(DATA_DIR, "Songs/");
		SKIN_ROOT_DIR = new File(DATA_DIR, "Skins/");
		BEATMAP_DB = new File(DATA_DIR, ".opsu.db");
		SCORE_DB = new File(DATA_DIR, ".opsu_scores.db");
		NATIVE_DIR = new File(CACHE_DIR, "Natives/");
		TEMP_DIR = new File(CACHE_DIR, "Temp/");

		LOG_FILE = new File(CONFIG_DIR, ".opsu.log");
		OPTIONS_FILE = new File(CONFIG_DIR, ".opsu.cfg");

		FONT_NAME = "DroidSansFallback.ttf";
		VERSION_FILE = "version";
		REPOSITORY_URI = URI.create("https://github.com/itdelatrisu/opsu");
		DANCE_REPOSITORY_URI = URI.create("https://github.com/yugecin/opsu-dance");
		ISSUES_URL = "https://github.com/yugecin/opsu-dance/issues/new?title=%s&body=%s";
		VERSION_REMOTE = "https://raw.githubusercontent.com/yugecin/opsu-dance/master/version";

		osuInstallationDirectory = loadOsuInstallationDirectory();

		themeBeatmap = createThemeBeatmap();
	}

	private Beatmap createThemeBeatmap() {
		try {
			String[] tokens = {"theme.mp3", "Rainbows", "Kevin MacLeod", "219350"};
			Beatmap beatmap = new Beatmap(null);
			beatmap.audioFilename = new File(tokens[0]);
			beatmap.title = tokens[1];
			beatmap.artist = tokens[2];
			beatmap.endTime = Integer.parseInt(tokens[3]);
			beatmap.timingPoints = new ArrayList<>(1);
			beatmap.timingPoints.add(new TimingPoint("1080,545.454545454545,4,1,0,100,0,0"));
			return beatmap;
		} catch (Exception e) {
			return null;
		}

	}

	private File loadOsuInstallationDirectory() {
		if (!System.getProperty("os.name").startsWith("Win")) {
			return null;
		}

		final WinReg.HKEY rootKey = WinReg.HKEY_CLASSES_ROOT;
		final String regKey = "osu\\DefaultIcon";
		final String regValue = null; // default value
		final String regPathPattern = "\"(.+)\\\\[^\\/]+\\.exe\"";

		String value;
		try {
			value = Advapi32Util.registryGetStringValue(rootKey, regKey, regValue);
		} catch (Win32Exception ignored) {
			return null;
		}
		Pattern pattern = Pattern.compile(regPathPattern);
		Matcher m = pattern.matcher(value);
		if (!m.find()) {
			return null;
		}
		File dir = new File(m.group(1));
		if (dir.isDirectory()) {
			return dir;
		}
		return null;
	}

	public void loadDirectories() {
		replayImportDir = loadDirectory(replayImportDir, new File(DATA_DIR, "ReplayImport"), "replay import");
		oszDir = loadDirectory(oszDir, new File(DATA_DIR, "SongPacks"), "song packs");
		screenshotDir = loadDirectory(screenshotDir, new File(DATA_DIR, "Screenshots"), "screenshots");
		replayDir = loadDirectory(replayDir, new File(DATA_DIR, "Replays"), "replays");
		beatmapDir = loadOsuDirectory(beatmapDir, BEATMAP_DIR, "beatmap");
		skinRootDir = loadOsuDirectory(skinRootDir, SKIN_ROOT_DIR, "skin root");
	}

	private File loadDirectory(File dir, File defaultDir, String kind) {
		if (dir.exists() && dir.isDirectory()) {
			return dir;
		}
		if (!defaultDir.isDirectory() && !defaultDir.mkdir()) {
			String msg = String.format("Failed to create %s directory at '%s'.", kind, defaultDir.getAbsolutePath());
			EventBus.post(new BubbleNotificationEvent(msg, BubbleNotificationEvent.COMMONCOLOR_RED));
		}
		return defaultDir;
	}

	private File loadOsuDirectory(File dir, File defaultDir, String kind) {
		if (dir != null && dir.isDirectory()) {
			return dir;
		}

		if (osuInstallationDirectory != null) {
			dir = new File(osuInstallationDirectory, defaultDir.getName());
			if (dir.isDirectory()) {
				return dir;
			}
		}

		return loadDirectory(dir, defaultDir, kind);
	}

	private boolean areXDGDirectoriesEnabled() {
		JarFile jarFile = Utils.getJarFile();
		if (jarFile == null) {
			return false;
		}
		try {
			Manifest manifest = jarFile.getManifest();
			if (manifest == null) {
				return false;
			}
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
	private File getXDGBaseDir(String env, String fallback) {
		File workingdir;
		if (Utils.isJarRunning()) {
			workingdir = Utils.getRunningDirectory().getParentFile();
		} else {
			workingdir = Paths.get(".").toAbsolutePath().normalize().toFile();
		}

		if (!USE_XDG) {
			return workingdir;
		}

		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("nix") == -1 && OS.indexOf("nux") == -1 && OS.indexOf("aix") == -1){
			return workingdir;
		}

		String rootPath = System.getenv(env);
		if (rootPath == null) {
			String home = System.getProperty("user.home");
			if (home == null) {
				return new File("./");
			}
			rootPath = String.format("%s/%s", home, fallback);
		}
		File dir = new File(rootPath, "opsu");
		if (!dir.isDirectory() && !dir.mkdir()) {
			ErrorHandler.error(String.format("Failed to create configuration folder at '%s/opsu'.", rootPath), new Exception("empty")).preventReport().show();
		}
		return dir;
	}

	/**
	 * @author http://wiki.lwjgl.org/index.php?title=Taking_Screen_Shots
	 */
	public void takeScreenShot() {
		// TODO: get a decent place for this
		// create the screenshot directory
		if (!screenshotDir.isDirectory() && !screenshotDir.mkdir()) {
			EventBus.post(new BubbleNotificationEvent(String.format("Failed to create screenshot directory at '%s'.", screenshotDir.getAbsolutePath()), BubbleNotificationEvent.COMMONCOLOR_RED));
			return;
		}

		// create file name
		SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmss");
		final String fileName = String.format("screenshot_%s.%s", date.format(new Date()), OPTION_SCREENSHOT_FORMAT.getValueString().toLowerCase());
		final File file = new File(screenshotDir, fileName);

		SoundController.playSound(SoundEffect.SHUTTER);

		// copy the screen to file
		final int width = Display.getWidth();
		final int height = Display.getHeight();
		final int bpp = 3;  // assuming a 32-bit display with a byte each for red, green, blue, and alpha
		final ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
		GL11.glReadBuffer(GL11.GL_FRONT);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
		new Thread() {
			@Override
			public void run() {
				try {
					BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					for (int x = 0; x < width; x++) {
						for (int y = 0; y < height; y++) {
							int i = (x + (width * y)) * bpp;
							int r = buffer.get(i) & 0xFF;
							int g = buffer.get(i + 1) & 0xFF;
							int b = buffer.get(i + 2) & 0xFF;
							image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
						}
					}
					ImageIO.write(image, OPTION_SCREENSHOT_FORMAT.getValueString().toLowerCase(), file);
					EventBus.post(new BubbleNotificationEvent("Created " + fileName, BubbleNotificationEvent.COMMONCOLOR_PURPLE));
				} catch (Exception e) {
					ErrorHandler.error("Failed to take a screenshot.", e).show();
				}
			}
		}.start();
	}

}
