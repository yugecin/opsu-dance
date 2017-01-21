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
package yugecin.opsudance;

import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.BeatmapWatchService;
import itdelatrisu.opsu.db.DBController;
import itdelatrisu.opsu.downloads.DownloadList;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.states.Splash;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.errorhandling.ErrorHandler;
import yugecin.opsudance.states.EmptyState;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import static yugecin.opsudance.core.Entrypoint.sout;

/*
 * loosely based on itdelatrisu.opsu.Opsu
 */
public class OpsuDance {

	private final DisplayContainer container;

	private ServerSocket singleInstanceSocket;

	public OpsuDance(DisplayContainer container) {
		this.container = container;
	}

	public void start(String[] args) {
		try {
			sout("initialized");

			checkRunningDirectory();
			Options.parseOptions();
			ensureSingleInstance();
			sout("prechecks done and options parsed");

			initDatabase();
			initUpdater(args);
			sout("database & updater initialized");

			//container.init(EmptyState.class);
			container.init(Splash.class);
		} catch (Exception e) {
			errorAndExit("startup failure", e);
		}

		while (rungame());
		container.teardownAL();

		Options.saveOptions();
		closeSingleInstanceSocket();
		DBController.closeConnections();
		DownloadList.get().cancelAllDownloads();
		Utils.deleteDirectory(Options.TEMP_DIR);
		if (!Options.isWatchServiceEnabled()) {
			BeatmapWatchService.destroy();
		}
	}

	private boolean rungame() {
		try {
			container.setup();
			container.resume();
		} catch (Exception e) {
			ErrorHandler.error("could not initialize GL", e).allowTerminate().preventContinue().show();
			return false;
		}
		Exception caughtException = null;
		try {
			container.run();
		} catch (Exception e) {
			caughtException = e;
		}
		container.teardown();
		container.pause();
		return caughtException != null && ErrorHandler.error("update/render error", caughtException).allowTerminate().show().shouldIgnoreAndContinue();
	}

	private void initDatabase() {
		try {
			DBController.init();
		} catch (UnsatisfiedLinkError e) {
			errorAndExit("Could not initialize database.", e);
		}
	}

	private void initUpdater(String[] args) {
		// check if just updated
		if (args.length >= 2)
			Updater.get().setUpdateInfo(args[0], args[1]);

		// check for updates
		if (Options.isUpdaterDisabled()) {
			return;
		}
		new Thread() {
			@Override
			public void run() {
				try {
					Updater.get().checkForUpdates();
				} catch (IOException e) {
					Log.warn("updatecheck failed.", e);
				}
			}
		}.start();
	}

	private void checkRunningDirectory() {
		if (!Utils.isJarRunning()) {
			return;
		}
		File runningDir = Utils.getRunningDirectory();
		if (runningDir == null) {
			return;
		}
		if (runningDir.getAbsolutePath().indexOf('!') == -1) {
			return;
		}
		errorAndExit("Cannot run from a path that contains a '!'. Please move or rename the jar and try again.");
	}

	private void ensureSingleInstance() {
		if (Options.noSingleInstance()) {
			return;
		}
		try {
			singleInstanceSocket = new ServerSocket(Options.getPort(), 1, InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			// shouldn't happen
		} catch (IOException e) {
			errorAndExit(String.format(
					"Could not launch. Either opsu! is already running or a different program uses port %d.\n" +
					"You can change the port opsu! uses by editing the 'Port' field in the .opsu.cfg configuration file.\n" +
					"If that still does not resolve the problem, you can set 'NoSingleInstance' to 'true', but this is not recommended.", Options.getPort()), e);
		}
	}

	private void closeSingleInstanceSocket() {
		if (singleInstanceSocket == null) {
			return;
		}
		try {
			singleInstanceSocket.close();
		} catch (IOException e) {
			Log.error("Single instance socket was not closed!", e);
		}
	}

	private void errorAndExit(String errstr) {
		ErrorHandler.error(errstr, new Throwable()).allowTerminate().preventContinue().show();
		System.exit(1);
	}

	private void errorAndExit(String errstr, Throwable cause) {
		ErrorHandler.error(errstr, cause).preventContinue().show();
		System.exit(1);
	}

}
