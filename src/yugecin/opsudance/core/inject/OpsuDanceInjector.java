/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
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
 *
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.core.inject;

import itdelatrisu.opsu.beatmap.BeatmapParser;
import itdelatrisu.opsu.beatmap.OszUnpacker;
import itdelatrisu.opsu.downloads.Updater;
import itdelatrisu.opsu.replay.ReplayImporter;
import itdelatrisu.opsu.states.*;
import yugecin.opsudance.PreStartupInitializer;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.state.specialstates.BarNotificationState;
import yugecin.opsudance.core.state.specialstates.BubbleNotificationState;
import yugecin.opsudance.core.state.specialstates.FpsRenderState;
import yugecin.opsudance.core.errorhandling.ErrorHandler;
import yugecin.opsudance.options.Configuration;
import yugecin.opsudance.options.OptionsService;
import yugecin.opsudance.render.GameObjectRenderer;
import yugecin.opsudance.skinning.SkinService;

public class OpsuDanceInjector extends Injector {

	protected void configure() {
		bind(Configuration.class).asEagerSingleton();

		bind(OptionsService.class).asLazySingleton();
		bind(ReplayImporter.class).asLazySingleton();
		bind(OszUnpacker.class).asLazySingleton();
		bind(BeatmapParser.class).asLazySingleton();
		bind(Updater.class).asLazySingleton();
		bind(SkinService.class).asEagerSingleton();

		bind(PreStartupInitializer.class).asEagerSingleton();
		bind(DisplayContainer.class).asEagerSingleton();

		bind(ErrorHandler.class).asEagerSingleton();

		bind(FpsRenderState.class).asEagerSingleton();
		bind(BarNotificationState.class).asEagerSingleton();
		bind(BubbleNotificationState.class).asEagerSingleton();

		bind(GameObjectRenderer.class).asEagerSingleton();

		bind(Splash.class).asEagerSingleton();
		bind(MainMenu.class).asEagerSingleton();
		bind(ButtonMenu.class).asEagerSingleton();
		bind(SongMenu.class).asEagerSingleton();
		bind(DownloadsMenu.class).asEagerSingleton();
		bind(Game.class).asEagerSingleton();
		bind(GameRanking.class).asEagerSingleton();
		bind(GamePauseMenu.class).asEagerSingleton();
	}

}
