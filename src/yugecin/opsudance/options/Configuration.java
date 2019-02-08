// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.options;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.TimingPoint;
import yugecin.opsudance.core.Entrypoint;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.util.Log;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static itdelatrisu.opsu.ui.Colors.*;
import static yugecin.opsudance.options.Options.*;
import static yugecin.opsudance.core.InstanceContainer.*;

public class Configuration
{
	public final File BEATMAP_DIR;
	public final File SKIN_ROOT_DIR;
	public final File BEATMAP_DB;
	public final File SCORE_DB;
	public final File NATIVE_DIR;
	public final File TEMP_DIR;

	public final File OPTIONS_FILE;

	public final File osuInstallationDirectory;

	public File beatmapDir;
	public File oszDir;
	public File screenshotDir;
	public File replayDir;
	public File replayImportDir;
	public File skinRootDir;

	public Configuration()
	{
		BEATMAP_DIR = new File(Entrypoint.workingdir, "Songs/");
		SKIN_ROOT_DIR = new File(Entrypoint.workingdir, "Skins/");
		BEATMAP_DB = new File(Entrypoint.workingdir, ".opsu.db");
		SCORE_DB = new File(Entrypoint.workingdir, ".opsu_scores.db");
		NATIVE_DIR = new File(Entrypoint.workingdir, "Natives/");
		TEMP_DIR = new File(Entrypoint.workingdir, "Temp/");

		OPTIONS_FILE = new File(Entrypoint.workingdir, ".opsu.cfg");

		osuInstallationDirectory = loadOsuInstallationDirectory();

		themeBeatmap = new Beatmap(null);
		themeBeatmap.audioFilename = new File("theme.ogg");
		themeBeatmap.title = "On the Bach";
		themeBeatmap.artist = "Jingle Punks";
		themeBeatmap.endTime = 66000;
		themeBeatmap.timingPoints = new ArrayList<>(1);
		themeBeatmap.timingPoints.add(new TimingPoint("-44,631.57894736842,4,1,0,100,1,0"));
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
		replayImportDir = loadDirectory(replayImportDir, new File(Entrypoint.workingdir, "ReplayImport"), "replay import");
		oszDir = loadDirectory(oszDir, new File(Entrypoint.workingdir, "SongPacks"), "song packs");
		screenshotDir = loadDirectory(screenshotDir, new File(Entrypoint.workingdir, "Screenshots"), "screenshots");
		replayDir = loadDirectory(replayDir, new File(Entrypoint.workingdir, "Replays"), "replays");
		beatmapDir = loadOsuDirectory(beatmapDir, BEATMAP_DIR, "beatmap");
		skinRootDir = loadOsuDirectory(skinRootDir, SKIN_ROOT_DIR, "skin root");
	}

	private File loadDirectory(File dir, File defaultDir, String kind) {
		if (dir != null && dir.exists() && dir.isDirectory()) {
			return dir;
		}
		if (!defaultDir.isDirectory() && !defaultDir.mkdir()) {
			String msg = String.format("Failed to create %s directory at '%s'.", kind, defaultDir.getAbsolutePath());
			bubNotifs.send(BUB_RED, msg);
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

	/**
	 * @author http://wiki.lwjgl.org/index.php?title=Taking_Screen_Shots
	 */
	public void takeScreenShot() {
		// TODO: get a decent place for this
		// create the screenshot directory
		if (!screenshotDir.isDirectory() && !screenshotDir.mkdir()) {
			bubNotifs.sendf(
				BUB_RED,
				"Failed to create screenshot directory at '%s'.",
				screenshotDir.getAbsolutePath()
			);
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
					bubNotifs.send(BUB_PURPLE, "Created " + fileName);
				} catch (Exception e) {
					Log.error("Could not take screenshot", e);
					bubNotifs.send(
						BUB_PURPLE,
						"Failed to take a screenshot. See log file for details"
					);
				}
			}
		}.start();
	}
}
