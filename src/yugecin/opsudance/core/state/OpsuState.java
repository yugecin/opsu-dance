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
package yugecin.opsudance.core.state;

import org.newdawn.slick.Graphics;
import yugecin.opsudance.core.errorhandling.ErrorDumpable;

public interface OpsuState extends ErrorDumpable {

	void update();
	void preRenderUpdate();
	void render(Graphics g);
	void enter();
	void leave();

	/**
	 * @return true if closing is allowed
	 */
	boolean onCloseRequest();

	/**
	 * @return false to stop event bubbling
	 */
	boolean keyPressed(int key, char c);

	/**
	 * @return false to stop event bubbling
	 */
	boolean keyReleased(int key, char c);

	/**
	 * @return false to stop event bubbling
	 */
	boolean mouseWheelMoved(int delta);

	/**
	 * @return false to stop event bubbling
	 */
	boolean mousePressed(int button, int x, int y);

	/**
	 * @return false to stop event bubbling
	 */
	boolean mouseReleased(int button, int x, int y);

	/**
	 * @return false to stop event bubbling
	 */
	boolean mouseDragged(int oldx, int oldy, int newx, int newy);

}
