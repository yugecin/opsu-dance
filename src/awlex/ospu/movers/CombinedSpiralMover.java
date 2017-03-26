package awlex.ospu.movers;

import awlex.ospu.FakeGameObject;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.Mover;
import yugecin.opsudance.movers.factories.AutoMoverFactory;
import yugecin.opsudance.options.Options;

/**
 * Created by Alex Wieser on 09.10.2016.
 * WHO DO YOU THINK I AM?
 * <p>
 * This {@link Mover} exists for the sake of staying inbounds and is rarely instantiated.
 * It creates 1 {@link FakeGameObject} between the 2 passed objects and then creates
 * 2 inner {@link Mover}s around it. This class works recursively and might lead to
 * high ram usage on very jumpy maps or even a StackOverFlow.
 */
public class CombinedSpiralMover extends Mover {

    private Mover[] movers;
    private GameObject fakeObject;
    private int halfTime;
    private int startTime;
    private int endTime;

    public CombinedSpiralMover(GameObject middle, GameObject start, GameObject end, int dir) {
        super(start, end, dir);
        fakeObject = middle != null ? middle : new FakeGameObject(start, end);

        halfTime = fakeObject.getEndTime();
        startTime = start.getEndTime();
        endTime = end.getTime();

        movers = new Mover[2];
        movers[0] = bestPick(0, start, fakeObject, dir);
        movers[1] = bestPick(1, fakeObject, end, dir);
    }

    public CombinedSpiralMover(GameObject start, GameObject end, int dir) {
        this(null, start, end, dir);
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

        if (endTime - startTime < 10 || Utils.distance(start.end.x, start.end.y, end.start.x, end.start.y) < 40)
            return new AutoMoverFactory().create(start, end, dir);

        SpiralToMover spiralTo = new SpiralToMover(start, end, dir);
        CentralSpiralMover center = new CentralSpiralMover(start, end, dir);

        if (pos == 0) {
            if (inBounds1(spiralTo) || inBounds1(spiralTo = new SpiralToMover(start, end, -dir)))
                return spiralTo;
            else if (inBounds1(center) || inBounds1(center = new CentralSpiralMover(start, end, -dir)))
                return center;
        } else if (pos == 1) {
            if (inBounds2(center) || inBounds2(center = new CentralSpiralMover(start, end, -dir)))
                return center;
            else if (inBounds2(spiralTo = new SpiralToMover(start, end, -dir)) || inBounds2(spiralTo = new SpiralToMover(start, end, dir)))
                return spiralTo;
        } else throw new IllegalStateException("Only 2 inner Movers allowed");

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

    @Override
    public String getName() {
        return "CombinedSpiralMover";
    }

    /**
     * /**
     * Check if the first object would be inbounds
     *
     * @param mover - the mover to check
     * @return is mover always inbounds
     */
    public boolean inBounds1(Mover mover) {
        boolean ret = true;
        int middle = halfTime - startTime;
        for (int i = 1; ret && i < 15; i++) {
            ret = checkBounds(mover.getPointAt(startTime + (middle * i) / 16));
            //System.out.println("i: " + i + " = " + ret);
        }

        return ret;
    }

    /**
     * /**
     * Check if the second object would be inbounds
     *
     * @param mover - the mover to check
     * @return is mover always inbounds
     */
    private boolean inBounds2(Mover mover) {
        boolean ret = true;
        int middle = endTime - halfTime;
        for (int i = 1; ret && i < 15; i++) {
            ret = checkBounds(mover.getPointAt(startTime + (middle * i) / 16));
            //System.out.println("i: " + i + " = " + ret);
        }

        return ret;
    }

    private boolean checkBounds(double[] pos) {
        return 0 < pos[0] && pos[0] < Options.width && 0 < pos[1] && pos[1] < Options.height;
    }
}
