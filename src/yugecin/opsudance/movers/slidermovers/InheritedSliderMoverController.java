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
package yugecin.opsudance.movers.slidermovers;

import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;

public class InheritedSliderMoverController implements SliderMoverController {

	private GameObject currentSlider;
	private Circle[] positions;
	private int idx;

	@Override
	public GameObject[] process(GameObject p, GameObject c, int time) {
		GameObject[] ret = new GameObject[2];
		if (!c.isSlider()) {
			ret[0] = p;
			ret[1] = c;
			return ret;
		}
		if (currentSlider != c) {
			currentSlider = c;
			positions = ((Slider) c).getTickPositionCircles();
			idx = 0;
		}
		for (; idx < positions.length; idx++) {
			if (time < positions[idx].getTime() || idx == positions.length - 1) {
				ret[1] = positions[idx];
				if (idx == 0) {
					ret[0] = p;
				} else {
					ret[0] = positions[idx - 1];
				}
				return ret;
			}
		}
		return null; // make the compiler happy c:
	}

	@Override
	public String toString() {
		return "Inherited from normal mover";
	}

}
