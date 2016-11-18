package awlex.ospu.movers;

import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.Mover;

/**
 * Created by Alex Wieser on 09.10.2016.
 * WHO DO YOU THINK I AM?
 * <p>
 * This {@link Mover} ends the spiral from the start object
 */
public class SpiralToMover extends SpiralMover {
	
	public SpiralToMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
	}
	
	@Override
	public double[] getPointAt(int time) {
		double rad = radius * (1 - getT(time));
		double ang = angle + Math.PI + 2d * Math.PI * (1 - getT(time)) * dir;
		return new double[]{
			endX + rad * Math.cos(ang),
			endY + rad * Math.sin(ang)
		};
	}
	
	@Override
	public boolean checkBounds() {
		boolean ret = true;
		int totalTime = endTime - startTime;
		for (int i = 0; ret && i <= 20; i++)
			ret = checkIfPointInBounds(getPointAt((int) (startTime + totalTime * (1 - i / 20d))));
		return ret;
	}
}
