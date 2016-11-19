package awlex.ospu.polymover.factory;

import itdelatrisu.opsu.objects.GameObject;

/**
 * Created by Awlex on 18.11.2016.
 */
public interface PolyMoverFactory {
	
	/**
	 * @param time point in time whose cursor position has to be calculated
	 * @return [x, y]
	 */
	double[] getPointAt(int time);
	
	void init(GameObject[] objects, int startIndex);
	
	void update(GameObject g);
	
	/**
	 * How many items the Factory would like to look in the future
	 *
	 * @return
	 */
	int getPrefferedBufferSize();
	
	boolean isInitialized();
	
	int getLatestIndex();
}
