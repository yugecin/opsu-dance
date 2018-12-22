/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;
import itdelatrisu.opsu.ui.animations.AnimationEquation;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import yugecin.opsudance.core.input.*;
import yugecin.opsudance.core.state.BaseOpsuState;

import static itdelatrisu.opsu.GameImage.*;
import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

/**
 * "Game Pause/Fail" state.
 * <p>
 * Players are able to continue the game (if applicable), retry the beatmap,
 * or return to the song menu from this state.
 */
public class GamePauseMenu extends BaseOpsuState {

	private MenuButton continueButton, retryButton, backButton;

	@Override
	public void render(Graphics g) {
		// get background image
		GameImage bg = (gameState.getRestart() == Game.Restart.LOSE) ?
				GameImage.FAIL_BACKGROUND : GameImage.PAUSE_OVERLAY;

		// don't draw default background if button skinned and background unskinned
		boolean buttonsSkinned =
			GameImage.PAUSE_CONTINUE.hasBeatmapSkinImage() ||
			GameImage.PAUSE_RETRY.hasBeatmapSkinImage() ||
			GameImage.PAUSE_BACK.hasBeatmapSkinImage();
		if (!buttonsSkinned || bg.hasBeatmapSkinImage())
			bg.getImage().draw();
		else
			g.setBackground(Color.black);

		// draw buttons
		if (gameState.getRestart() != Game.Restart.LOSE)
			continueButton.draw();
		retryButton.draw();
		backButton.draw();
	}

	@Override
	public void preRenderUpdate() {
		int delta = renderDelta;
		UI.update(delta);
		continueButton.hoverUpdate(delta, mouseX, mouseY);
		retryButton.hoverUpdate(delta, mouseX, mouseY);
		backButton.hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		// game keys
		if (!Keyboard.isRepeatEvent()) {
			if (e.keyCode == OPTION_KEY_LEFT.keycode) {
				this.onClick(mouseX, mouseY);
			} else if (e.keyCode == OPTION_KEY_RIGHT.keycode) {
				this.onClick(mouseX, mouseY);
			}
		}

		if (e.keyCode == KEY_ESCAPE) {
			// 'esc' will normally unpause, but will return to song menu if health is zero
			if (gameState.getRestart() == Game.Restart.LOSE) {
				SoundController.playSound(SoundEffect.MENUBACK);
				songMenuState.resetGameDataOnLoad();
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
				displayContainer.switchState(songMenuState);
			} else {
				SoundController.playSound(SoundEffect.MENUBACK);
				gameState.setRestart(Game.Restart.FALSE);
				displayContainer.switchState(gameState);
			}
		}

		if (e.keyCode == KEY_R && input.isControlDown()) {
			gameState.setRestart(Game.Restart.MANUAL);
			displayContainer.switchState(gameState);
		}

		if (e.keyCode == KEY_SUBTRACT || e.keyCode == KEY_MINUS) {
			gameState.adjustLocalMusicOffset(-5);
			return;
		}
		if (e.keyCode == KEY_EQUALS || e.keyCode == KEY_ADD || e.chr == '+') {
			gameState.adjustLocalMusicOffset(5);
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.button != Input.MMB) {
			this.onClick(e.x, e.y);
		}
	}

	private void onClick(int x, int y)
	{
		boolean loseState = (gameState.getRestart() == Game.Restart.LOSE);
		if (continueButton.contains(x, y) && !loseState) {
			SoundController.playSound(SoundEffect.MENUBACK);
			gameState.setRestart(Game.Restart.FALSE);
			displayContainer.switchState(gameState);
		} else if (retryButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			gameState.setRestart(Game.Restart.MANUAL);
			displayContainer.switchState(gameState);
		} else if (backButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			songMenuState.resetGameDataOnLoad();
			if (loseState)
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
			else
				MusicController.resume();
			if (displayContainer.cursor.isBeatmapSkinned()) {
				displayContainer.cursor.reset();
			}
			MusicController.setPitch(1.0f);
			displayContainer.switchState(songMenuState);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (!OPTION_DISABLE_MOUSE_WHEEL.state) {
			volumeControl.changeVolume(e.delta);
		}
	}

	@Override
	public void enter() {
		super.enter();

		UI.enter();
		MusicController.pause();
		continueButton.resetHover();
		retryButton.resetHover();
		backButton.resetHover();
	}

	@Override
	public boolean onCloseRequest() {
		songMenuState.resetTrackOnLoad();
		songMenuState.resetGameDataOnLoad();
		displayContainer.switchState(songMenuState);
		return false;
	}

	/**
	 * Loads all game pause/fail menu images.
	 */
	public void loadImages() {
		// initialize buttons
		continueButton = new MenuButton(PAUSE_CONTINUE.getImage(), width2, height * 0.25f);
		retryButton = new MenuButton(PAUSE_RETRY.getImage(), width2, height2);
		backButton = new MenuButton(PAUSE_BACK.getImage(), width2, height * 0.75f);
		final int buttonAnimationDuration = 300;
		continueButton.setHoverAnimationDuration(buttonAnimationDuration);
		retryButton.setHoverAnimationDuration(buttonAnimationDuration);
		backButton.setHoverAnimationDuration(buttonAnimationDuration);
		final AnimationEquation buttonAnimationEquation = AnimationEquation.IN_OUT_BACK;
		continueButton.setHoverAnimationEquation(buttonAnimationEquation);
		retryButton.setHoverAnimationEquation(buttonAnimationEquation);
		backButton.setHoverAnimationEquation(buttonAnimationEquation);
		continueButton.setHoverExpand();
		retryButton.setHoverExpand();
		backButton.setHoverExpand();
	}

}
