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

import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;

public class Pippi {

	private static double angle = 0;
	private static int currentdelta;
	private static final int targetdelta = 4;

	public static boolean enabled = false;
	public static int angleInc = 10;
	public static int angleSliderInc = 50;
	public static boolean preventWobblyStreams = true;
	public static boolean followcircleExpand = true;
	public static boolean circleSlowSliders = false;
	public static boolean circleLazySliders = false;

	private static double pippirad;
	private static double pippiminrad;
	private static double pippimaxrad;
	private static GameObject previous;

	public static void reset() {
		angle = 0;
		currentdelta = 0;
		pippiminrad = pippirad = Circle.diameter / 2d - 10d;
		pippimaxrad = Circle.diameter - 10d;
	}

	public static void dance(int time, GameObject c, boolean isCurrentLazySlider) {
		boolean slowSlider = circleSlowSliders && c.isSlider() && (((((Slider) c).pixelLength < 200 || c.getEndTime() - c.getTime() > 400)) || isCurrentLazySlider);
		if (!slowSlider) {
			slowSlider = circleLazySliders && isCurrentLazySlider;
		}
		if ((!enabled || c.isSpinner()) && !slowSlider) {
			return;
		}
		if (currentdelta >= targetdelta && c != previous) {
			currentdelta = 0;
			if (c.isSlider() && c.getTime() < time) {
				angle += angleSliderInc / 1800d * Math.PI;
				if (!slowSlider) {
					if (followcircleExpand) {
						if (c.getEndTime() - time < 40 && pippirad > pippimaxrad) {
							pippirad -= 5d;
						} else if (time - c.getTime() > 10 && c.getEndTime() - c.getTime() > 600 && pippirad < pippimaxrad) {
							pippirad += 3d;
						}
					}
				}
			} else if (!c.isSpinner()) {
				if (followcircleExpand && pippirad != pippiminrad) {
					pippirad = pippiminrad;
				}
				angle += angleInc / 1800d * Math.PI;
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
		return enabled && distance < Circle.diameter * 0.93f && preventWobblyStreams;
	}

}
