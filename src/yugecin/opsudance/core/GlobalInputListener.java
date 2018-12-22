// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

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
				input.isControlDown() && !displayContainer.isIn(gameState)) {
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
