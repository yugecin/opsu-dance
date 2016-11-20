package awlex.ospu.polymover;

import itdelatrisu.opsu.objects.GameObject;

/**
 * Created by Awlex on 18.11.2016.
 */
public interface PolyMover {

	double[] getPointAt(int time);

	GameObject[] getItems();

	GameObject getLastItem();
}