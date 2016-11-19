package awlex.ospu.polymover;

import itdelatrisu.opsu.objects.DummyObject;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.LinearMover;

/**
 * Created by Awlex on 19.11.2016.
 */
public class LineMover extends PolyMover {
	
	GameObject[] objects;
	
	public LineMover(GameObject[] objects, int startIndex, int count) {
		this.objects = new GameObject[count];
		System.arraycopy(objects, startIndex, this.objects, 0, count);
	}
	
	@Override
	public double[] getPointAt(int time) {
		int i = 0;
		while (time > objects[i].getTime() && i < objects.length - 1)
			i++;
		
		return new LinearMover(i == 0 ? new DummyObject() : objects[i - 1], objects[i], 1).getPointAt(time);
	}
	
	@Override
	public GameObject[] getItems() {
		return new GameObject[0];
	}
}
