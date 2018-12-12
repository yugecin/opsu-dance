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

import org.newdawn.slick.Font;
import org.newdawn.slick.util.Log;

import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.animations.AnimatedValue;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.options.NumericOption;

import static itdelatrisu.opsu.Utils.*;
import static itdelatrisu.opsu.ui.animations.AnimationEquation.LINEAR;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.options.Options.*;

public class VolumeControl implements ResolutionChangedListener
{
	private static final int VALUE_ANIMATION_TIME = 200;
	private static final int DISPLAY_TIME = 2000;

	private static int
		programId = -1, bgprogramId = -1,
		program_attrib_uv, program_uniform_tang, program_uniform_bgalpha, bgprogram_attrib_uv;

	public static void createProgram()
	{
		if (programId != -1) {
			return;
		}

		programId = glCreateProgram();
		int vert = glCreateShader(GL_VERTEX_SHADER);
		int frag = glCreateShader(GL_FRAGMENT_SHADER);

		bgprogramId = glCreateProgram();
		int bgfrag = glCreateShader(GL_FRAGMENT_SHADER);

		boolean result;

		result = compileShader(
			vert,
"#version 110\n"
+"attribute vec2 a_uv;varying vec2 uv;void main(){"
+"gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;uv=a_uv;}"
		);

		if (!result) {
			destroyProgram();
			return;
		}

		// https://www.shadertoy.com/view/ltKBRd
		result = compileShader(
			frag,
"#version 110\n"
+"uniform float tang;uniform float bgalpha;varying vec2 uv;"
+"void main(){"
+"float b,d=length(uv-.5),p=atan(uv.y-.5,uv.x-.5)-1.5707963,g=smoothstep(.2,.0,tang)*.015,bf=0.2618;"
+"p+=step(p,0.)*6.283185;"
+"b=smoothstep(.2594-g,.3444,d)*smoothstep(.485+g,.4,d)*"
+"clamp(max((p-tang+bf)/bf,1.-p/bf),.5,1.-clamp((tang-6.24827842)/0.03490658,0.,.5));\n"
+"float w=1.-.4*step(p,tang),x=smoothstep(.3439,.3444,d)*smoothstep(.4,.3995,d);"
+"gl_FragColor=(1.-x)*vec4(vec3(.3,.8,1.)*b,b*(1.-bgalpha))+x*vec4(w,w,w,1.)"
+"+(1.-smoothstep(.49,.495,d))*vec4(0.,0.,0.,bgalpha);}"
		);

		if (!result) {
			glDeleteShader(vert);
			destroyProgram();
			return;
		}

		result = compileShader(
			bgfrag,
"#version 110\n"
+"varying vec2 uv;void main(){gl_FragColor=vec4(0.,0.,0.,(1.-min(length(uv),1.))*.7);}"
		);

		if (!result) {
			glDeleteShader(vert);
			glDeleteShader(frag);
			destroyProgram();
			return;
		}

		linkShader(programId, vert, frag);
		linkShader(bgprogramId, vert, bgfrag);

		glDeleteShader(vert);
		glDeleteShader(frag);
		glDeleteShader(bgfrag);

		program_attrib_uv = glGetAttribLocation(programId, "a_uv");
		program_uniform_tang = glGetUniformLocation(programId, "tang");
		program_uniform_bgalpha = glGetUniformLocation(programId, "bgalpha");
		bgprogram_attrib_uv = glGetAttribLocation(bgprogramId, "a_uv");
	}

	public static void destroyProgram()
	{
		if (programId != -1) {
			glDeleteProgram(programId);
			glDeleteProgram(bgprogramId);
			programId = -1;
			bgprogramId = -1;
		}
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

	private static void linkShader(int program, int vert, int frag)
	{
		if (program == -1) {
			return;
		}

		glAttachShader(program, vert);
		glAttachShader(program, frag);
		glLinkProgram(program);

		if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
			String error = glGetProgramInfoLog(program, 1024);
			Log.error("Program linking failed.", new Exception(error));
			destroyProgram();
		}
	}

	private final Dial music, effects, master;

	private int bgsize;
	private int bgxpad, bgypad;
	private int displayTimeLeft;

	public VolumeControl()
	{
		this.music = new Dial("music", OPTION_MUSIC_VOLUME, Fonts.SMALLBOLD, -1f, 0f);
		this.effects = new Dial("effect", OPTION_EFFECT_VOLUME, Fonts.SMALLBOLD, -1f, 0f);
		this.master = new Dial("master", OPTION_MASTER_VOLUME, Fonts.MEDIUMBOLD, -.866f, .5f);

		displayContainer.addResolutionChangedListener(this);
	}

	@Override
	public void onResolutionChanged(int w, int h)
	{
		this.music.updatePositions(0.044f, 0.0815f, 0.192f);
		this.effects.updatePositions(0.044f, 0.114f, 0.1173f);
		this.master.updatePositions(0.09f, 0.0225f, 0.0417f);

		this.bgsize = (int) (w * 0.20468f);
		this.bgxpad = w - bgsize;
		this.bgypad = h - bgsize;
	}

	/**
	 * This changes either master, music or effect volume
	 * @param direction any number, positive means up and negative means down
	 */
	public void changeVolume(int direction)
	{
		final int value = 5 * (1 - ((direction & 0x80000000) >>> 30));
		this.master.changeVolume(value);
		this.displayTimeLeft = DISPLAY_TIME;
	}

	public void draw()
	{
		if (displayTimeLeft <= 0) {
			return;
		}
		displayTimeLeft -= renderDelta;

		this.master.val.update(renderDelta);

		if (OPTION_FORCE_FALLBACK_VOLUMECONTROL.state || programId == -1) {
			this.drawFallback();
			return;
		}

		this.music.val.update(renderDelta);
		this.effects.val.update(renderDelta);

		// circle center is at .6743 of the square x&y
		glUseProgram(bgprogramId);
		glPushMatrix();
		glTranslatef(this.bgxpad, this.bgypad, 0f);
		glBegin(GL_QUADS);
		glVertexAttrib2f(bgprogram_attrib_uv, -1f, -1f);
		glVertex3f(0f, 0f, 0f);
		glVertexAttrib2f(bgprogram_attrib_uv, .3257f, -1f);
		glVertex3f(this.bgsize, 0f, 0f);
		glVertexAttrib2f(bgprogram_attrib_uv, .3257f, .3257f);
		glVertex3f(this.bgsize, this.bgsize, 0f);
		glVertexAttrib2f(bgprogram_attrib_uv, -1f, .3257f);
		glVertex3f(0f, this.bgsize, 0f);
		glEnd();
		glPopMatrix();
		glUseProgram(0);

		this.master.draw();
		this.effects.draw();
		this.music.draw();
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

		int width = (int) (MAXWIDTH * this.master.val.getValue());
		glVertex3f(MINX, Y + TWOPAD, 0f);
		glVertex3f(MINX + width, Y + TWOPAD, 0f);
		glVertex3f(MINX + width, Y2 - TWOPAD, 0f);
		glVertex3f(MINX, Y2 - TWOPAD, 0f);
		glEnd();
	}

	private static class Dial
	{
		private final String name;
		private final NumericOption option;
		private final AnimatedValue val;
		private final Font numberFont;
		private final float textxoff, textyoff;

		private int size;
		private float xpad, ypad;
		private float textx, texty;
		private float numberx, numbery;

		private Dial(
			String name,
			NumericOption option,
			Font numberFont,
			float textxoff,
			float textyoff)
		{
			this.name = name;
			this.option = option;
			this.numberFont = numberFont;
			this.textxoff = textxoff;
			this.textyoff = textyoff;

			final float value = option.val / 100f;
			this.val = new AnimatedValue(VALUE_ANIMATION_TIME, value, value, LINEAR);
		}

		private void changeVolume(int diff)
		{
			final int targetVolume = clamp(this.option.val + diff, 0, 100);
			final float displayedVolume = val.getValue();
			val.setTime(0);
			val.setValues(displayedVolume, targetVolume / 100f);
			OPTION_MASTER_VOLUME.setValue(targetVolume);
		}

		private void updatePositions(float wratio, float xpadratio, float ypadratio)
		{
			this.size = (int) (wratio * width);
			this.xpad = width - (int) (xpadratio * width + this.size);
			this.ypad = height - (int) (ypadratio * height + this.size);

			final float textwidth = Fonts.MEDIUM.getWidth(this.name + " ");
			final float lh2 = Fonts.MEDIUM.getHeight(this.name) * .7f;
			final float size2 = this.size / 2f;
			this.numberx = this.xpad + size2;
			this.numbery = this.ypad + size2;
			this.textx = this.numberx + this.textxoff * size2 - textwidth;
			this.texty = this.numbery + this.textyoff * size2 - lh2;
			this.numbery -= this.numberFont.getHeight("196%") * .7f;
		}

		private void draw()
		{
			final float targetAngle = (1f - val.getValue()) * 6.2831853071795864f;
			glUseProgram(programId);
			glUniform1f(program_uniform_tang, targetAngle);
			glUniform1f(program_uniform_bgalpha, .3f);
			glPushMatrix();
			glTranslatef(this.xpad, this.ypad, 0f);
			glBegin(GL_QUADS);
			glVertexAttrib2f(program_attrib_uv, 0f, 1f);
			glVertex3f(0f, 0f, 0f);
			glVertexAttrib2f(program_attrib_uv, 1f, 1f);
			glVertex3f(this.size, 0f, 0f);
			glVertexAttrib2f(program_attrib_uv, 1f, 0f);
			glVertex3f(this.size, this.size, 0f);
			glVertexAttrib2f(program_attrib_uv, 0f, 0f);
			glVertex3f(0f, this.size, 0f);
			glEnd();
			glPopMatrix();
			glUseProgram(0);

			Fonts.MEDIUM.drawString(this.textx, this.texty, this.name);
			final String number = String.valueOf((int) (val.getValue() * 100)) + "%";
			final float noffx = this.numberFont.getWidth(number) / 2f;
			this.numberFont.drawString(this.numberx - noffx, this.numbery, number);
		}
	}
}
