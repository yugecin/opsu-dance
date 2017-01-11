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

import com.google.inject.Inject;
import org.lwjgl.LWJGLException;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.errorhandling.ErrorHandler;

import static yugecin.opsudance.kernel.Entrypoint.sout;

public class OpsuDance {

	private final DisplayContainer container;

	@Inject
	public OpsuDance(DisplayContainer container) {
		this.container = container;
	}

	public void start() {
		sout("initialized");
		container.init();
		while (rungame());
	}

	private boolean rungame() {
		try {
			container.setup();
		} catch (LWJGLException e) {
			ErrorHandler.error("could not initialize GL", e, container).showAndExit();
		}
		Exception caughtException = null;
		try {
			container.run();
		} catch (Exception e) {
			caughtException = e;
		}
		container.teardown();
		return caughtException != null && ErrorHandler.error("update/render error", caughtException, container).show().shouldIgnoreAndContinue();
	}

}
