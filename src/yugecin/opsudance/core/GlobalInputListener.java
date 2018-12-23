// Copyright 2017-2018 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import yugecin.opsudance.core.input.*;

import static org.lwjgl.input.Keyboard.*;
import static yugecin.opsudance.core.InstanceContainer.*;
import static yugecin.opsudance.options.Options.*;

public class GlobalInputListener implements InputListener
{
	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		switch (e.keyCode) {
		case KEY_F7:
			OPTION_TARGET_FPS.clickListItem((targetFPSIndex + 1) % targetFPS.length);
			final String value = OPTION_TARGET_FPS.getValueString();
			barNotifs.sendf("Frame limiter: %s", value);
			break;
		case KEY_F10:
			OPTION_DISABLE_MOUSE_BUTTONS.toggle();
			break;
		case KEY_F12:
			config.takeScreenShot();
			break;
		case KEY_S:
			if (isKeyDown(KEY_LMENU) &&
				isKeyDown(KEY_LSHIFT) &&
				input.isControlDown() &&
				!displayContainer.isIn(gameState))
			{
				skinservice.reloadSkin();
			}
		default:
			return;
		}
		e.consume();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (input.isAltDown() || volumeControl.isHovered()) {
			volumeControl.changeVolume(e.direction);
			e.consume();
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (backButton.mouseReleased(e) || bubNotifs.mouseReleased(e)) {
			e.consume();
		}
	}

	@Override
	public void mouseDragged(MouseDragEvent e)
	{
	}
}
