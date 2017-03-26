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
package yugecin.opsudance;

import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;
import yugecin.opsudance.render.GameObjectRenderer;

import static yugecin.opsudance.options.Options.*;

public class Pippi {

	private static double angle = 0;
	private static int currentdelta;
	private static final int targetdelta = 4;

	private static int radiusPercent;

	private static double pippirad;
	private static double pippiminrad;
	private static double pippimaxrad;
	private static GameObject previous;

	public static void setRadiusPercent(int radiusPercent) {
		Pippi.radiusPercent = radiusPercent;
		pippiminrad = pippirad = (GameObjectRenderer.instance.getCircleDiameter() / 2d - 10d) * radiusPercent / 100d;
	}

	public static void reset() {
		angle = 0;
		currentdelta = 0;
		setRadiusPercent(radiusPercent);
		pippimaxrad = GameObjectRenderer.instance.getCircleDiameter() - 10d;
	}

	public static void dance(int time, GameObject c, boolean isCurrentLazySlider) {
		boolean slowSlider = OPTION_DANCE_CIRLCE_IN_SLOW_SLIDERS.state && c.isSlider() && (((((Slider) c).pixelLength < 200 || c.getEndTime() - c.getTime() > 400)) || isCurrentLazySlider);
		if (!slowSlider) {
			slowSlider = OPTION_DANCE_CIRLCE_IN_LAZY_SLIDERS.state && isCurrentLazySlider;
		}
		if ((!OPTION_PIPPI_ENABLE.state || c.isSpinner()) && !slowSlider) {
			return;
		}
		if (currentdelta >= targetdelta && c != previous) {
			currentdelta = 0;
			if (c.isSlider() && c.getTime() < time) {
				angle += OPTION_PIPPI_ANGLE_INC_MUL_SLIDER.val / 1800d * Math.PI;
				if (!slowSlider) {
					if (OPTION_PIPPI_SLIDER_FOLLOW_EXPAND.state) {
						if (c.getEndTime() - time < 40 && pippirad > pippimaxrad) {
							pippirad -= 5d;
						} else if (time - c.getTime() > 10 && c.getEndTime() - c.getTime() > 600 && pippirad < pippimaxrad) {
							pippirad += 3d;
						}
					}
				}
			} else if (!c.isSpinner()) {
				if (OPTION_PIPPI_SLIDER_FOLLOW_EXPAND.state && pippirad != pippiminrad) {
					pippirad = pippiminrad;
				}
				angle += OPTION_PIPPI_ANGLE_INC_MUL.val / 1800d * Math.PI;
			}
			// don't inc on long movements
			if (c.getTime() - time > 400) {
				previous = c;
			}
		}
		Dancer.instance.x += pippirad * Math.cos(angle);
		Dancer.instance.y += pippirad * Math.sin(angle);
		if (slowSlider) {
			c.end.set(Dancer.instance.x,Dancer.instance.y);
		}
	}

	public static void update(int delta) {
		currentdelta += delta;
	}

	public static boolean shouldPreventWobblyStream(double distance) {
		return OPTION_PIPPI_ENABLE.state && distance < GameObjectRenderer.instance.getCircleDiameter() * 0.93f && OPTION_PIPPI_PREVENT_WOBBLY_STREAMS.state;
	}

}
