// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import itdelatrisu.opsu.downloads.Updater;
import yugecin.opsudance.OpsuDance;

import javax.swing.*;

import org.newdawn.slick.util.Log;

import static yugecin.opsudance.core.Constants.PROJECT_NAME;
import static yugecin.opsudance.core.errorhandling.ErrorHandler.*;
import static yugecin.opsudance.core.InstanceContainer.*;

import java.io.File;
import java.nio.file.Paths;

public class Entrypoint
{
	public static final long startTime = System.currentTimeMillis();

	public static File workingdir;
	public static boolean isJarRunning;
	public static File jarfile;
	public static File LOGFILE;

	public static void main(String[] args)
	{
		setRuntimeInfo();
		LOGFILE = new File(workingdir, ".opsu.log");
		final LogImpl logImpl = new LogImpl();
		Log.setLogSystem(logImpl);
		Log.info("launched");
		Log.info("working directory: " + workingdir.getAbsolutePath());

		try {
			InstanceContainer.kickstart();
		} catch (Exception e) {
			explode(
				"Failed to kickstart. " + PROJECT_NAME + " will close.",
				e,
				FORCE_TERMINATE
			);
			logImpl.close();
			return;
		}

		new OpsuDance().start(args);

		if (updater.getStatus() == Updater.Status.UPDATE_FINAL) {
			updater.runUpdate();
		}

		logImpl.close();
	}

	private static void setRuntimeInfo()
	{
		final Class<Entrypoint> thiz = Entrypoint.class;
		final String loc = thiz.getResource(thiz.getSimpleName() + ".class").toString();
		isJarRunning = loc.startsWith("jar:");

		if (!isJarRunning) {
			workingdir = Paths.get(".").toAbsolutePath().normalize().toFile();
			jarfile = null;
			return;
		}

		final String wdir = loc.substring(9); // remove jar:file:
		final String separator = "!/";
		final int separatorIdx = wdir.indexOf(separator);
		final int lastSeparatorIdx = wdir.lastIndexOf(separator);
		if (separatorIdx != lastSeparatorIdx) {
			setLAF();
			explode(
				"Cannot run from paths containing '!/', please move the jar file."
				+ "\nCurrent directory: " + wdir.substring(0, lastSeparatorIdx)
				+ "\n" + PROJECT_NAME + " will exit.",
				new Exception(),
				FORCE_TERMINATE | PREVENT_REPORT
			);
			System.exit(0x688);
		}
		jarfile = new File(wdir.substring(0, separatorIdx));
		workingdir = jarfile.getParentFile();
	}

	public static void setLAF()
	{
		if (UIManager.getLookAndFeel().isNativeLookAndFeel()) {
			return;
		}
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
			Log.warn("Unable to set native LAF", t);
		}
	}

	public static long runtime()
	{
		return System.currentTimeMillis() - startTime;
	}
}
