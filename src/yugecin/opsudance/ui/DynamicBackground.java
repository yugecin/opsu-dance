// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.ui;

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.beatmap.Beatmap;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.events.SkinChangedListener;
import yugecin.opsudance.render.TextureData;

import static itdelatrisu.opsu.GameImage.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.utils.GLHelper.*;

import java.util.Objects;

/**
 * draws background image in main menu and song menu
 */
public class DynamicBackground implements SkinChangedListener, ResolutionChangedListener
{
	private TextureData defaultBg;
	private Beatmap oldBeatmap, newBeatmap;
	private int fadeOutTime, fadeInTime;
	private static final int FADE_TIME = 300;
	private static final float MAX_ALPHA = .7f;

	public DynamicBackground()
	{
		skinservice.addSkinChangedListener(this);
		displayContainer.addResolutionChangedListener(this);
	}

	@Override
	public void onResolutionChanged(int w, int h)
	{
		this.revalidate();
	}

	@Override
	public void onSkinChanged(String name)
	{
		this.revalidate();
	}

	public void update()
	{
		if (this.fadeOutTime > 0) {
			if ((this.fadeOutTime -= renderDelta) < 0) {
				this.fadeOutTime = 0;
			}
		}
		if ((this.newBeatmap.bg == null || !this.newBeatmap.isBackgroundLoading()) &&
			this.fadeInTime < FADE_TIME) {
			if ((this.fadeInTime += renderDelta) > FADE_TIME) {
				this.fadeInTime = FADE_TIME;
			}
		}
	}

	public void draw()
	{
		if (this.fadeOutTime != 0) {
			final float oldAlpha = MAX_ALPHA * (float) this.fadeOutTime / FADE_TIME;
			this.drawBeatmapBackgroundOrDefault(this.oldBeatmap, oldAlpha);
		}

		final float newAlpha = this.fadeInTime == FADE_TIME
			? MAX_ALPHA
			: MAX_ALPHA * this.fadeInTime / FADE_TIME;
		this.drawBeatmapBackgroundOrDefault(this.newBeatmap, newAlpha);
	}

	private void drawBeatmapBackgroundOrDefault(Beatmap beatmap, float alpha)
	{
		if (beatmap == null || !beatmap.drawBackground(width, height, alpha, true)) {
			glColor4f(1f, 1f, 1f, alpha);
			glEnable(GL_TEXTURE_2D);
			simpleTexturedQuadTopLeft(this.defaultBg);
		}
	}

	public void reset()
	{
		this.fadeOutTime = 0;
		this.fadeInTime = 0;
		this.oldBeatmap = null;
		this.newBeatmap = MusicController.getBeatmap();
		if (this.newBeatmap != null) {
			this.newBeatmap.loadBackground();
		}
	}

	public void fadeInNow()
	{
		this.fadeOutTime = 0;
		this.fadeInTime = 0;
	}

	public void songChanged()
	{
		final Beatmap beatmap = MusicController.getBeatmap();
		if (beatmap == this.newBeatmap) {
			return;
		}
		if (this.newBeatmap != null && beatmap != null &&
			Objects.equals(beatmap.bg, this.newBeatmap.bg))
		{
			return;
		}
		this.fadeOutTime = this.fadeInTime;
		this.fadeInTime = 0;
		this.oldBeatmap = this.newBeatmap;
		this.newBeatmap = beatmap;
	}

	private void revalidate()
	{
		final TextureData td = this.defaultBg = new TextureData(MENU_BG);
		final float ratio = td.height / td.width;
		final float gameRatio = (float) height / width;
		td.width = width;
		td.height = (ratio > gameRatio ? ratio : gameRatio) * width;

		this.reset();
		this.songChanged();
	}
}
