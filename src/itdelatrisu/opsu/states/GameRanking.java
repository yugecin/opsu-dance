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

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.GameMod;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.inject.InstanceContainer;
import yugecin.opsudance.core.state.BaseOpsuState;
import yugecin.opsudance.events.BarNotificationEvent;

/**
 * "Game Ranking" (score card) state.
 * <p>
 * Players are able to view their score statistics, retry the beatmap (if applicable),
 * or watch a replay of the game from this state.
 * </ul>
 */
public class GameRanking extends BaseOpsuState {

	private final InstanceContainer instanceContainer;

	/** Associated GameData object. */
	private GameData data;

	/** "Retry" and "Replay" buttons. */
	private MenuButton retryButton, replayButton;

	/** Button coordinates. */
	private float retryY, replayY;

	public GameRanking(DisplayContainer displayContainer, InstanceContainer instanceContainer) {
		super(displayContainer);
		this.instanceContainer = instanceContainer;

	}

	@Override
	public void revalidate() {
		super.revalidate();

		// buttons
		Image retry = GameImage.PAUSE_RETRY.getImage();
		Image replay = GameImage.PAUSE_REPLAY.getImage();
		replayY = (displayContainer.height * 0.985f) - replay.getHeight() / 2f;
		retryY = replayY - (replay.getHeight() / 2f) - (retry.getHeight() / 1.975f);
		retryButton = new MenuButton(retry, displayContainer.width - (retry.getWidth() / 2f), retryY);
		replayButton = new MenuButton(replay, displayContainer.width - (replay.getWidth() / 2f), replayY);
		retryButton.setHoverFade();
		replayButton.setHoverFade();
	}

	@Override
	public void render(Graphics g) {
		Beatmap beatmap = MusicController.getBeatmap();

		// background
		if (!beatmap.drawBackground(displayContainer.width, displayContainer.height, 0.7f, true)) {
			GameImage.PLAYFIELD.getImage().draw(0, 0);
		}

		// ranking screen elements
		data.drawRankingElements(g, beatmap);

		// buttons
		replayButton.draw();
		if (data.isGameplay() && !GameMod.AUTO.isActive())
			retryButton.draw();
		UI.getBackButton().draw();

		UI.draw(g);

		super.render(g);
	}

	@Override
	public void preRenderUpdate() {
		int delta = displayContainer.renderDelta;
		UI.update(delta);
		replayButton.hoverUpdate(delta, displayContainer.mouseX, displayContainer.mouseY);
		if (data.isGameplay()) {
			retryButton.hoverUpdate(delta, displayContainer.mouseX, displayContainer.mouseY);
		} else {
			MusicController.loopTrackIfEnded(true);
		}
		UI.getBackButton().hoverUpdate(delta, displayContainer.mouseX, displayContainer.mouseY);
	}

	@Override
	public boolean mouseWheelMoved(int newValue) {
		if (displayContainer.input.isKeyDown(Input.KEY_LALT) || displayContainer.input.isKeyDown(Input.KEY_RALT)) {
			UI.changeVolume((newValue < 0) ? -1 : 1);
		}
		return true;
	}

	@Override
	public boolean keyPressed(int key, char c) {
		if (super.keyPressed(key, c)) {
			return true;
		}

		if (key == Input.KEY_ESCAPE) {
			returnToSongMenu();
		}
		return true;
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		if (super.mousePressed(button, x, y)) {
			return true;
		}

		// check mouse button
		if (button == Input.MOUSE_MIDDLE_BUTTON) {
			return false;
		}

		// back to menu
		if (UI.getBackButton().contains(x, y)) {
			returnToSongMenu();
			return true;
		}

		// replay
		Game gameState = instanceContainer.provide(Game.class);
		boolean returnToGame = false;
		boolean replayButtonPressed = replayButton.contains(x, y);
		if (replayButtonPressed && !(data.isGameplay() && GameMod.AUTO.isActive())) {
			Replay r = data.getReplay(null, null);
			if (r != null) {
				try {
					r.load();
					gameState.setReplay(r);
					gameState.setRestart((data.isGameplay()) ? Game.Restart.REPLAY : Game.Restart.NEW);
					returnToGame = true;
				} catch (FileNotFoundException e) {
					EventBus.instance.post(new BarNotificationEvent("Replay file not found."));
				} catch (IOException e) {
					Log.error("Failed to load replay data.", e);
					EventBus.instance.post(new BarNotificationEvent("Failed to load replay data. See log for details."));
				}
			} else
				EventBus.instance.post(new BarNotificationEvent("Replay file not found."));
		}

		// retry
		else if (data.isGameplay() &&
		         (!GameMod.AUTO.isActive() && retryButton.contains(x, y)) ||
		         (GameMod.AUTO.isActive() && replayButtonPressed)) {
			gameState.setReplay(null);
			gameState.setRestart(Game.Restart.MANUAL);
			returnToGame = true;
		}

		if (returnToGame) {
			Beatmap beatmap = MusicController.getBeatmap();
			gameState.loadBeatmap(beatmap);
			SoundController.playSound(SoundEffect.MENUHIT);
			displayContainer.switchState(Game.class);
		}
		return true;
	}

	@Override
	public void enter() {
		super.enter();

		UI.enter();
		if (!data.isGameplay()) {
			if (!MusicController.isTrackDimmed())
				MusicController.toggleTrackDimmed(0.5f);
			replayButton.setY(retryY);
		} else {
			SoundController.playSound(SoundEffect.APPLAUSE);
			retryButton.resetHover();
			replayButton.setY(!GameMod.AUTO.isActive() ? replayY : retryY);
		}
		replayButton.resetHover();
	}

	@Override
	public void leave() {
		super.leave();

		this.data = null;
		if (MusicController.isTrackDimmed()) {
			MusicController.toggleTrackDimmed(1f);
		}
	}

	@Override
	public boolean onCloseRequest() {
		SongMenu songmenu = instanceContainer.provide(SongMenu.class);
		if (data != null && data.isGameplay()) {
			songmenu.resetTrackOnLoad();
		}
		songmenu.resetGameDataOnLoad();
		displayContainer.switchState(SongMenu.class);
		return false;
	}

	/**
	 * Returns to the song menu.
	 */
	private void returnToSongMenu() {
		SoundController.muteSoundComponent();
		SoundController.playSound(SoundEffect.MENUBACK);
		SongMenu songMenu = instanceContainer.provide(SongMenu.class);
		if (data.isGameplay()) {
			songMenu.resetTrackOnLoad();
		}
		songMenu.resetGameDataOnLoad();
		if (displayContainer.cursor.isBeatmapSkinned()) {
			displayContainer.resetCursor();
		}
		displayContainer.switchState(SongMenu.class);
	}

	/**
	 * Sets the associated GameData object.
	 * @param data the GameData
	 */
	public void setGameData(GameData data) { this.data = data; }

	/**
	 * Returns the current GameData object (usually null unless state active).
	 */
	public GameData getGameData() { return data; }
}
