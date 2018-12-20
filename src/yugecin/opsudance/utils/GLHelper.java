// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
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

import yugecin.opsudance.render.TextureData;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.core.errorhandling.ErrorHandler.*;

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
			explode("Cannot hide native cursor", e, DEFAULT_OPTIONS);
		}
	}

	public static void showNativeCursor() {
		try {
			Mouse.setNativeCursor(null);
		} catch (LWJGLException e) {
			explode("Cannot show native cursor", e, DEFAULT_OPTIONS);
		}
	}

	public static void simpleTexturedQuad(TextureData td)
	{
		glBindTexture(GL_TEXTURE_2D, td.id);
		glBegin(GL_QUADS);
		glTexCoord2f(0f, 0f);
		glVertex2f(-td.width2, -td.height2);
		glTexCoord2f(td.txtw, 0f);
		glVertex2f(td.width2, -td.height2);
		glTexCoord2f(td.txtw, td.txth);
		glVertex2f(td.width2, td.height2);
		glTexCoord2f(0f, td.txth);
		glVertex2f(-td.width2, td.height2);
		glEnd();
	}
}
