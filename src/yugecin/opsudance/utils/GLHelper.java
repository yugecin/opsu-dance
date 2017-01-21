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

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.opengl.ImageIOImageData;
import org.newdawn.slick.opengl.LoadableImageData;
import org.newdawn.slick.opengl.TGAImageData;
import org.newdawn.slick.util.Log;
import org.newdawn.slick.util.ResourceLoader;
import yugecin.opsudance.core.errorhandling.ErrorHandler;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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

	/**
	 * from org.newdawn.slick.AppGameContainer#setDisplayMode
	 */
	public static void setIcons(String[] refs) {
		ByteBuffer[] bufs = new ByteBuffer[refs.length];

		for (int i = 0; i < refs.length; i++) {
			LoadableImageData data;
			boolean flip = true;

			if (refs[i].endsWith(".tga")) {
				data = new TGAImageData();
			} else {
				flip = false;
				data = new ImageIOImageData();
			}

			try {
				bufs[i] = data.loadImage(ResourceLoader.getResourceAsStream(refs[i]), flip, false, null);
			} catch (Exception e) {
				Log.error("failed to set the icon", e);
				return;
			}
		}

		Display.setIcon(bufs);
	}

	public static void hideNativeCursor() {
		try {
			int min = Cursor.getMinCursorSize();
			IntBuffer tmp = BufferUtils.createIntBuffer(min * min);
			Mouse.setNativeCursor(new Cursor(min, min, min / 2, min / 2, 1, tmp, null));
		} catch (LWJGLException e) {
			ErrorHandler.error("Cannot hide native cursor", e).show();
		}
	}

	public static void showNativeCursor() {
		try {
			Mouse.setNativeCursor(null);
		} catch (LWJGLException e) {
			ErrorHandler.error("Cannot show native cursor", e).show();
		}
	}

}
