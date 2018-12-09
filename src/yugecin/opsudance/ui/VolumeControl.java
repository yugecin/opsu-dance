/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2018 yugecin
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
package yugecin.opsudance.ui;

import static yugecin.opsudance.core.InstanceContainer.*;

import org.newdawn.slick.util.Log;

import static itdelatrisu.opsu.Utils.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.options.Options.*;

public class VolumeControl
{
	private static final int DISPLAY_TIME = 2000;

	private static int programId = -1;
	private static int program_attrib_uv;

	public static void createProgram()
	{
		if (programId != -1) {
			return;
		}

		programId = glCreateProgram();
		int vert = glCreateShader(GL_VERTEX_SHADER);
		int frag = glCreateShader(GL_FRAGMENT_SHADER);

		boolean result;

		result = compileShader(
			vert,
			"#version 110\n"
			+ "attribute vec2 a_uv;"
			+ "varying vec2 uv;\n"
			+ "void main(){"
			+ "gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;"
			+ "uv=a_uv;"
			+ "}"
		);

		if (!result) {
			destroyProgram();
			return;
		}

		// https://www.shadertoy.com/view/ltKBRd
		result = compileShader(
			frag,
			"#version 110\n"
+"#define PI2 1.570796326795\n"
+"#define PI 3.14159265359\n"
+"#define TWOPI 6.28318530718\n"
+"\n"
+"#define RADIUS .3\n"
+"#define CIRCLE_WIDTH .0275\n"
+"#define OUTER_RADIUS RADIUS + CIRCLE_WIDTH\n"
+"#define INNER_RADIUS RADIUS - CIRCLE_WIDTH\n"
+"#define GLOW_WIDTH .035\n"
+"#define GLOW_EXTRA_WIDTH .005\n"
+"#define TRANSITION_WIDTH .001\n"
+"#define GLOW_OVERFLOW_RADIANS 4.\n"
+"\n"
+"varying vec2 uv;\n"
+"\n"
+"void main()\n"
+"{\n"
+"    vec2 d = uv - vec2(.5);\n"
+"    float dl = length(d);\n"
+"    \n"
+"    float pang = atan(d.y, d.x) - PI2;\n"
+"    if (pang < 0.) pang += TWOPI;\n"
+"    float tang = .6f;\n"
+"    \n"
+"    float extraglow = tang == 0. ? GLOW_EXTRA_WIDTH : 0.;\n"
+"    \n"
+"    vec3 blue =\n"
+"        vec3(.3, .8, 1.)\n"
+"        *smoothstep(INNER_RADIUS - GLOW_WIDTH - extraglow, INNER_RADIUS, dl)\n"
+"        *smoothstep(OUTER_RADIUS + GLOW_WIDTH + extraglow, OUTER_RADIUS, dl)\n"
+"        ;\n"
+"    float bluefade = radians(GLOW_OVERFLOW_RADIANS);\n"
+"    float maxblue = 1. - clamp((tang - TWOPI + radians(2.)) / radians(2.), 0., .65);\n"
+"    blue *= clamp(max((pang - (tang - bluefade)) / bluefade, 1. - pang / bluefade), .35, maxblue);\n"
+"\n"
+"    vec3 white = vec3(1.) - .4 * step(pang, tang);\n"
+"    //float whitefade = radians(2.);\n"
+"    //vec3 white = vec3(1.) * clamp((pang - (tang - whitefade)) / whitefade, .6, 1.);\n"
+"    \n"
+"    float x =\n"
+"        smoothstep(RADIUS - CIRCLE_WIDTH - TRANSITION_WIDTH, RADIUS - CIRCLE_WIDTH, dl)\n"
+"        *smoothstep(RADIUS + CIRCLE_WIDTH, RADIUS + CIRCLE_WIDTH - TRANSITION_WIDTH, dl);\n"
+"    vec3 col = (1. - x) * blue + x * white;\n"
+"    \n"
+"    gl_FragColor = vec4(col,1.0);\n"
+"}\n"

		);

		if (!result) {
			glDeleteShader(vert);
			destroyProgram();
			return;
		}

		glAttachShader(programId, vert);
		glAttachShader(programId, frag);
		glLinkProgram(programId);

		if (glGetProgrami(programId, GL_LINK_STATUS) != GL_TRUE) {
			String error = glGetProgramInfoLog(programId, 1024);
			Log.error("Program linking failed.", new Exception(error));
			destroyProgram();
		}

		glDeleteShader(vert);
		glDeleteShader(frag);

		program_attrib_uv = glGetAttribLocation(programId, "a_uv");
	}

	public static void destroyProgram()
	{
		glDeleteProgram(programId);
		programId = -1;
	}

	private static boolean compileShader(int handle, String source)
	{
		glShaderSource(handle, source);
		glCompileShader(handle);
		if (glGetShaderi(handle, GL_COMPILE_STATUS) != GL_TRUE) {
			String error = glGetShaderInfoLog(handle, 1024);
			Log.error("Shader compilation failed.", new Exception(error));
			return false;
		}
		return true;
	}

	private int displayTimeLeft;

	/**
	 * This changes either master, music or effect volume
	 * @param direction any number, positive means up and negative means down
	 */
	public void changeVolume(int direction)
	{
		float newvolume = OPTION_MASTER_VOLUME.val / 100f;
		newvolume += .05f * (1 - ((direction & 0x80000000) >>> 30));
		newvolume = clamp(newvolume, 0f, 1f);
		OPTION_MASTER_VOLUME.setValue((int) (newvolume * 100f));
		this.displayTimeLeft = DISPLAY_TIME;
	}

	public void draw()
	{
		if (displayTimeLeft <= 0) {
			return;
		}
		displayTimeLeft -= renderDelta;

		if (OPTION_FORCE_FALLBACK_VOLUMECONTROL.state || programId == -1) {
			this.drawFallback();
			return;
		}

		final float DIM = 100f;
		glUseProgram(programId);
		glBegin(GL_QUADS);
		glTexCoord2f(0f, 0f);
		glVertexAttrib2f(program_attrib_uv, 0f, 1f);
		glVertex3f(width - DIM, height - DIM, 0f);
		glTexCoord2f(1f, 0f);
		glVertexAttrib2f(program_attrib_uv, 1f, 1f);
		glVertex3f(width, height - DIM, 0f);
		glTexCoord2f(1f, 1f);
		glVertexAttrib2f(program_attrib_uv, 1f, 0f);
		glVertex3f(width, height, 0f);
		glTexCoord2f(0f, 1f);
		glVertexAttrib2f(program_attrib_uv, 0f, 0f);
		glVertex3f(width - DIM, height, 0f);
		glEnd();
		glUseProgram(0);
	}

	private void drawFallback()
	{
		final int IPAD = 4, OPAD = 20, TWOPAD = IPAD * 2;
		final int HEIGHT = 40;
		final int Y = height - OPAD - HEIGHT - IPAD * 4;
		final int Y2 = height - OPAD;
		final int ENDX = width - OPAD;
		final int MAXWIDTH = ENDX - OPAD - TWOPAD * 2;
		final int MINX = OPAD + TWOPAD;

		glDisable(GL_TEXTURE_2D);
		glColor3f(1f, 1f, 1f);
		glBegin(GL_QUADS);
		glVertex3f(OPAD, Y, 0f);
		glVertex3f(ENDX, Y, 0f);
		glVertex3f(ENDX, Y + IPAD, 0f);
		glVertex3f(OPAD, Y + IPAD, 0f);

		glVertex3f(OPAD, Y2 - IPAD, 0f);
		glVertex3f(ENDX, Y2 - IPAD, 0f);
		glVertex3f(ENDX, Y2, 0f);
		glVertex3f(OPAD, Y2, 0f);

		glVertex3f(OPAD, Y, 0f);
		glVertex3f(OPAD + IPAD, Y, 0f);
		glVertex3f(OPAD + IPAD, Y2, 0f);
		glVertex3f(OPAD, Y2, 0f);

		glVertex3f(ENDX - IPAD, Y, 0f);
		glVertex3f(ENDX, Y, 0f);
		glVertex3f(ENDX, Y2, 0f);
		glVertex3f(ENDX - IPAD, Y2, 0f);

		int width = (int) (MAXWIDTH * OPTION_MASTER_VOLUME.val / 100f);
		glVertex3f(MINX, Y + TWOPAD, 0f);
		glVertex3f(MINX + width, Y + TWOPAD, 0f);
		glVertex3f(MINX + width, Y2 - TWOPAD, 0f);
		glVertex3f(MINX, Y2 - TWOPAD, 0f);
		glEnd();
	}
}
