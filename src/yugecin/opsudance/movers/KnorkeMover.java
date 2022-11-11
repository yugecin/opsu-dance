// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.movers;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.objects.curves.Vec2f;

import static yugecin.opsudance.core.InstanceContainer.*;

import java.util.Random;

public class KnorkeMover extends Mover {

	public Vec2f p1, p2;

	static double lastAngle = 0;
	static boolean firstpoint;

	public static void reset()
	{
		lastAngle = 0;
		firstpoint = true;
	}

	private int startTime;
	private int totalTime;

	public KnorkeMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);

		this.startTime = start.getEndTime();
		this.totalTime = end.getTime() - startTime;

		Vec2f s = start.end;
		Vec2f e = end.start;

		double scaleddistance = totalTime;

		if (firstpoint) {
			lastAngle = Math.atan2(-s.y, -s.x);
			firstpoint = false;
		}

		double newAngle = lastAngle + Math.PI;
		if (newAngle > Math.PI * 2d) {
			newAngle -= Math.PI * 2d;
		}

		boolean mirrorx = false, mirrory = false;
		boolean secondpass = false;
		for (;;) {
			if (start instanceof Slider) {
				Slider ss = (Slider) start;
				newAngle = ss.getCurve().getEndAngle();
				if (ss.getRepeats() % 2 == 1) {
					newAngle = ss.getCurve().getStartAngle();
				}

				ss = (Slider) start;
				Vec2f startpos = start.end;
				Vec2f nextpos = ss.getPointAt(ss.getEndTime() - 10);
				if (ss.getRepeats() % 2 == 0) {
					startpos = start.start;
					nextpos = ss.getPointAt(ss.getTime() + 10);
				}
				newAngle = Math.atan2(-nextpos.y + startpos.y, -nextpos.x + startpos.x);
			}

			p1 = new Vec2f(s.x + (float) (Math.cos(newAngle) * scaleddistance), s.y + (float) (Math.sin(newAngle) * scaleddistance));
			p2 = p1;

			if (end instanceof Slider) {
				Slider ss = (Slider) end;
				Vec2f nextpos = ss.getPointAt(ss.getTime() + 10);
				double angle = Math.atan2(-nextpos.y + end.start.y, -nextpos.x + end.start.x);

				p2 = new Vec2f(e.x + (float) (Math.cos(angle) * scaleddistance), e.y + (float) (Math.sin(angle) * scaleddistance));
			}

			if (secondpass) {
				break;
			}
			secondpass = true;

			double maxdist = 0f;
			double midx = (startX + endX) / 2, midy = (startY + endY) / 2;
			for (int i = 0; i < 200; i++) {
				double[] p = getPointAt(i / 200f);
				if (p[0] < 0 || p[1] < 0 || p[0] > width || p[1] > height) {
					double d = Utils.distance(midx, midy, p[0], p[1]);
					if (d > maxdist) {
						maxdist = d;
					}
				}
			}
			if (maxdist > 0) {
				maxdist *= 1.2f;
				// this  - MATH.PI / 2 modification is to make it less boring,
				// because when using the unmodified newAngle it just goes too
				// straight. Maybe find a better way. Random?
				double d = new Random((long) (newAngle * 1000)).nextDouble() * Math.PI * 2d;
				double x = e.x + Math.cos(newAngle + d) * maxdist;
				double y = e.y + Math.sin(newAngle + d) * maxdist;
				int m = (int) (x / width);
				int n = (int) (y / height);
				if (x < 0) {
					m--;
				}
				if (y < 0) {
					n--;
				}
				if (Math.abs(m) % 2 == 1) {
					endX = width - e.x;
					p2.x = width - p2.x;
					mirrorx = true;
				}
				endX += m * width;
				p2.x += m * width;
				if (Math.abs(n) % 2 == 1) {
					endY = height - e.y;
					p2.y = height - p2.y;
					mirrory = true;
				}
				endY += n * height;
				p2.y += n * height;
				e.y = (float) endY;
				e.x = (float) endX;
				//continue;
			}
			break;
		}

		if (scaleddistance > 1) {
			float dy = - e.y + p2.y, dx = - e.x + p1.x;
			if (mirrorx) {
				dx = -dx;
			}
			if (mirrory) {
				dy = -dy;
			}
			lastAngle = Math.atan2(dy, dx);
		}
	}

	@Override
	public double[] getPointAt(int time) {
		double[] p = getPointAt((float) (time - startTime) / totalTime);
		double x = p[0], y = p[1];
		if (x < 0) {
			x = -x;
		}
		if (y < 0) {
			y = -y;
		}
		if (x > width) {
			int m = (int) (x / width);
			x -= m * width;
			if (m % 2 == 1) {
				x = width - x;
			}
		}
		if (y > height) {
			int m = (int) (y / height);
			y -= m * height;
			if (m % 2 == 1) {
				y = height - y;
			}
		}
		p[0] = x;
		p[1] = y;
		return p;
	}

	public double[] getPointAt(float t) {
		double ct = (1 - t);
		return new double[] {
			ct * ct * ct * startX + 3 * ct * ct * t * p1.x + 3 * ct * t * t * p2.x + t * t * t * endX,
			ct * ct * ct * startY + 3 * ct * ct * t * p1.y + 3 * ct * t * t * p2.y + t * t * t * endY,
		};
	}

	@Override
	public String getName() {
		return "knorke";
	}
}
