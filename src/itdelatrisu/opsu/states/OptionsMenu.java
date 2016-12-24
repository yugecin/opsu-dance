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
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Options.GameOption;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.ui.UI;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.EmptyTransition;
import yugecin.opsudance.ui.OptionsOverlay;
import yugecin.opsudance.ui.OptionsOverlay.OptionTab;

/**
 * "Game Options" state.
 * <p>
 * Players are able to view and change various game settings in this state.
 */
public class OptionsMenu extends BasicGameState implements OptionsOverlay.Parent {

	/** Option tabs. */
	private static final OptionTab[] options = new OptionsOverlay.OptionTab[]{
		new OptionTab("Display", new GameOption[]{
			GameOption.SCREEN_RESOLUTION,
			GameOption.FULLSCREEN,
			GameOption.ALLOW_LARGER_RESOLUTIONS,
			GameOption.SKIN,
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
			GameOption.SHOW_UNICODE,
			GameOption.SCREENSHOT_FORMAT,
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.LOAD_HD_IMAGES,
			GameOption.LOAD_VERBOSE,
			GameOption.COLOR_MAIN_MENU_LOGO,
		}),
		new OptionTab("Music", new GameOption[] {
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.SAMPLE_VOLUME_OVERRIDE,
			GameOption.MUSIC_OFFSET,
			GameOption.DISABLE_SOUNDS,
			GameOption.ENABLE_THEME_SONG
		}),
		new OptionTab("Gameplay", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.SNAKING_SLIDERS,
			GameOption.SHRINKING_SLIDERS,
			GameOption.FALLBACK_SLIDERS,
			GameOption.MERGING_SLIDERS,
			GameOption.DRAW_SLIDER_ENDCIRCLES,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_HIT_ANIMATIONS,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
			GameOption.SHOW_HIT_ERROR_BAR,
			GameOption.MAP_START_DELAY,
			GameOption.MAP_END_DELAY,
			GameOption.EPILEPSY_WARNING,
		}),
		new OptionTab("Input", new GameOption[] {
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
			GameOption.CURSOR_SIZE,
			GameOption.NEW_CURSOR,
			GameOption.DISABLE_CURSOR
		}),
		new OptionTab("Custom", new GameOption[] {
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
			GameOption.DISABLE_UPDATER,
			GameOption.ENABLE_WATCH_SERVICE
		}),
		new OptionTab("Dance", new GameOption[] {
			GameOption.DANCE_MOVER,
			GameOption.DANCE_QUAD_BEZ_AGGRESSIVENESS,
			GameOption.DANCE_QUAD_BEZ_SLIDER_AGGRESSIVENESS_FACTOR,
			GameOption.DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS,
			GameOption.DANCE_QUAD_BEZ_CUBIC_AGGRESSIVENESS_FACTOR,
			GameOption.DANCE_MOVER_DIRECTION,
			GameOption.DANCE_SLIDER_MOVER_TYPE,
			GameOption.DANCE_SPINNER,
			GameOption.DANCE_SPINNER_DELAY,
			GameOption.DANCE_LAZY_SLIDERS,
			GameOption.DANCE_CIRCLE_STREAMS,
			GameOption.DANCE_ONLY_CIRCLE_STACKS,
			GameOption.DANCE_CIRLCE_IN_SLOW_SLIDERS,
			GameOption.DANCE_CIRLCE_IN_LAZY_SLIDERS,
			GameOption.DANCE_MIRROR,
		}),
		new OptionTab("Dance display", new GameOption[] {
			GameOption.DANCE_DRAW_APPROACH,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED,
			GameOption.DANCE_RGB_OBJECT_INC,
			GameOption.DANCE_CURSOR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_MIRROR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_ONLY_COLOR_TRAIL,
			GameOption.DANCE_RGB_CURSOR_INC,
			GameOption.DANCE_CURSOR_TRAIL_OVERRIDE,
			GameOption.DANCE_REMOVE_BG,
			GameOption.DANCE_HIDE_OBJECTS,
			GameOption.DANCE_HIDE_UI,
			GameOption.DANCE_ENABLE_SB,
			GameOption.DANCE_HIDE_WATERMARK,
		}),
		new OptionTab ("Pippi", new GameOption[] {
			GameOption.PIPPI_ENABLE,
			GameOption.PIPPI_RADIUS_PERCENT,
			GameOption.PIPPI_ANGLE_INC_MUL,
			GameOption.PIPPI_ANGLE_INC_MUL_SLIDER,
			GameOption.PIPPI_SLIDER_FOLLOW_EXPAND,
			GameOption.PIPPI_PREVENT_WOBBLY_STREAMS,
		})
	};

	private StateBasedGame game;
	private Input input;
	private final int state;

	private OptionsOverlay optionsOverlay;

	public OptionsMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.game = game;
		this.input = container.getInput();

		optionsOverlay = new OptionsOverlay(this, options, 5, container);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g) throws SlickException {
		// background
		GameImage.OPTIONS_BG.getImage().draw();

		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		optionsOverlay.render(g, mouseX, mouseY);
		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta) throws SlickException {
		UI.update(delta);
		MusicController.loopTrackIfEnded(false);
		optionsOverlay.update(delta, input.getMouseX(), input.getMouseY());
	}

	@Override
	public int getID() {
		return state;
	}

	@Override
	public void mouseReleased(int button, int x, int y) {
		optionsOverlay.mouseReleased(button, x, y);
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		optionsOverlay.mousePressed(button, x, y);
	}

	@Override
	public void mouseDragged(int oldx, int oldy, int newx, int newy) {
		optionsOverlay.mouseDragged(oldx, oldy, newx, newy);
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		optionsOverlay.mouseWheelMoved(newValue);
	}

	@Override
	public void keyPressed(int key, char c) {
		optionsOverlay.keyPressed(key, c);
	}

	/**
	 * This string is built with option values when entering the options menu.
	 * When leaving the options menu, this string is checked against the new optionstring with the same options.
	 * If those do not match, it means some option has change which requires a restart
	 */
	private String restartOptions;

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		restartOptions = "" + Options.getResolutionIdx() + Options.isFullscreen() + Options.allowLargeResolutions() + Options.getSkinName();
	}

	@Override
	public void leave(GameContainer container, StateBasedGame game) throws SlickException {
		if (!("" + Options.getResolutionIdx() + Options.isFullscreen() + Options.allowLargeResolutions() + Options.getSkinName()).equals(restartOptions)) {
			container.setForceExit(false);
			container.exit();
			return;
		}
		SoundController.playSound(SoundEffect.MENUBACK);
	}

	@Override
	public void onLeave() {
		game.enterState(Opsu.STATE_SONGMENU, new EmptyTransition(), new FadeInTransition());
	}

	@Override
	public void onSaveOption(GameOption option) {

	}

}
