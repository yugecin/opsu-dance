package awlex.ospu.polymover.factory;

import awlex.ospu.polymover.ArcMover;
import awlex.ospu.polymover.LineMover;
import awlex.ospu.polymover.PolyMover;
import itdelatrisu.opsu.objects.DummyObject;
import itdelatrisu.opsu.objects.GameObject;

import java.util.Arrays;

/**
 * Created by Awlex on 18.11.2016.
 */
public class ArcFactory extends PolyMoverFactory {

	private static final int PREFFERED_BUFFER_SIZE = 3;

	public void init(GameObject[] objects, int count) {
		if (count < 3 || (!ArcMover.canCricleExistBetweenItems(objects[0], objects[1], objects[2])))
			addMover(new LineMover(objects, 3));
		else
			addMover(new ArcMover(objects[0], objects[1], objects[2]));
	}

	@Override
	public int getMaxBufferSize() {
		return ArcMover.ITEMS_NEEDED;
	}

	@Override
	public int getMinBufferSize() {
		return LineMover.ITEMS_NEEDED;
	}

	@Override
	public String toString() {
		return "Arcs";
	}

}
