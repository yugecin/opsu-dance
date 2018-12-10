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

import itdelatrisu.opsu.ui.animations.AnimatedValue;

import static itdelatrisu.opsu.Utils.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.LINEAR;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.options.Options.*;

public class VolumeControl
{
	private static final int VALUE_ANIMATION_TIME = 200;
	private static final int DISPLAY_TIME = 2000;

	private static int programId = -1;
	private static int program_attrib_uv, program_uniform_tang;

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
+"#define CIRCLE_WIDTH .04\n"
+"#define OUTER_RADIUS RADIUS + CIRCLE_WIDTH\n"
+"#define INNER_RADIUS RADIUS - CIRCLE_WIDTH\n"
+"#define GLOW_WIDTH .08\n"
+"#define GLOW_EXTRA_WIDTH .015\n"
+"#define TRANSITION_WIDTH .005\n"
+"#define GLOW_OVERFLOW_RADIANS 15.\n"
+"\n"
+"uniform float tang;\n"
+"varying vec2 uv;\n"
+"\n"
+"void main()\n"
+"{\n"
+"    vec2 d = uv - vec2(.5);\n"
+"    float dl = length(d);\n"
+"    \n"
+"    float pang = atan(d.y, d.x) - PI2;\n"
+"    if (pang < 0.) pang += TWOPI;\n"
+"    \n"
+"    float extraglow = tang == 0. ? GLOW_EXTRA_WIDTH : 0.;\n"
+"    \n"
+"    float blue =\n"
+"        smoothstep(INNER_RADIUS - GLOW_WIDTH - extraglow, INNER_RADIUS, dl)\n"
+"        *smoothstep(OUTER_RADIUS + GLOW_WIDTH + extraglow, OUTER_RADIUS, dl)\n"
+"        ;\n"
+"    float bluefade = radians(GLOW_OVERFLOW_RADIANS);\n"
+"    float maxblue = 1. - clamp((tang - TWOPI + radians(2.)) / radians(2.), 0., .65);\n"
+"    blue *= clamp(max((pang - (tang - bluefade)) / bluefade, 1. - pang / bluefade), .35, maxblue);\n"
+"\n"
+"    float white = 1. - .4 * step(pang, tang);\n"
+"    \n"
+"    float x =\n"
+"        smoothstep(RADIUS - CIRCLE_WIDTH - TRANSITION_WIDTH, RADIUS - CIRCLE_WIDTH, dl)\n"
+"        *smoothstep(RADIUS + CIRCLE_WIDTH, RADIUS + CIRCLE_WIDTH - TRANSITION_WIDTH, dl);\n"
+"    gl_FragColor = (1. - x) * vec4(.3, .8, 1., blue) + x * vec4(1., 1., 1., white);\n"
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
		program_uniform_tang = glGetUniformLocation(programId, "tang");
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
	private float targetVolume;
	private AnimatedValue val;

	public VolumeControl()
	{
		this.val = new AnimatedValue(VALUE_ANIMATION_TIME, 0, 1, LINEAR);

		final float currentVolume = OPTION_MASTER_VOLUME.val / 100f;
		this.val.setValues(currentVolume, currentVolume);
	}

	/**
	 * This changes either master, music or effect volume
	 * @param direction any number, positive means up and negative means down
	 */
	public void changeVolume(int direction)
	{
		this.targetVolume = clamp(
			OPTION_MASTER_VOLUME.val / 100f + .05f * (1 - ((direction & 0x80000000) >>> 30)),
			0f,
			1f
		);
		final float displayedVolume = val.getValue();
		val.setTime(0);
		val.setValues(displayedVolume, this.targetVolume);
		OPTION_MASTER_VOLUME.setValue((int) (this.targetVolume * 100f));
		this.displayTimeLeft = DISPLAY_TIME;
	}

	public void draw()
	{
		if (displayTimeLeft <= 0) {
			return;
		}
		displayTimeLeft -= renderDelta;

		val.update(renderDelta);

		if (OPTION_FORCE_FALLBACK_VOLUMECONTROL.state || programId == -1) {
			this.drawFallback();
			return;
		}

		final float targetAngle = (1f - val.getValue()) * 6.2831853071795864f;
		final float DIM = 200f;
		glUseProgram(programId);
		glUniform1f(program_uniform_tang, targetAngle);
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

		int width = (int) (MAXWIDTH * this.val.getValue());
		glVertex3f(MINX, Y + TWOPAD, 0f);
		glVertex3f(MINX + width, Y + TWOPAD, 0f);
		glVertex3f(MINX + width, Y2 - TWOPAD, 0f);
		glVertex3f(MINX, Y2 - TWOPAD, 0f);
		glEnd();
	}
}
