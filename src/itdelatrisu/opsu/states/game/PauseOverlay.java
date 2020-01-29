// Copyright 2018-2020 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package itdelatrisu.opsu.states.game;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.states.game.Game.RestartReason;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import yugecin.opsudance.core.state.*;
import yugecin.opsudance.events.ResolutionChangedListener;
import yugecin.opsudance.events.SkinChangedListener;
import yugecin.opsudance.core.input.*;

import static itdelatrisu.opsu.GameImage.*;
import static itdelatrisu.opsu.Utils.clamp;
import static org.lwjgl.input.Keyboard.*;
import static org.lwjgl.opengl.GL11.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

import java.io.StringWriter;

class PauseOverlay
	implements Renderable, InputListener, SkinChangedListener, ResolutionChangedListener
{
	private static final int
		LOSE_WAIT_TIME = 2000,
		LOSE_FADEIN_TIME = 500,
		INVALID_FADE_TIME = 20000;

	private Image background;
	private MenuButton continueButton, retryButton, backButton;
	private boolean dirty, active;
	private boolean ignoreNextEscapeRelease;
	private boolean readyToResume;
	private boolean wasCursorVisible;
	private boolean isLose;
	private int fadeInTime;

	int mousePauseX, mousePauseY;
	float pausePulseTiming;
	boolean requireMousePositionBeforeResume;

	PauseOverlay()
	{
		this.dirty = true;
		displayContainer.addResolutionChangedListener(this);
		skinservice.addSkinChangedListener(this);
	}

	void engagePause()
	{
		this.isLose = false;
		this.background = PAUSE_OVERLAY.getImage();
		MusicController.pause();
		this.fadeInTime = INVALID_FADE_TIME;
		this.engage();
	}

	void engageLose()
	{
		this.isLose = true;
		this.background = FAIL_BACKGROUND.getImage();
		this.fadeInTime = -LOSE_WAIT_TIME;
		this.engage();
	}

	private void engage()
	{
		this.active = true;
		if (this.dirty) {
			this.revalidate();
		}
		this.readyToResume = false;
		this.ignoreNextEscapeRelease = true;
		input.addListener(this);
		displayContainer.addOverlay(this);
		this.continueButton.resetHover();
		this.retryButton.resetHover();
		this.backButton.resetHover();
		this.wasCursorVisible = displayContainer.drawCursor;
		displayContainer.drawCursor = true;
	}

	private void disengage()
	{
		this.hide();
		MusicController.stopFade();
		MusicController.resume();
	}

	void hide()
	{
		this.active = false;
		input.removeListener(this);
		displayContainer.removeOverlay(this);
		displayContainer.drawCursor = this.wasCursorVisible;
	}

	void backToSongMenu()
	{
		SoundController.playSound(SoundEffect.MENUBACK);
		songMenuState.resetGameDataOnLoad();
		MusicController.stopFade();
		MusicController.setPitch(1.0f);
		displayContainer.switchState(songMenuState);
	}

	boolean isActive()
	{
		return this.active;
	}

	private void revalidate()
	{
		this.dirty = false;
		continueButton = new MenuButton(PAUSE_CONTINUE.getImage(), width2, height * 0.25f);
		retryButton = new MenuButton(PAUSE_RETRY.getImage(), width2, height2);
		backButton = new MenuButton(PAUSE_BACK.getImage(), width2, height * 0.75f);
		continueButton.setHoverAnimationDuration(100);
		retryButton.setHoverAnimationDuration(100);
		backButton.setHoverAnimationDuration(100);
		continueButton.setHoverAnimationEquation(AnimationEquation.LINEAR);
		retryButton.setHoverAnimationEquation(AnimationEquation.LINEAR);
		backButton.setHoverAnimationEquation(AnimationEquation.LINEAR);
		continueButton.setHoverExpand();
		retryButton.setHoverExpand();
		backButton.setHoverExpand();
	}

	private void resumeIfClickedInPausedPosition(Event e, int mouseX, int mouseY)
	{
		final int dx = this.mousePauseX - mouseX;
		final int dy = this.mousePauseY - mouseY;
		final double distance = Math.hypot(dx, dy);
		final int circleRadius = HITCIRCLE.getWidth() / 2;
		if (distance < circleRadius) {
			e.unconsume();
			this.disengage();
		}
	}

	private void readyToResume()
	{
		if (this.readyToResume) {
			this.readyToResume = false;
			return;
		}

		this.readyToResume = true;
		if (!this.requireMousePositionBeforeResume) {
			this.disengage();
		}
	}

	private void checkButtonClick(int x, int y)
	{
		if (!this.isLose && this.continueButton.contains(x, y)) {
			this.readyToResume();
			return;
		}

		if (this.retryButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			this.disengage();
			gameState.restart(RestartReason.USER);
			return;
		}

		if (this.backButton.contains(x, y)) {
			this.backToSongMenu();
		}
	}

	@Override
	public void onResolutionChanged(int w, int h)
	{
		this.dirty = true;
		if (this.active) {
			this.revalidate();
		}
	}

	@Override
	public void onSkinChanged(String name)
	{
		this.dirty = true;
		if (this.active) {
			this.revalidate();
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		e.consume();
		volumeControl.changeVolume(e.direction);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		e.consume();
		if (this.readyToResume) {
			this.resumeIfClickedInPausedPosition(e, e.x, e.y);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		e.consume();
		this.checkButtonClick(e.x, e.y);
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
		e.consume();
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		e.consume();
		if (e.keyCode == KEY_S && input.isControlDown()) {
			gameState.saveCheckpoint();
		} else if (e.keyCode == KEY_SUBTRACT || e.keyCode == KEY_MINUS || e.chr == '-') {
			gameState.adjustLocalMusicOffset(-5);
		} else if (e.keyCode == KEY_EQUALS || e.keyCode == KEY_ADD || e.chr == '+') {
			gameState.adjustLocalMusicOffset(5);
		}

		if (!Keyboard.isRepeatEvent()) {
			if (e.keyCode == OPTION_KEY_LEFT.keycode ||
				e.keyCode == OPTION_KEY_RIGHT.keycode)
			{
				if (this.readyToResume) {
					this.resumeIfClickedInPausedPosition(e, mouseX, mouseY);
				} else {
					this.checkButtonClick(mouseX, mouseY);
				}
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		e.consume();
		if (e.keyCode == KEY_ESCAPE) {
			if (this.ignoreNextEscapeRelease) {
				this.ignoreNextEscapeRelease = false;
				return;
			}

			if (this.fadeInTime < LOSE_FADEIN_TIME) {
				this.fadeInTime = LOSE_FADEIN_TIME;
				return;
			}

			this.readyToResume();
		}
	}

	@Override
	public void preRenderUpdate()
	{
		this.pausePulseTiming += renderDelta / 750f;
		if (this.pausePulseTiming > 1f) {
			this.pausePulseTiming = 0f;
		}

		if (this.fadeInTime < INVALID_FADE_TIME) {
			this.fadeInTime += renderDelta;
		}

		this.continueButton.hoverUpdate(renderDelta, mouseX, mouseY);
		this.retryButton.hoverUpdate(renderDelta, mouseX, mouseY);
		this.backButton.hoverUpdate(renderDelta, mouseX, mouseY);
	}

	@Override
	public void render(Graphics g)
	{
		if (this.fadeInTime < 0) {
			return;
		}

		if (this.readyToResume) {
			// draw glowing hit select circle and pulse effect
			Image cursorCircle = HITCIRCLE_SELECT.absScale(HITCIRCLE.getWidth());
			cursorCircle.setAlpha(1.0f);
			cursorCircle.drawCentered(this.mousePauseX, this.mousePauseY);
			final float pulseScale = 1f + this.pausePulseTiming;
			Image cursorCirclePulse = cursorCircle.getScaledCopy(pulseScale);
			cursorCirclePulse.setAlpha(1f - this.pausePulseTiming);
			cursorCirclePulse.drawCentered(this.mousePauseX, this.mousePauseY);
			return;
		}

		final float fadein = clamp((float) this.fadeInTime / LOSE_FADEIN_TIME, 0f, 1f);
		glDisable(GL_TEXTURE_2D);
		glColor4f(0f, 0f, 0f, .6f * fadein);
		glBegin(GL_QUADS);
		glVertex2f(0, 0);
		glVertex2f(width, 0);
		glVertex2f(width, height);
		glVertex2f(0, height);
		glEnd();
		glEnable(GL_TEXTURE_2D);

		glColor4f(1f, 1f, 1f, fadein);
		this.background.draw(0, 0, null);

		if (!this.isLose) {
			glColor4f(1f, 1f, 1f, fadein);
			this.continueButton.draw(null);
		}
		glColor4f(1f, 1f, 1f, fadein);
		this.retryButton.draw(null);
		glColor4f(1f, 1f, 1f, fadein);
		this.backButton.draw(null);
	}

	@Override
	public void writeErrorDump(StringWriter dump)
	{
		dump.write("> pause overlay is active\n");
	}
}
