package awlex.ospu.polymover;

import itdelatrisu.opsu.objects.GameObject;

/**
 * Created by Awlex on 18.11.2016.
 */
public abstract class PolyMover {
	public abstract double[] getPoint(int time);
	
	public abstract GameObject[] getItems();
}