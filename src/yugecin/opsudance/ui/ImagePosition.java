/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2018 yugecin
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

import java.awt.Rectangle;

import org.newdawn.slick.Image;

public class ImagePosition extends Rectangle {
	
	private Image image;
	
	public ImagePosition(Image image) {
		this.image = image;
	}
	
	public boolean contains(int x, int y, float alphaThreshold) {
		if (!super.contains(x, y)) {
			return false;
		}
		final int ix = x - this.x;
		final int iy = y - this.y;
		return this.image.getAlphaAt(ix, iy) > alphaThreshold;
	}

}
