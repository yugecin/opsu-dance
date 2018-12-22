/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017-2018 yugecin
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
package yugecin.opsudance.core;

import itdelatrisu.opsu.states.Game;
import itdelatrisu.opsu.ui.UI;
import yugecin.opsudance.core.input.Input;

import org.newdawn.slick.InputListener;

import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

public class GlobalInputListener implements InputListener {

	@Override
	public boolean keyPressed(int key, char c) {
		return false;
	}

	@Override
	public boolean keyReleased(int key, char c) {
		if (key == KEY_F7) {
			OPTION_TARGET_FPS.clickListItem((targetFPSIndex + 1) % targetFPS.length);
			final String value = OPTION_TARGET_FPS.getValueString();
			barNotifs.sendf("Frame limiter: %s", value);
			return true;
		}
		if (key == KEY_F10) {
			OPTION_DISABLE_MOUSE_BUTTONS.toggle();
			return true;
		}
		if (key == KEY_F12) {
			config.takeScreenShot();
			return true;
		}
		if (key == KEY_S && isKeyDown(KEY_LMENU) && isKeyDown(KEY_LSHIFT) &&
				input.isControlDown() && !displayContainer.isInState(Game.class)) {
			skinservice.reloadSkin();
		}
		return false;
	}

	@Override
	public boolean mouseWheelMoved(int delta) {
		if (input.isAltDown()) {
			volumeControl.changeVolume(delta);
			return true;
		}
		return false;
	}

	@Override
	public boolean mousePressed(int button, int x, int y) {
		return false;
	}

	@Override
	public boolean mouseReleased(int button, int x, int y) {
		return false;
	}

	@Override
	public boolean mouseDragged(int oldx, int oldy, int newx, int newy) {
		return false;
	}

}
