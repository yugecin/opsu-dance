package awlex.ospu.movers;

import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.Mover;

/**
 * Created by Alex Wieser on 09.10.2016.
 * WHO DO YOU THINK I AM?
 * <p>
 * This {@link Mover} starts the spiral from the start object
 */
public class CentralSpiralMover extends SpiralMover {
	
	public CentralSpiralMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
	}
	
	@Override
	public double[] getPointAt(int time) {
		double rad = radius * getT(time);
		double ang = angle + 2d * Math.PI * getT(time) * dir;
		return new double[]{
			startX + rad * Math.cos(ang),
			startY + rad * Math.sin(ang)
		};
	}
	
	@Override
	public boolean checkBounds() {
		boolean ret = true;
		int totalTime = endTime - startTime;
		for (int i = 0; ret && i <= 20; i++)
			ret = checkIfPointInBounds(getPointAt((int) (startTime + totalTime * (i / 20d))));
		return ret;
	}
}
