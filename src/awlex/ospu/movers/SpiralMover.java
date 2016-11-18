package awlex.ospu.movers;

import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.GameObject;
import org.newdawn.slick.util.pathfinding.Mover;

/**
 * Created by Alex Wieser on 22.10.2016.
 */
abstract class SpiralMover extends yugecin.opsudance.movers.Mover {

	protected double angle;
	protected double radius;

	int startTime;
	int endTime;

	SpiralMover(GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		angle = Math.atan2(endY - startY, endX - startX);
		radius = Utils.distance(startX, startY, endX, endY);
		startTime = start.getEndTime();
		endTime = end.getTime();
	}

	public double getAngle() {
		return angle;
	}

	public double getRadius() {
		return radius;
	}

	public int getDir() {
		return dir;
	}

	public abstract boolean checkBounds();
	
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}
	
	/**
	 * checks if a point is in bounds
	 *
	 * @param pos x and y coordinations
	 * @return whether this point is within the screen
	 */
	boolean checkIfPointInBounds(double[] pos) {
		return 0 < pos[0] && pos[0] < Options.width && 0 < pos[1] && pos[1] < Options.height;
	}
}
