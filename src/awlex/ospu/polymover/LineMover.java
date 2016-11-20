package awlex.ospu.polymover;

import itdelatrisu.opsu.objects.DummyObject;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.states.Game;
import yugecin.opsudance.movers.LinearMover;

import java.util.Arrays;

/**
 * Created by Awlex on 19.11.2016.
 */
public class LineMover implements PolyMover {

	public static final int ITEMS_NEEDED = 2;

	private GameObject[] objects;
	private LinearMover m;
	private int currentIndex;

	
	public LineMover(GameObject[] objects, int startIndex, int count) {
		this.objects = objects;
		m = new LinearMover(objects[startIndex], objects[startIndex + 1], 1);
		currentIndex = startIndex + 1;
	}

	public LineMover(GameObject[] objects, int count) {
		this(objects, 0, count);
	}

	@Override
	public double[] getPointAt(int time) {
		if (objects[currentIndex].getEndTime() < time)
			m = new LinearMover(objects[currentIndex], objects[currentIndex + 1], 1);
		return m.getPointAt(time);
	}
	
	@Override
	public GameObject[] getItems() {
		return objects;
	}

}
