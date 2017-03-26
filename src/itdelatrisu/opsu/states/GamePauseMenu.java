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
import org.newdawn.slick.Input;
import yugecin.opsudance.core.inject.Inject;
import yugecin.opsudance.core.inject.InstanceContainer;
import yugecin.opsudance.core.state.BaseOpsuState;

import static yugecin.opsudance.options.Options.*;

/**
 * "Game Pause/Fail" state.
 * <p>
 * Players are able to continue the game (if applicable), retry the beatmap,
 * or return to the song menu from this state.
 */
public class GamePauseMenu extends BaseOpsuState {

	@Inject
	private InstanceContainer instanceContainer;

	@Inject
	private Game gameState;

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

		UI.draw(g);
	}

	@Override
	public void preRenderUpdate() {
		int delta = displayContainer.renderDelta;
		UI.update(delta);
		continueButton.hoverUpdate(delta, displayContainer.mouseX, displayContainer.mouseY);
		retryButton.hoverUpdate(delta, displayContainer.mouseX, displayContainer.mouseY);
		backButton.hoverUpdate(delta, displayContainer.mouseX, displayContainer.mouseY);
	}

	@Override
	public boolean keyPressed(int key, char c) {
		if (super.keyPressed(key, c)) {
			return true;
		}

		// game keys
		if (!Keyboard.isRepeatEvent()) {
			if (key == OPTION_KEY_LEFT.intval) {
				mousePressed(Input.MOUSE_LEFT_BUTTON, displayContainer.mouseX, displayContainer.mouseY);
			} else if (key == OPTION_KEY_RIGHT.intval) {
				mousePressed(Input.MOUSE_RIGHT_BUTTON, displayContainer.mouseX, displayContainer.mouseY);
			}
		}

		if (key == Input.KEY_ESCAPE) {
			// 'esc' will normally unpause, but will return to song menu if health is zero
			if (gameState.getRestart() == Game.Restart.LOSE) {
				SoundController.playSound(SoundEffect.MENUBACK);
				instanceContainer.provide(SongMenu.class).resetGameDataOnLoad();
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
				displayContainer.switchState(SongMenu.class);
			} else {
				SoundController.playSound(SoundEffect.MENUBACK);
				gameState.setRestart(Game.Restart.FALSE);
				displayContainer.switchState(Game.class);
			}
			return true;
		}

		if (key == Input.KEY_R && (displayContainer.input.isKeyDown(Input.KEY_RCONTROL) || displayContainer.input.isKeyDown(Input.KEY_LCONTROL))) {
			gameState.setRestart(Game.Restart.MANUAL);
			displayContainer.switchState(Game.class);
			return true;
		}

		return false;
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		if (super.mousePressed(button, x, y)) {
			return true;
		}

		if (button == Input.MOUSE_MIDDLE_BUTTON) {
			return true;
		}

		boolean loseState = (gameState.getRestart() == Game.Restart.LOSE);
		if (continueButton.contains(x, y) && !loseState) {
			SoundController.playSound(SoundEffect.MENUBACK);
			gameState.setRestart(Game.Restart.FALSE);
			displayContainer.switchState(Game.class);
		} else if (retryButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			gameState.setRestart(Game.Restart.MANUAL);
			displayContainer.switchState(Game.class);
		} else if (backButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			instanceContainer.provide(SongMenu.class).resetGameDataOnLoad();
			if (loseState)
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
			else
				MusicController.resume();
			if (displayContainer.cursor.isBeatmapSkinned()) {
				displayContainer.resetCursor();
			}
			MusicController.setPitch(1.0f);
			displayContainer.switchState(SongMenu.class);
		}

		return true;
	}

	@Override
	public boolean mouseWheelMoved(int newValue) {
		if (super.mouseWheelMoved(newValue)) {
			return true;
		}

		if (OPTION_DISABLE_MOUSE_WHEEL.state) {
			return true;
		}

		UI.changeVolume((newValue < 0) ? -1 : 1);
		return true;
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
		SongMenu songmenu = instanceContainer.provide(SongMenu.class);
		songmenu.resetTrackOnLoad();
		songmenu.resetGameDataOnLoad();
		displayContainer.switchState(SongMenu.class);
		return false;
	}

	/**
	 * Loads all game pause/fail menu images.
	 */
	public void loadImages() {
		// initialize buttons
		continueButton = new MenuButton(GameImage.PAUSE_CONTINUE.getImage(), displayContainer.width / 2f, displayContainer.height * 0.25f);
		retryButton = new MenuButton(GameImage.PAUSE_RETRY.getImage(), displayContainer.width / 2f, displayContainer.height * 0.5f);
		backButton = new MenuButton(GameImage.PAUSE_BACK.getImage(), displayContainer.width / 2f, displayContainer.height * 0.75f);
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
