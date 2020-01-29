// Copyright 2017-2020 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.beatmap.BeatmapWatchService;
import itdelatrisu.opsu.db.DBController;

import org.lwjgl.openal.AL;
import org.newdawn.slick.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import static yugecin.opsudance.core.errorhandling.ErrorHandler.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * loosely based on {@link itdelatrisu.opsu.Opsu}
 */
public class OpsuDance
{
	private ServerSocket singleInstanceSocket;

	public void start(String[] args) {
		try {
			Log.info("initialized");

			optionservice.loadOptions();
			if (this.isOtherInstanceRunning()) {
				return;
			}
			Log.info("prechecks done and options parsed");

			try {
				DBController.init();
			} catch (Throwable e) {
				explode("Could not initialize database.", e, PREVENT_CONTINUE);
				return;
			}
			initUpdater(args);
			Log.info("database & updater initialized");

			displayContainer.init(splashState);
		} catch (Exception e) {
			explode("startup failure", e, PREVENT_CONTINUE);
			return;
		}

		while (rungame());
		AL.destroy();

		optionservice.saveOptions();
		closeSingleInstanceSocket();
		DBController.closeConnections();
		Utils.deleteDirectory(config.TEMP_DIR);
		if (!OPTION_ENABLE_WATCH_SERVICE.state) {
			BeatmapWatchService.destroy();
		}
	}

	private boolean rungame() {
		try {
			displayContainer.setup();
			displayContainer.resume();
		} catch (Exception e) {
			explode("could not initialize GL", e, ALLOW_TERMINATE | PREVENT_CONTINUE);
			return false;
		}
		Exception caughtException = null;
		try {
			displayContainer.run();
		} catch (Exception e) {
			caughtException = e;
		}
		displayContainer.teardown();
		displayContainer.pause();
		return caughtException != null && explode("update/render error", caughtException, ALLOW_TERMINATE);
	}

	private void initUpdater(String[] args) {
		// check if just updated
		if (args.length >= 2) {
			updater.setUpdateInfo(args[0], args[1]);
		}

		// check for updates
		if (OPTION_DISABLE_UPDATER.state) {
			return;
		}
		new Thread() {
			@Override
			public void run() {
				try {
					updater.checkForUpdates();
				} catch (IOException e) {
					Log.warn("updatecheck failed.", e);
				}
			}
		}.start();
	}

	private boolean isOtherInstanceRunning()
	{
		if (OPTION_NOSINGLEINSTANCE.state) {
			return false;
		}

		try {
			singleInstanceSocket = new ServerSocket(OPTION_PORT.val, 1, InetAddress.getLocalHost());
			return false;
		} catch (UnknownHostException e) {
			// shouldn't happen
			return false;
		} catch (IOException e) {
			final String message = String.format(
				"Could not launch. Either opsu! is already running or a different program uses port %d.\n" +
				"You can change the port opsu! uses by editing the 'Port' field in the .opsu.cfg configuration file.\n" +
				"If that still does not resolve the problem, you can set 'NoSingleInstance' to 'true', but this is not recommended.", OPTION_PORT.val);
			explode(message, e, PREVENT_CONTINUE);
			return true;
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
}
