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
package yugecin.opsudance.utils;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class GLHelper {

	/**
	 * from org.newdawn.slick.AppGameContainer#setDisplayMode
	 */
	public static DisplayMode findFullscreenDisplayMode(int targetBPP, int targetFrequency, int width, int height) throws LWJGLException {
		DisplayMode[] modes = Display.getAvailableDisplayModes();
		DisplayMode foundMode = null;
		int freq = 0;
		int bpp = 0;

		for (DisplayMode current : modes) {
			if (current.getWidth() != width || current.getHeight() != height) {
				continue;
			}

			if (current.getBitsPerPixel() == targetBPP && current.getFrequency() == targetFrequency) {
				return current;
			}

			if (current.getFrequency() >= freq && (foundMode == null || current.getBitsPerPixel() >= bpp)) {
				foundMode = current;
				freq = foundMode.getFrequency();
				bpp = foundMode.getBitsPerPixel();
			}
		}
		return foundMode;
	}

}
