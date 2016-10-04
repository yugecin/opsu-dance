/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
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
package yugecin.opsudance.ui;

import itdelatrisu.opsu.ui.Fonts;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.state.StateBasedGame;
import yugecin.opsudance.Dancer;

public class RWM {

	private int x;
	private int y;
	private int width;
	private int height;
	private int delay;

	private boolean visible;

	public void init(GameContainer container) {
		x = container.getWidth() / 10;
		y = 0;
		width = x * 8;
		height = container.getHeight();
	}

	public boolean isVisible() {
		return visible;
	}

	public void show() {
		visible = true;
		delay = 0;
	}

	public void update(int delta) {
		delay += delta;
	}

	public void render(GameContainer container, StateBasedGame game, Graphics g) {
		g.setColor(Color.black);
		g.fillRect(x, y, width, height);

		int y = 20;
		y = drawCentered(y, "Hi! robin_be here", Color.cyan);
		y = drawCentered(y, "I'm glad you're enjoying this client", Color.white);
		y += 20;
		y = drawCentered(y, "If you want to remove the watermark to make a video, please read following conditions:", Color.white);
		y += 20;
		y = drawCentered(y, "* You will not deny you used opsu!dance to make your video", Color.white);
		y = drawCentered(y, "* You may provide a link to my repository. This is always very appreciated <3.", Color.white);
		y = drawCentered(y, "  Please do keep in mind I only made some adjustements/fixes and added the dancing stuff. opsu! was made by itdelatrisu", Color.white);
		y = drawCentered(y, "* Asking for beatmap requests is discouraged. After all, everyone can download this and see it for themselves.", Color.white);
		y = drawCentered(y, "* YOU WILL NOT PRETEND LIKE YOU MADE/OWN THIS SOFTWARE", Color.red);
		y += 20;
		y = drawCentered(y, "I'll love you if you leave the watermark visible, but I understand that it sucks to have that in a video.", Color.white);
		y = drawCentered(y, "If you decide to hide it, please consider linking the repository in the video's description.", Color.white);
		y += 20;
		if (delay < 10000) {
			y = drawCentered(y, "You can disable the watermark in a few seconds..", Color.white);
		} else {
			y += 50;
			y = drawCentered(y, "Click this black area to disable it", Color.green);
		}
	}

	public int drawCentered(int y, String text, Color color) {
		int textwidth = Fonts.MEDIUM.getWidth(text);
		Fonts.MEDIUM.drawString(x + width / 2 - textwidth / 2, y, text, color);
		return y + Fonts.MEDIUM.getLineHeight() + 5;
	}

	public void mouseReleased(int button, int x, int y) {
		if (x < this.x || x > this.x + width) {
			this.visible = false;
			return;
		}
		if (delay > 10000) {
			Dancer.hidewatermark = true;
			this.visible = false;
		}
	}

	public void keyPressed(int key, char c) {
		if (key == Input.KEY_ESCAPE) {
			visible = false;
		}
	}

}
