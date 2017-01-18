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
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.core.inject;

import itdelatrisu.opsu.states.ButtonMenu;
import itdelatrisu.opsu.states.MainMenu;
import itdelatrisu.opsu.states.SongMenu;
import itdelatrisu.opsu.states.Splash;
import yugecin.opsudance.PreStartupInitializer;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.events.EventBus;
import yugecin.opsudance.core.state.specialstates.BarNotificationState;
import yugecin.opsudance.core.state.specialstates.BubbleNotificationState;
import yugecin.opsudance.core.state.specialstates.FpsRenderState;
import yugecin.opsudance.core.state.transitions.EmptyTransitionState;
import yugecin.opsudance.core.state.transitions.FadeInTransitionState;
import yugecin.opsudance.core.state.transitions.FadeOutTransitionState;
import yugecin.opsudance.core.errorhandling.ErrorHandler;
import yugecin.opsudance.states.EmptyRedState;
import yugecin.opsudance.states.EmptyState;

public class OpsuDanceInjector extends Injector {

	protected void configure() {
		bind(EventBus.class).asEagerSingleton();

		bind(PreStartupInitializer.class).asEagerSingleton();
		bind(DisplayContainer.class).asEagerSingleton();

		bind(ErrorHandler.class).asEagerSingleton();

		bind(FpsRenderState.class).asEagerSingleton();
		bind(BarNotificationState.class).asEagerSingleton();
		bind(BubbleNotificationState.class).asEagerSingleton();

		bind(EmptyTransitionState.class).asEagerSingleton();
		bind(FadeInTransitionState.class).asEagerSingleton();
		bind(FadeOutTransitionState.class).asEagerSingleton();

		bind(EmptyRedState.class).asEagerSingleton();
		bind(EmptyState.class).asEagerSingleton();

		bind(Splash.class).asEagerSingleton();
		bind(MainMenu.class).asEagerSingleton();
		bind(ButtonMenu.class).asEagerSingleton();
		bind(SongMenu.class).asEagerSingleton();
	}

}
