package awlex.ospu.movers;

import awlex.ospu.FakeGameObject;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.CircleMover;
import yugecin.opsudance.movers.LinearMover;
import yugecin.opsudance.movers.Mover;
import yugecin.opsudance.movers.factories.AutoMoverFactory;

/**
 * Created by Alex Wieser on 09.10.2016.
 * WHO DO YOU THINK I AM?
 * <p>
 * This {@link Mover} exists for the sake of staying inbounds and is rarely instantiated.
 * It creates 1 {@link FakeGameObject} between the 2 passed objects and then creates
 * 2 inner {@link Mover}s around it. This class works recursively and might lead to
 * high ram usage on very jumpy maps or even a StackOverFlow.
 */
public class CombinedSpiralMover extends SpiralMover {
	
	private Mover[] movers;
	private GameObject fakeObject;
	private int halfTime;
	private int startTime;
	private int endTime;
	
	public CombinedSpiralMover(GameObject middle, GameObject start, GameObject end, int dir) {
		this(true, middle, start, end, dir);
	}
	
	public CombinedSpiralMover(boolean prefered, GameObject middle, GameObject start, GameObject end, int dir) {
		super(start, end, dir);
		fakeObject = middle != null ? middle : new FakeGameObject(start, end);
		
		halfTime = fakeObject.getEndTime();
		startTime = start.getEndTime();
		endTime = end.getTime();
		
		movers = new Mover[2];
		movers[0] = bestPick(prefered ? 0 : 1, start, fakeObject, dir);
		movers[1] = bestPick(prefered ? 1 : 0, fakeObject, end, dir);
	}
	
	public CombinedSpiralMover(GameObject start, GameObject end, int dir) {
		this(null, start, end, dir);
	}
	
	@Override
	public boolean checkBounds() {
		/*Not needed in this case*/
		return true;
	}
	
	
	public CombinedSpiralMover(Mover mover1, Mover mover2, int startTime, int halfTime, int endTime) {
		super(new FakeGameObject(), new FakeGameObject(), 0); //With this constructor you only care about the movers
		this.startTime = startTime;
		this.endTime = endTime;
		this.halfTime = halfTime;
		movers = new Mover[]{
			mover1,
			mover2
		};
	}
	
	/**
	 * Method to pick the 2 inner {@link Mover}s.
	 * Tries to pick a {@link SpiralToMover} first position and a
	 * {@link CombinedSpiralMover} for second position for the sake of good looks
	 *
	 * @param pos   index of
	 * @param start start object
	 * @param end   end object
	 * @param dir   direction
	 * @return best fitting Mover
	 */
	private Mover bestPick(int pos, GameObject start, GameObject end, int dir) {
		
		if (endTime - startTime < 40 || Utils.distance(start.end.x, start.end.y, end.start.x, end.start.y) < 40)
			return new LinearMover(start, end, dir);
		
		SpiralToMover spiralTo = new SpiralToMover(start, end, dir);
		CentralSpiralMover center = new CentralSpiralMover(start, end, dir);
		
		if (pos == 0) {
			if (spiralTo.checkBounds() || (spiralTo = new SpiralToMover(start, end, -dir)).checkBounds())
				return spiralTo;
			else if (center.checkBounds() || (center = new CentralSpiralMover(start, end, -dir)).checkBounds())
				return center;
		}
		else if (pos == 1) {
			if (center.checkBounds() || (center = new CentralSpiralMover(start, end, -dir)).checkBounds())
				return center;
			else if ((spiralTo = new SpiralToMover(start, end, -dir)).checkBounds() || (spiralTo = new SpiralToMover(start, end, dir)).checkBounds())
				return spiralTo;
		}
		else throw new IllegalStateException("Only 2 inner Movers allowed");
		
		return new CombinedSpiralMover(start, end, dir);
	}
	
	
	@Override
	public double[] getPointAt(int time) {
		if (time < halfTime)
			return movers[0].getPointAt(time);
		else if (time > halfTime)
			return movers[1].getPointAt(time);
		else return new double[]{
				fakeObject.start.x,
				fakeObject.start.y
			};
	}
	
}
