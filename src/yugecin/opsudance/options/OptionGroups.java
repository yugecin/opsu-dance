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

package yugecin.opsudance.options;

import itdelatrisu.opsu.GameImage;

import static yugecin.opsudance.options.Options.*;

public class OptionGroups {

	public static final OptionTab[] normalOptions = new OptionTab[] {
		new OptionTab("General", GameImage.MENU_NAV_GENERAL),
		new OptionTab("GENERAL", new Option[]{
			OPTION_DISABLE_UPDATER,
			OPTION_ENABLE_WATCH_SERVICE
		}),
		new OptionTab("LANGUAGE", new Option[]{
			OPTION_SHOW_UNICODE,
		}),
		new OptionTab("Graphics", GameImage.MENU_NAV_GRAPHICS),
		new OptionTab("RENDERER", new Option[] {
			OPTION_SCREEN_RESOLUTION,
			OPTION_ALLOW_LARGER_RESOLUTIONS,
			OPTION_FULLSCREEN,
			OPTION_TARGET_UPS,
			OPTION_TARGET_FPS,
			OPTION_SHOW_FPS,
			OPTION_USE_FPS_DELTAS,
			OPTION_STARFOUNTAINS,
			OPTION_SCREENSHOT_FORMAT,
		}),
		new OptionTab("SLIDER OPTIONS", new Option[]{
			OPTION_SNAKING_SLIDERS,
			OPTION_FALLBACK_SLIDERS,
			OPTION_SHRINKING_SLIDERS,
			OPTION_MERGING_SLIDERS,
			OPTION_MERGING_SLIDERS_MIRROR_POOL,
			OPTION_DRAW_SLIDER_ENDCIRCLES,
		}),
		new OptionTab("DANCING HITCIRCLES", new Option[] {
			OPTION_DANCING_CIRCLES,
			OPTION_DANCING_CIRCLES_MULTIPLIER,
		}),
		new OptionTab("Skin", GameImage.MENU_NAV_SKIN),
		new OptionTab("SKIN", new Option[]{
			OPTION_SKIN,
			OPTION_IGNORE_BEATMAP_SKINS,
			OPTION_DYNAMIC_BACKGROUND,
			OPTION_LOAD_HD_IMAGES,
			OPTION_LOAD_VERBOSE,
			OPTION_COLOR_MAIN_MENU_LOGO,
		}),
		new OptionTab("CURSOR", new Option[]{
			OPTION_CURSOR_SIZE,
			OPTION_NEW_CURSOR,
			OPTION_DISABLE_CURSOR
			// TODO use combo colour as tint for slider ball option
		}),
		new OptionTab("Audio", GameImage.MENU_NAV_AUDIO),
		new OptionTab("VOLUME", new Option[]{
			OPTION_MASTER_VOLUME,
			OPTION_MUSIC_VOLUME,
			OPTION_EFFECT_VOLUME,
			OPTION_HITSOUND_VOLUME,
			OPTION_SAMPLE_VOLUME_OVERRIDE,
		}),
		new OptionTab("MISC", new Option[] {
			OPTION_MUSIC_OFFSET,
			OPTION_DISABLE_SOUNDS,
			OPTION_ENABLE_THEME_SONG
		}),
		new OptionTab("Gameplay", GameImage.MENU_NAV_GAMEPLAY),
		new OptionTab("GENERAL", new Option[] {
			OPTION_BACKGROUND_DIM,
			OPTION_FORCE_DEFAULT_PLAYFIELD,
			OPTION_SHOW_HIT_LIGHTING,
			OPTION_SHOW_HIT_ANIMATIONS,
			OPTION_SHOW_COMBO_BURSTS,
			OPTION_SHOW_PERFECT_HIT,
			OPTION_SHOW_FOLLOW_POINTS,
			OPTION_SHOW_HIT_ERROR_BAR,
			OPTION_MAP_START_DELAY,
			OPTION_MAP_END_DELAY,
			OPTION_EPILEPSY_WARNING,
		}),
		new OptionTab("Input", GameImage.MENU_NAV_INPUT),
		new OptionTab("KEY MAPPING", new Option[]{
			OPTION_KEY_LEFT,
			OPTION_KEY_RIGHT,
		}),
		new OptionTab("MOUSE", new Option[] {
			OPTION_DISABLE_MOUSE_WHEEL,
			OPTION_DISABLE_MOUSE_BUTTONS,
		}),
		new OptionTab("Custom", GameImage.MENU_NAV_CUSTOM),
		new OptionTab("DIFFICULTY", new Option[]{
			OPTION_FIXED_CS,
			OPTION_FIXED_HP,
			OPTION_FIXED_AR,
			OPTION_FIXED_OD,
		}),
		new OptionTab("MISC", new Option[] {
			OPTION_CHECKPOINT,
			OPTION_REPLAY_SEEKING,
		}),
		new OptionTab("Dance", GameImage.MENU_NAV_DANCE),
		new OptionTab("MOVER", new Option[]{
			OPTION_DANCE_MOVER,
			OPTION_DANCE_EXGON_DELAY,
			OPTION_DANCE_QUAD_BEZ_AGGRESSIVENESS,
			OPTION_DANCE_QUAD_BEZ_SLIDER_AGGRESSIVENESS_FACTOR,
			OPTION_DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS,
			OPTION_DANCE_QUAD_BEZ_CUBIC_AGGRESSIVENESS_FACTOR,
			OPTION_DANCE_MOVER_DIRECTION,
			OPTION_DANCE_SLIDER_MOVER_TYPE,
		}),
		new OptionTab("SPINNER", new Option[]{
			OPTION_DANCE_SPINNER,
			OPTION_DANCE_SPINNER_DELAY,
		}),
		new OptionTab("SLIDER OPTIONS", new Option[]{
			OPTION_DANCE_LAZY_SLIDERS,
			OPTION_DANCE_CIRLCE_IN_SLOW_SLIDERS,
			OPTION_DANCE_CIRLCE_IN_LAZY_SLIDERS,
		}),
		new OptionTab("CIRCLE MOVEMENTS", new Option[]{
			OPTION_DANCE_CIRCLE_STREAMS,
			OPTION_DANCE_ONLY_CIRCLE_STACKS,
		}),
		new OptionTab("MIRROR", new Option[] {
			OPTION_DANCE_MIRROR,
		}),
		new OptionTab("Advanced Display", GameImage.MENU_NAV_ADVANCED),
		new OptionTab("OBJECTS", new Option[]{
			OPTION_DANCE_DRAW_APPROACH,
			OPTION_DANCE_OBJECT_COLOR_OVERRIDE,
			OPTION_DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED,
			OPTION_DANCE_RGB_OBJECT_INC,
			OPTION_DANCE_HIDE_OBJECTS,
		}),
		new OptionTab("CURSOR", new Option[]{
			OPTION_DANCE_CURSOR_COLOR_OVERRIDE,
			OPTION_DANCE_CURSOR_MIRROR_COLOR_OVERRIDE,
			OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL,
			OPTION_DANCE_RGB_CURSOR_INC,
			OPTION_DANCE_CURSOR_TRAIL_OVERRIDE,
		}),
		new OptionTab("MISC", new Option[] {
			OPTION_DANCE_HIDE_UI,
			OPTION_DANCE_REMOVE_BG,
			OPTION_DANCE_ENABLE_SB,
		}),
		new OptionTab ("Pippi", GameImage.MENU_NAV_PIPPI),
		new OptionTab ("GENERAL", new Option[]{
			OPTION_PIPPI_ENABLE,
			OPTION_PIPPI_RADIUS_PERCENT,
		}),
		new OptionTab ("ANGLE MULTIPLIERS", new Option[]{
			OPTION_PIPPI_ANGLE_INC_MUL,
			OPTION_PIPPI_ANGLE_INC_MUL_SLIDER,
		}),
		new OptionTab ("MISC", new Option[] {
			OPTION_PIPPI_SLIDER_FOLLOW_EXPAND,
			OPTION_PIPPI_PREVENT_WOBBLY_STREAMS,
		})
	};

	public static final OptionTab[] storyboardOptions = new OptionTab[] {
		new OptionTab("Gameplay", GameImage.MENU_NAV_GAMEPLAY),
		new OptionTab("GENERAL", new Option[] {
			OPTION_BACKGROUND_DIM,
			OPTION_DANCE_REMOVE_BG,
			OPTION_SNAKING_SLIDERS,
			OPTION_SHRINKING_SLIDERS,
			OPTION_SHOW_HIT_LIGHTING,
			OPTION_SHOW_HIT_ANIMATIONS,
			OPTION_SHOW_COMBO_BURSTS,
			OPTION_SHOW_PERFECT_HIT,
			OPTION_SHOW_FOLLOW_POINTS,
		}),
		new OptionTab("Input", GameImage.MENU_NAV_INPUT),
		new OptionTab("INPUT", new Option[] {
			OPTION_CURSOR_SIZE,
			OPTION_NEW_CURSOR,
			OPTION_DISABLE_CURSOR
		}),
		new OptionTab("Dance", GameImage.MENU_NAV_DANCE),
		new OptionTab("MOVER", new Option[]{
			OPTION_DANCE_MOVER,
			OPTION_DANCE_EXGON_DELAY,
			OPTION_DANCE_QUAD_BEZ_AGGRESSIVENESS,
			OPTION_DANCE_QUAD_BEZ_SLIDER_AGGRESSIVENESS_FACTOR,
			OPTION_DANCE_QUAD_BEZ_USE_CUBIC_ON_SLIDERS,
			OPTION_DANCE_QUAD_BEZ_CUBIC_AGGRESSIVENESS_FACTOR,
			OPTION_DANCE_MOVER_DIRECTION,
			OPTION_DANCE_SLIDER_MOVER_TYPE,
		}),
		new OptionTab("SPINNER", new Option[]{
			OPTION_DANCE_SPINNER,
			OPTION_DANCE_SPINNER_DELAY,
		}),
		new OptionTab("SLIDER OPTIONS", new Option[]{
			OPTION_DANCE_LAZY_SLIDERS,
			OPTION_DANCE_CIRLCE_IN_SLOW_SLIDERS,
			OPTION_DANCE_CIRLCE_IN_LAZY_SLIDERS,
		}),
		new OptionTab("CIRCLE MOVEMENTS", new Option[]{
			OPTION_DANCE_CIRCLE_STREAMS,
			OPTION_DANCE_ONLY_CIRCLE_STACKS,
		}),
		new OptionTab("MIRROR", new Option[] {
			OPTION_DANCE_MIRROR,
		}),
		new OptionTab("Advanced Display", GameImage.MENU_NAV_ADVANCED),
		new OptionTab("OBJECTS", new Option[]{
			OPTION_DANCE_DRAW_APPROACH,
			OPTION_DANCE_OBJECT_COLOR_OVERRIDE,
			OPTION_DANCE_OBJECT_COLOR_OVERRIDE_MIRRORED,
			OPTION_DANCE_RGB_OBJECT_INC,
			OPTION_DANCE_HIDE_OBJECTS,
		}),
		new OptionTab("CURSOR", new Option[]{
			OPTION_DANCE_CURSOR_COLOR_OVERRIDE,
			OPTION_DANCE_CURSOR_MIRROR_COLOR_OVERRIDE,
			OPTION_DANCE_CURSOR_ONLY_COLOR_TRAIL,
			OPTION_DANCE_RGB_CURSOR_INC,
			OPTION_DANCE_CURSOR_TRAIL_OVERRIDE,
		}),
		new OptionTab("MISC", new Option[] {
			OPTION_DANCE_HIDE_UI,
			OPTION_DANCE_REMOVE_BG,
			OPTION_DANCE_ENABLE_SB,
		}),
		new OptionTab ("Pippi", GameImage.MENU_NAV_PIPPI),
		new OptionTab ("GENERAL", new Option[]{
			OPTION_PIPPI_ENABLE,
			OPTION_PIPPI_RADIUS_PERCENT,
		}),
		new OptionTab ("ANGLE MULTIPLIERS", new Option[]{
			OPTION_PIPPI_ANGLE_INC_MUL,
			OPTION_PIPPI_ANGLE_INC_MUL_SLIDER,
		}),
		new OptionTab ("MISC", new Option[] {
			OPTION_PIPPI_SLIDER_FOLLOW_EXPAND,
			OPTION_PIPPI_PREVENT_WOBBLY_STREAMS,
		}),
	};

}
