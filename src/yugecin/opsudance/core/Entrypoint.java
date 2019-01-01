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
package yugecin.opsudance.core;

import itdelatrisu.opsu.downloads.Updater;
import yugecin.opsudance.OpsuDance;

import javax.swing.*;

import org.newdawn.slick.util.Log;

import static yugecin.opsudance.core.Constants.PROJECT_NAME;
import static yugecin.opsudance.core.InstanceContainer.*;

public class Entrypoint {

	public static final long startTime = System.currentTimeMillis();

	public static void main(String[] args) {
		sout("launched");

		try {
			InstanceContainer.kickstart();
		} catch (Exception e) {
			Log.error("cannot start", e);
			JOptionPane.showMessageDialog(null, e.getMessage(), "Cannot start " + PROJECT_NAME, JOptionPane.ERROR_MESSAGE);
			// TODO replace with errorhandler
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
