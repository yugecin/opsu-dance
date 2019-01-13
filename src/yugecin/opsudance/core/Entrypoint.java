// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import itdelatrisu.opsu.downloads.Updater;
import yugecin.opsudance.OpsuDance;

import javax.swing.*;

import static yugecin.opsudance.core.Constants.PROJECT_NAME;
import static yugecin.opsudance.core.InstanceContainer.*;

public class Entrypoint
{
	public static final long startTime = System.currentTimeMillis();

	public static void main(String[] args) {
		sout("launched");

		try {
			InstanceContainer.kickstart();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(), "Cannot start " + PROJECT_NAME, JOptionPane.ERROR_MESSAGE);
			return;
			// TODO replace with errorhandler (but stuff may not have kickstarted yet?)
		}

		new OpsuDance().start(args);

		if (updater.getStatus() == Updater.Status.UPDATE_FINAL) {
			updater.runUpdate();
		}
	}

	public static long runtime() {
		return System.currentTimeMillis() - startTime;
	}

	public static void sout(String message) {
		System.out.println(String.format("[%8d] %s", runtime(), message));
	}
}
