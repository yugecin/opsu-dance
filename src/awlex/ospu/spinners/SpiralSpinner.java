package awlex.ospu.spinners;

import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import yugecin.opsudance.spinners.Spinner;

/**
 * Created by Alex Wieser on 09.10.2016.
 * WHO DO YOU THINK I AM?
 */
public class SpiralSpinner extends Spinner {

	/**
	 * How many points the Spinner uses.
	 * if there are not enough points the spinner looks angular.
	 * if there are too many points the spinner doesn't operate smooth
	 */
	private final int SIZE = 100;

	/**
	 * The density of the spinner
	 * Determines how many times the spinner will around the center.
	 * if the value is to high the spinner will become angular.
	 * if this value goes under 2 the spinner won't be complete
	 */
	private final int DENSITY = 10;

	/**
	 * How much the spinner will be rotated.
	 * Negative = clockwise
	 * Positive = counter clockwise
	 */
	private final double DELTA = -Math.PI / 20;
	private int MAX_RAD;
	private int index;
	private double[][] points;
	private boolean down;

	@Override
	public void init() {
		points = new double[SIZE][];
		double ang;
		double rad;
		for (int i = 0; i < SIZE / 2; i++) {
			MAX_RAD = (int) (Options.height * .35);
			ang = (DENSITY * (Math.PI / SIZE) * i);
			rad = (MAX_RAD / (SIZE / 2)) * i;
			int offsetX = Options.width / 2;
			int offsetY = Options.height / 2;
			points[SIZE / 2 - 1 - i] = new double[]{
				offsetX + rad * Math.cos(ang),
				offsetY + rad * Math.sin(ang)
			};
			points[SIZE / 2 + i] = new double[]{
				offsetX + rad * (Math.cos(ang) * Math.cos(Math.PI) - Math.sin(ang) * Math.sin(Math.PI)),
				offsetY + rad * -Math.sin(ang)
			};
		}
	}

	@Override
	public String toString() {
		return "Spiralspinner";
	}

	@Override
	public double[] getPoint() {
		if (down) {
			if (--index == 0)
				down = !down;
		} else if (++index == SIZE - 1)
			down = !down;

		if (!down && index == 1) {
			rotatePointAroundCenter(points[0], DELTA);
		} else if (down && index == SIZE - 2) {
			rotatePointAroundCenter(points[SIZE - 1], DELTA);
		}
		rotatePointAroundCenter(points[index], DELTA);
		return points[index];
	}

	private void rotatePointAroundCenter(double[] point, double beta) {
		double angle = Math.atan2(point[1] - Options.height / 2, point[0] - Options.width / 2);
		double rad = Utils.distance(point[0], point[1], Options.width / 2, Options.height / 2);

		//rotationMatrix
		point[0] = Options.width / 2 + rad * (Math.cos(angle) * Math.cos(beta) - Math.sin(angle) * Math.sin(beta));
		point[1] = Options.height / 2 + rad * (Math.cos(angle) * Math.sin(beta) + Math.sin(angle) * Math.cos(beta));
	}


}
