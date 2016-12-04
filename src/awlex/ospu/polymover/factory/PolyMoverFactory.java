package awlex.ospu.polymover.factory;

import awlex.ospu.polymover.LineMover;
import awlex.ospu.polymover.PolyMover;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.Dancer;
import yugecin.opsudance.movers.Mover;
import yugecin.opsudance.movers.factories.MoverFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Awlex on 18.11.2016.
 */
public abstract class PolyMoverFactory implements MoverFactory, MoverFactory.MultiPoint {

	private LinkedList<PolyMover> movers;
	private int latestIndex;

	public PolyMoverFactory() {
		movers = new LinkedList<>();
	}

	/**
	 * @param time point in time whose cursor position has to be calculated
	 * @return [x, y]
	 */
	public double[] getPointAt(int time) {
		double[] ret = new double[2];
		int i = 0;
		while (i < movers.size()) {
			if (movers.get(i).getLastItem().getEndTime() < time)
				break;
			double[] point = movers.get(i).getPointAt(time);
			ret[0] += point[0];
			ret[1] += point[1];
			i++;
		}
		ret[0] /= i;
		ret[1] /= i;
		return ret;
	}

	@Override
	public Mover create(GameObject start, GameObject end, int dir) {
		throw new UnsupportedOperationException("Polymovers should use the create variant with all the gameobjects + startindex");
	}

	public final void create(GameObject[] objects, int startIndex) {
		if (latestIndex <= startIndex) {
			movers.clear();
		}
		GameObject[] items = new GameObject[getMaxBufferSize()];
		int i = 1;
		items[0] = startIndex == -1 ? Dancer.d : objects[startIndex];
		while (i < items.length - 1) {
			GameObject g = objects[startIndex + i];
			if (g.isSlider() || g.isSpinner())
				break;
			items[i++] = g;
		}
		items[i] = objects[startIndex + i];
		latestIndex = startIndex + getMaxBufferSize() + i - items.length;
		if (++i >= getMinBufferSize()) {
			init(items, i);
		}
		else {
			addMover(new LineMover(objects, startIndex + 1, i));
		}
	}

	protected abstract void init(GameObject[] objects, int count);

	public final void update(GameObject g) {
		GameObject[] items = movers.get(movers.size() - 1).getItems();
		if (items[items.length - 1] != g) {
			System.arraycopy(items, 1, items, 0, items.length - 1);
			items[items.length - 1] = g;
			create(items, 0);
			latestIndex++;
		}
	}

	/**
	 * How many items the Factory would like to look in the future at most
	 *
	 * @return
	 */
	public abstract int getMaxBufferSize();

	/**
	 * How many items the Factory would like to look in the future at least
	 *
	 * @return
	 */
	public abstract int getMinBufferSize();


	public boolean isInitialized() {
		return movers.isEmpty();
	}

	protected PolyMover getCurrent() {
		return movers.peekLast();
	}

	protected List<PolyMover> getMovers() {
		return movers;
	}

	/**
	 * Adds a Mover to the end of the list. It will also remove the first mover, if the list is bigger than the buffersize.
	 *
	 * @param mover the mover to be added
	 */
	protected void addMover(PolyMover mover) {
		movers.add(mover);
		if (movers.size() >= getMaxBufferSize())
			movers.remove();
	}

	public int getLatestIndex() {
		return latestIndex;
	}

}
