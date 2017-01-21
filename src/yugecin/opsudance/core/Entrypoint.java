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
import yugecin.opsudance.core.inject.OpsuDanceInjector;

public class Entrypoint {

	public static final long startTime = System.currentTimeMillis();

	public static void main(String[] args) {
		sout("launched");
		(new OpsuDanceInjector()).provide(OpsuDance.class).start(args);

		if (Updater.get().getStatus() == Updater.Status.UPDATE_FINAL) {
			Updater.get().runUpdate();
		}
	}

	public static long runtime() {
		return System.currentTimeMillis() - startTime;
	}

	public static void sout(String message) {
		System.out.println(String.format("[%7d] %s", runtime(), message));
	}

}
