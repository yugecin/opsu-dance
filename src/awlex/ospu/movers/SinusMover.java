package awlex.ospu.movers;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.Mover;

/**
 * Created by Awlex on 27.10.2016.
 */
public class SinusMover extends Mover {

	private double angle;
	private double radius;
	private double amplitude;

	public SinusMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		angle = Math.atan2(endY - startY, endX - startX);
		radius = Utils.distance(startX, startY, endX, endY);
		amplitude = radius / 4;
		this.dir = angle < 180 ? dir : -dir;
	}

	@Override
	public double[] getPointAt(int time) {
		double x = radius * getT(time);
		double y = amplitude * Math.sin(2d * Math.PI * getT(time)) * dir;
		return new double[]{
			startX + x * Math.cos(angle) - y * Math.sin(angle),
			startY + x * Math.sin(angle) + y * Math.cos(angle)
		};
	}

	@Override
	public String getName() {
		return "Sinustic";
	}
}
