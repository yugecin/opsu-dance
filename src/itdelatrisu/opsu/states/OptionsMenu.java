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

import yugecin.opsudance.ui.OptionsOverlay.OptionTab;

public class OptionsMenu {

	public static final OptionTab[] normalOptions = new OptionTab[] {
		new OptionTab("GENERAL", null),
		new OptionTab("GENERAL", new GameOption[]{
			GameOption.DISABLE_UPDATER,
			GameOption.ENABLE_WATCH_SERVICE
		}),
		new OptionTab("LANGUAGE", new GameOption[]{
			GameOption.SHOW_UNICODE,
		}),
		new OptionTab("GRAPHICS", null),
		new OptionTab("RENDERER", new GameOption[] {
			GameOption.SCREEN_RESOLUTION,
			GameOption.ALLOW_LARGER_RESOLUTIONS,
			GameOption.FULLSCREEN,
			// TODO d: UPS option
			GameOption.TARGET_FPS,
			GameOption.SHOW_FPS,
			GameOption.SCREENSHOT_FORMAT,
		}),
		new OptionTab("SLIDER OPTIONS", new GameOption[]{
			GameOption.SNAKING_SLIDERS,
			GameOption.FALLBACK_SLIDERS,
			GameOption.SHRINKING_SLIDERS,
			GameOption.MERGING_SLIDERS,
			//GameOption.MERGING_SLIDERS_MIRROR_POOL,
			GameOption.DRAW_SLIDER_ENDCIRCLES,
		}),
		new OptionTab("SKIN", null),
		new OptionTab("SKIN", new GameOption[]{
			GameOption.SKIN,
			GameOption.IGNORE_BEATMAP_SKINS,
			GameOption.DYNAMIC_BACKGROUND,
			GameOption.LOAD_HD_IMAGES,
			GameOption.LOAD_VERBOSE,
			GameOption.COLOR_MAIN_MENU_LOGO,
		}),
		new OptionTab("CURSOR", new GameOption[]{
			GameOption.CURSOR_SIZE,
			GameOption.NEW_CURSOR,
			GameOption.DISABLE_CURSOR
			// TODO use combo colour as tint for slider ball option
		}),
		new OptionTab("AUDIO", null),
		new OptionTab("VOLUME", new GameOption[]{
			GameOption.MASTER_VOLUME,
			GameOption.MUSIC_VOLUME,
			GameOption.EFFECT_VOLUME,
			GameOption.HITSOUND_VOLUME,
			GameOption.SAMPLE_VOLUME_OVERRIDE,
		}),
		new OptionTab("MISC", new GameOption[] {
			GameOption.MUSIC_OFFSET,
			GameOption.DISABLE_SOUNDS,
			GameOption.ENABLE_THEME_SONG
		}),
		new OptionTab("GAMEPLAY", null),
		new OptionTab("GENERAL", new GameOption[] {
			GameOption.BACKGROUND_DIM,
			GameOption.FORCE_DEFAULT_PLAYFIELD,
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
		new OptionTab("INPUT", null),
		new OptionTab("KEY MAPPING", new GameOption[]{
			GameOption.KEY_LEFT,
			GameOption.KEY_RIGHT,
		}),
		new OptionTab("MOUSE", new GameOption[] {
			GameOption.DISABLE_MOUSE_WHEEL,
			GameOption.DISABLE_MOUSE_BUTTONS,
		}),
		new OptionTab("CUSTOM", null),
		new OptionTab("DIFFICULTY", new GameOption[]{
			GameOption.FIXED_CS,
			GameOption.FIXED_HP,
			GameOption.FIXED_AR,
			GameOption.FIXED_OD,
		}),
		new OptionTab("MISC", new GameOption[] {
			GameOption.CHECKPOINT,
			GameOption.REPLAY_SEEKING,
		}),
		new OptionTab("DANCE", null),
		new OptionTab("MOVER", new GameOption[]{
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
		}),
		new OptionTab("SLIDER OPTIONS", new GameOption[]{
			GameOption.DANCE_LAZY_SLIDERS,
			GameOption.DANCE_CIRLCE_IN_SLOW_SLIDERS,
			GameOption.DANCE_CIRLCE_IN_LAZY_SLIDERS,
		}),
		new OptionTab("CIRCLE MOVEMENTS", new GameOption[]{
			GameOption.DANCE_CIRCLE_STREAMS,
			GameOption.DANCE_ONLY_CIRCLE_STACKS,
		}),
		new OptionTab("MIRROR", new GameOption[] {
			GameOption.DANCE_MIRROR,
		}),
		new OptionTab("ADVANCED DISPLAY", null),
		new OptionTab("OBJECTS", new GameOption[]{
			GameOption.DANCE_DRAW_APPROACH,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE,
			GameOption.DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED,
			GameOption.DANCE_RGB_OBJECT_INC,
			GameOption.DANCE_HIDE_OBJECTS,
		}),
		new OptionTab("CURSOR", new GameOption[]{
			GameOption.DANCE_CURSOR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_MIRROR_COLOR_OVERRIDE,
			GameOption.DANCE_CURSOR_ONLY_COLOR_TRAIL,
			GameOption.DANCE_RGB_CURSOR_INC,
			GameOption.DANCE_CURSOR_TRAIL_OVERRIDE,
		}),
		new OptionTab("MISC", new GameOption[] {
			GameOption.DANCE_HIDE_UI,
			GameOption.DANCE_REMOVE_BG,
			GameOption.DANCE_ENABLE_SB,
		}),
		new OptionTab ("PIPPI", null),
		new OptionTab ("GENERAL", new GameOption[]{
			GameOption.PIPPI_ENABLE,
			GameOption.PIPPI_RADIUS_PERCENT,
		}),
		new OptionTab ("ANGLE MULTIPLIERS", new GameOption[]{
			GameOption.PIPPI_ANGLE_INC_MUL,
			GameOption.PIPPI_ANGLE_INC_MUL_SLIDER,
		}),
		new OptionTab ("MISC", new GameOption[] {
			GameOption.PIPPI_SLIDER_FOLLOW_EXPAND,
			GameOption.PIPPI_PREVENT_WOBBLY_STREAMS,
		})
	};

	public static final OptionTab[] storyboardOptions = new OptionTab[] {
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
