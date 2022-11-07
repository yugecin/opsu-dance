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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;

public class Entrypoint
{
	public static final long startTime = System.currentTimeMillis();

	public static File workingdir;
	public static boolean isJarRunning;
	public static File jarfile;
	public static File LOGFILE;
	public static File RESOURCES;

	public static void main(String[] args)
	{
		setRuntimeInfo();
		LOGFILE = new File(workingdir, ".opsu.log");
		final LogImpl logImpl = new LogImpl();
		Log.setLogSystem(logImpl);
		Log.info("launched");
		Log.info("working directory: " + workingdir.getAbsolutePath());
		if (!isJarRunning) {
			File root = workingdir;
			for (int i = 4;;) {
				File res = new File(root, "res");
				if (res.exists()) {
					RESOURCES = res;
					Log.info("resources directory: " + res.getAbsolutePath());
					break;
				}
				root = root.getParentFile();
				if (root == null || --i < 0) {
					Log.warn("no resources directory found!");
					break;
				}
			}
		}

		try {
			NativeLoader.loadNatives();
		} catch (Throwable e) {
			explode(
				"Failed to unpack natives. " + PROJECT_NAME + " will close.",
				e,
				FORCE_TERMINATE
			);
			logImpl.close();
			return;
		}

		try {
			InstanceContainer.kickstart();
		} catch (Throwable e) {
			explode(
				"Failed to kickstart. " + PROJECT_NAME + " will close.",
				e,
				FORCE_TERMINATE
			);
			logImpl.close();
			return;
		}

		new OpsuDance().start(args);
		jobContainer.interrupt();
		try {
			jobContainer.join(200);
		} catch (InterruptedException e) {
			Log.error("failed to join thread " + jobContainer.getName(), e);
		}

		if (updater.getStatus() == Updater.Status.UPDATE_FINAL) {
			updater.runUpdate();
		}

		logImpl.close();
		System.exit(0);
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
			explode(
				"Cannot run from paths containing '!/', please move the jar file."
				+ "\nCurrent directory: " + wdir.substring(0, lastSeparatorIdx)
				+ "\n" + PROJECT_NAME + " will exit.",
				new Exception(),
				FORCE_TERMINATE | PREVENT_REPORT
			);
			System.exit(0x688);
		}
		final String path = wdir.substring(0, separatorIdx);
		jarfile = new File(path);
		if (!jarfile.exists()) {
			try {
				jarfile = new File(URLDecoder.decode(path, "utf-8"));
			} catch (UnsupportedEncodingException e) {
				System.err.println("failed to decode path url");
			}
		}
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
