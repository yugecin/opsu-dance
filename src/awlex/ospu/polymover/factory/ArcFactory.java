package awlex.ospu.polymover.factory;

import awlex.ospu.polymover.Arc;
import awlex.ospu.polymover.PolyMover;
import itdelatrisu.opsu.objects.GameObject;

/**
 * Created by Awlex on 18.11.2016.
 */
public class ArcFactory implements MultiMoverFactory {
	
	private static final int PREFFERED_BUFFER_SIZE = 3;
	private PolyMover arc1, arc2;
	
	@Override
	public double[] getPoint(int time) {
		if (arc2 == null) {
			return arc1.getPoint(time);
		}
		
		double[] point1 = arc1.getPoint(time);
		double[] point2 = arc2.getPoint(time);
		
		return new double[]{
			(point1[0] + point2[0]) * 0.5,
			(point1[1] + point2[1]) * 0.5
		};
	}
	
	
	public void init(GameObject[] objects, int startIndex) {
		if (objects == null)
			throw new NullPointerException("Objects musn't be null");
		if (objects.length < startIndex + 2)
		arc1 = new Arc(objects[startIndex], objects[startIndex + 1], objects[startIndex + 2]);
		arc2 = null;
	}
	
	public void update(GameObject p) {
		if (arc2 != null)
			arc1 = arc2;
		arc2 = new Arc(arc1, p);
	}
	
	@Override
	public int getPrefferedBufferSize() {
		return PREFFERED_BUFFER_SIZE;
	}
	
	@Override
	public String toString() {
		return "Arcs";
	}
}
