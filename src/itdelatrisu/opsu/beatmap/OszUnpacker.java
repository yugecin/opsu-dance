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

package itdelatrisu.opsu.beatmap;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.inject.Inject;
import yugecin.opsudance.events.BubbleNotificationEvent;
import yugecin.opsudance.options.Configuration;

/**
 * Unpacker for OSZ (ZIP) archives.
 */
public class OszUnpacker {

	@Inject
	private Configuration config;

	/** The index of the current file being unpacked. */
	private int fileIndex = -1;

	/** The total number of files to unpack. */
	private File[] files;

	@Inject
	public OszUnpacker() {
	}

	/**
	 * Invokes the unpacker for each OSZ archive in a root directory.
	 * @param root the root directory
	 * @param dest the destination directory
	 * @return an array containing the new (unpacked) directories, or null
	 *         if no OSZs found
	 */
	public File[] unpackAll() {
		List<File> dirs = new ArrayList<File>();

		// find all OSZ files
		files = config.oszDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".osz");
			}
		});
		if (files == null || files.length < 1) {
			files = null;
			return new File[0];
		}

		// unpack OSZs
		BeatmapWatchService ws = BeatmapWatchService.get();
		if (ws != null)
			ws.pause();
		for (File file : files) {
			fileIndex++;
			String dirName = file.getName().substring(0, file.getName().lastIndexOf('.'));
			File songDir = new File(config.beatmapDir, dirName);
			if (!songDir.isDirectory()) {
				songDir.mkdir();
				unzip(file, songDir);
				file.delete();  // delete the OSZ when finished
				dirs.add(songDir);
			}
		}
		if (ws != null)
			ws.resume();

		fileIndex = -1;
		files = null;
		return dirs.toArray(new File[dirs.size()]);
	}

	/**
	 * Extracts the contents of a ZIP archive to a destination.
	 * @param file the ZIP archive
	 * @param dest the destination directory
	 */
	private void unzip(File file, File dest) {
		try {
			ZipFile zipFile = new ZipFile(file);
			zipFile.extractAll(dest.getAbsolutePath());
		} catch (ZipException e) {
			String err = String.format("Failed to unzip file %s to dest %s.", file.getAbsolutePath(), dest.getAbsolutePath());
			Log.error(err, e);
			EventBus.post(new BubbleNotificationEvent(err, BubbleNotificationEvent.COMMONCOLOR_RED));
		}
	}

	/**
	 * Returns the name of the current file being unpacked, or null if none.
	 */
	public String getCurrentFileName() {
		if (files == null || fileIndex == -1)
			return null;

		return files[fileIndex].getName();
	}

	/**
	 * Returns the progress of file unpacking, or -1 if not unpacking.
	 * @return the completion percent [0, 100] or -1
	 */
	public int getUnpackerProgress() {
		if (files == null || fileIndex == -1)
			return -1;

		return (fileIndex + 1) * 100 / files.length;
	}

}