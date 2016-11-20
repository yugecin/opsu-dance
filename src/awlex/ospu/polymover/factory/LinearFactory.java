package awlex.ospu.polymover.factory;

import awlex.ospu.polymover.LineMover;
import itdelatrisu.opsu.objects.GameObject;

/**
 * Created by Awlex on 20.11.2016.
 */
public class LinearFactory extends PolyMoverFactory {

	public final static int PREFFERED_BUFFER_SIZE = 2;

	@Override
	public double[] getPointAt(int time) {
		return getCurrent().getPointAt(time);
	}

	@Override
	public void init(GameObject[] objects, int count) {
		addMover(new LineMover(objects, count));
	}

	@Override
	public int getMaxBufferSize() {
		return LineMover.ITEMS_NEEDED;
	}

	@Override
	public int getMinBufferSize() {
		return LineMover.ITEMS_NEEDED;
	}


	@Override
	public String toString() {
		return "Linear";
	}
}
