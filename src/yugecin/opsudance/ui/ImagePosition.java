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

import java.awt.geom.Rectangle2D;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;

public class ImagePosition extends Rectangle2D.Float {
	
	private Image image;
	
	public ImagePosition(Image image) {
		this.image = image;
	}
	
	public boolean contains(int x, int y, float alphaThreshold) {
		if (!super.contains(x, y)) {
			return false;
		}
		final int ix = x - (int) this.x;
		final int iy = y - (int) this.y;
		return this.image.getAlphaAt(ix, iy) > alphaThreshold;
	}
	
	public float middleX() {
		return this.x + this.width / 2;
	}
	
	public float middleY() {
		return this.y + this.height / 2;
	}
	
	public void scale(float scale) {
		final float width = this.width * scale;
		final float height = this.height * scale;
		this.x -= (width - this.width) / 2f;
		this.y -= (height - this.height) / 2f;
		this.width = width;
		this.height = height;
	}

	public void draw(Color filter) {
		this.image.draw(this.x, this.y, this.width, this.height, filter);
	}
}
