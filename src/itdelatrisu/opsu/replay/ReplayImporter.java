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

package itdelatrisu.opsu.replay;

import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.db.ScoreDB;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.newdawn.slick.util.Log;

import static itdelatrisu.opsu.ui.Colors.*;
import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Importer for replay files.
 */
public class ReplayImporter {

	/** The subdirectory (within the replay import directory) to move replays that could not be imported. */
	private final String FAILED_IMPORT_DIR = "failed";

	/** The index of the current file being imported. */
	private int fileIndex = -1;

	/** The total number of replays to import. */
	private File[] files;

	/**
	 * Invokes the importer for each OSR file in the replay import dir, adding the replay
	 * to the score database and moving the file into the replay directory.
	 */
	public void importAll() {
		// find all OSR files
		files = config.replayImportDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".osr");
			}
		});
		if (files == null || files.length < 1) {
			files = null;
			return;
		}

		// get replay directory
		if (!config.replayDir.isDirectory() && !config.replayDir.mkdir()) {
			String err = String.format("Failed to create replay directory '%s'.", config.replayDir.getAbsolutePath());
			Log.error(err);
			bubNotifs.send(BUB_RED, err);
			return;
		}

		// import OSRs
		for (File file : files) {
			fileIndex++;
			Replay r = new Replay(file);
			try {
				r.loadHeader();
			} catch (IOException e) {
				moveToFailedDirectory(file);
				String err = String.format("Failed to import replay '%s'. The replay file could not be parsed.", file.getName());
				Log.error(err, e);
				bubNotifs.send(BUB_RED, err);
				continue;
			}
			Beatmap beatmap = beatmapList.getBeatmapFromHash(r.beatmapHash);
			if (beatmap != null) {
				// add score to database
				ScoreDB.addScore(r.getScoreData(beatmap));

				// move to replay directory
				File moveToFile = new File(config.replayDir, String.format("%s.osr", r.getReplayFilename()));
				try {
					Files.move(file.toPath(), moveToFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					Log.warn(String.format("Failed to move replay '%s' to the replay directory '%s'.", file, config.replayDir), e);
				}
			} else {
				moveToFailedDirectory(file);
				String err = String.format("Failed to import replay '%s'. The associated beatmap could not be found.", file.getName());
				Log.error(err);
				bubNotifs.send(BUB_RED, err);
			}
		}

		fileIndex = -1;
		files = null;
	}

	/**
	 * Moves a replay file into the failed import directory.
	 * @param file the file to move
	 */
	private void moveToFailedDirectory(File file) {
		File dir = new File(config.replayImportDir, FAILED_IMPORT_DIR);
		dir.mkdir();
		File moveToFile = new File(dir, file.getName());
		try {
			Files.move(file.toPath(), moveToFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Log.warn(String.format("Failed to move replay '%s' to the failed import directory '%s'.", file, dir), e);
		}
	}

	/**
	 * Returns the name of the current file being imported, or null if none.
	 */
	public String getCurrentFileName() {
		if (files == null || fileIndex == -1)
			return null;

		return files[fileIndex].getName();
	}

	/**
	 * Returns the progress of replay importing, or -1 if not importing.
	 * @return the completion percent [0, 100] or -1
	 */
	public int getLoadingProgress() {
		if (files == null || fileIndex == -1)
			return -1;

		return (fileIndex + 1) * 100 / files.length;
	}

}
