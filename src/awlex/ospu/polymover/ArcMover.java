package awlex.ospu.polymover;

import Jama.Matrix;
import itdelatrisu.opsu.objects.Circle;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.Slider;
import itdelatrisu.opsu.states.Game;

import static java.lang.Math.*;

/**
 * Created by Awlex on 13.11.2016.
 */
public class ArcMover extends PolyMover {
	
	private GameObject p1, middle, p2;
	private double xm, ym, r, alpha, beta, gamma;
	
	public ArcMover(GameObject p1, GameObject middle, GameObject p2) {
		this.p1 = p1;
		this.middle = middle;
		this.p2 = p2;
		init();
	}
	
	public ArcMover(PolyMover mover, GameObject p) {
		GameObject[] items = mover.getItems();
		p1 = items[items.length - 2];
		middle = items[items.length - 1];
		p2 = p;
		init();
	}
	
	private void init() {
		Matrix m = prepareMatrix(p1, middle, p2);
		xm = m.get(1, 0) * 0.5;
		ym = m.get(2, 0) * 0.5;
		r = sqrt(pow(xm, 2) + pow(ym, 2) - m.get(0, 0));
		alpha = atan2(p1.end.y - ym, p1.end.x - xm);
		beta = atan2(middle.end.y - ym, middle.end.x - xm);
		gamma = atan2(p2.start.y - ym, p2.start.x - xm);
		//fixAngles();
	}
	
	private void fixAngles() {
		if (alpha < gamma && gamma < beta) {
			gamma += 2 * PI;
		}
		else if ((beta < alpha && alpha < gamma)) {
			beta += 2 * PI;
			gamma += 2 * PI;
		}
		else if (beta < gamma && gamma < alpha) {
			alpha -= 2 * PI;
		}
		else if (gamma < alpha && alpha < beta) {
			alpha -= 2 * PI;
		}
	}
	
	@Override
	public double[] getPointAt(int time) {
		double angle;
		double percent;
		if (time < middle.getTime()) {
			percent = ((double) time - p1.getEndTime()) / (middle.getTime() - p1.getEndTime());
			angle = alpha + (beta - alpha) * percent;
		}
		else {
			percent = ((double) time - middle.getTime()) / (p2.getTime() - middle.getTime());
			angle = beta + (gamma - beta) * percent;
		}
		if (angle > PI)
			angle -= PI;
		return new double[]{
			xm + r * cos(angle),
			ym + r * sin(angle)
		};
	}
	
	@Override
	public GameObject[] getItems() {
		return new GameObject[]{
			p1,
			middle,
			p2
		};
	}
	
	private static Matrix prepareMatrix(GameObject p1, GameObject middle, GameObject p2) {
		Matrix a, b;
		if (p2.isSlider()) {
			Circle c = (((Slider) p2).getTickPositionCircles()[0]);
			a = new Matrix(new double[][]{
				{1, -p1.end.x, -p1.end.y},
				{1, -middle.start.x, -middle.start.y},
				{1, -c.end.x, -c.end.y}
			});
			b = new Matrix(new double[][]{
				{-(pow(p1.end.x, 2) + pow(p1.end.y, 2))},
				{-(pow(middle.start.x, 2) + pow(middle.start.y, 2))},
				{-(pow(c.end.x, 2) + pow(c.end.y, 2))},
			});
		}
		else {
			a = new Matrix(new double[][]{
				{1, -p1.end.x, -p1.end.y},
				{1, -middle.start.x, -middle.start.y},
				{1, -p2.start.x, -p2.start.y}
			});
			b = new Matrix(new double[][]{
				{-(pow(p1.end.x, 2) + pow(p1.end.y, 2))},
				{-(pow(middle.start.x, 2) + pow(middle.start.y, 2))},
				{-(pow(p2.start.x, 2) + pow(p2.start.y, 2))},
			});
		}
		
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
}
