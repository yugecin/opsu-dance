package awlex.ospu.polymover;

import Jama.Matrix;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;

import static java.lang.Math.*;

/**
 * Created by Awlex on 13.11.2016.
 */
public class ArcMover implements PolyMover {

	public static final int ITEMS_NEEDED = 3;
	private GameObject p1, middle, p2;
	private double xm, ym, r, alpha, beta, gamma;

	public ArcMover(GameObject p1, GameObject middle, GameObject p2) {
		this.p1 = p1;
		this.middle = middle;
		this.p2 = p2;
		init();
	}

	private void init() {
		Matrix m = prepareMatrix(p1, middle, p2);
		xm = m.get(1, 0) * 0.5;
		ym = m.get(2, 0) * 0.5;
		r = sqrt(pow(xm, 2) + pow(ym, 2) - m.get(0, 0));
		alpha = (atan2(p1.end.y - ym, p1.end.x - xm) + 360) % 360;
		beta = (atan2(middle.start.y - ym, middle.start.x - xm) + 360) % 360;

		// Fix angles
		if (beta - alpha > 180) {
			beta -= 360;
		}
		if (gamma - beta > 180) {
			gamma -= 180;
		}
	}

	@Override
	public double[] getPointAt(int time) {
		double angle;
		if (time < middle.getTime()) {
			double percent = ((double) time - p1.getEndTime()) / (middle.getTime() - p1.getEndTime());
			angle = alpha + (beta - alpha) * percent;
		} else {
			double percent = ((double) time - middle.getTime()) / (p2.getTime() - middle.getTime());
			angle = beta + (gamma - beta) * percent;
		}
		return new double[]{xm + r * cos(angle), ym + r * sin(angle)};
	}

	@Override
	public GameObject[] getItems() {
		return new GameObject[]{p1, middle, p2};
	}

	private static Matrix prepareMatrix(GameObject p1, GameObject middle, GameObject p2) {
		Matrix a, b;
		a = new Matrix(new double[][]{{1, -p1.end.x, -p1.end.y}, {1, -middle.start.x, -middle.start.y}, {1, -p2.start.x, -p2.start.y}});
		b = new Matrix(new double[][]{{-(pow(p1.end.x, 2) + pow(p1.end.y, 2))}, {-(pow(middle.start.x, 2) + pow(middle.start.y, 2))}, {-(pow(p2.start.x, 2) + pow(p2.start.y, 2))},});
		return a.solve(b);
	}

	public static boolean canCricleExistBetweenItems(GameObject p1, GameObject p2, GameObject p3) {
		try {
			prepareMatrix(p1, p2, p3);
		} catch (RuntimeException e) {
			return false;
		}
		return true;
	}

	@Override
	public GameObject getLastItem() {
		return p2;
	}
}
