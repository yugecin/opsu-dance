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

import itdelatrisu.opsu.Options.GameOption;

import yugecin.opsudance.ui.OptionsOverlay;
import yugecin.opsudance.ui.OptionsOverlay.OptionTab;

public class OptionsMenu {

	public static final OptionTab[] normalOptions = new OptionsOverlay.OptionTab[]{
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
			//GameOption.MERGING_SLIDERS_MIRROR_POOL,
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
			GameOption.DANCE_EXGON_DELAY,
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

	public static final OptionTab[] storyboardOptions = new OptionsOverlay.OptionTab[]{
		new OptionTab("Gameplay", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.DANCE_REMOVE_BG,
			GameOption.SNAKING_SLIDERS,
			GameOption.SHRINKING_SLIDERS,
			GameOption.SHOW_HIT_LIGHTING,
			GameOption.SHOW_HIT_ANIMATIONS,
			GameOption.SHOW_COMBO_BURSTS,
			GameOption.SHOW_PERFECT_HIT,
			GameOption.SHOW_FOLLOW_POINTS,
		}),
		new OptionTab("Input", new GameOption[] {
			GameOption.CURSOR_SIZE,
			GameOption.NEW_CURSOR,
			GameOption.DISABLE_CURSOR
		}),
		new OptionTab("Dance", new GameOption[] {
			GameOption.DANCE_MOVER,
			GameOption.DANCE_EXGON_DELAY,
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
			GameOption.DANCE_HIDE_OBJECTS,
			GameOption.DANCE_HIDE_UI,
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

}
