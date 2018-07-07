package awlex.ospu.movers.factories;

import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.objects.GameObject;
import awlex.ospu.movers.CentralSpiralMover;
import awlex.ospu.movers.CombinedSpiralMover;
import yugecin.opsudance.movers.Mover;
import awlex.ospu.movers.SpiralToMover;
import yugecin.opsudance.movers.factories.MoverFactory;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Created by Alex Wieser on 09.10.2016.
 * WHO DO YOU THINK I AM?
 */
public class SpiralMoverFactory implements MoverFactory {

    private int startTime;
    private int endTime;

    /**
     * This method will return either a {@link CentralSpiralMover}, {@link SpiralToMover} or
     * {@link CombinedSpiralMover}, depending on the situation
     *
     * @param start
     * @param end
     * @param dir
     * @return
     */
    @Override
    public Mover create(GameObject start, GameObject end, int dir) {

        startTime = start.getEndTime();
        endTime = end.getTime();

        SpiralToMover spiralTo = new SpiralToMover(start, end, dir);
        CentralSpiralMover center = new CentralSpiralMover(start, end, dir);

        if (Utils.distance(start.end.x, start.end.y, end.start.y, end.start.x) > 150) {
            if (inBounds(spiralTo) || inBounds(spiralTo = new SpiralToMover(start, end, -dir)))
                return spiralTo;
            else if (inBounds(center) || inBounds(center = new CentralSpiralMover(start, end, -dir)))
                return center;

        } else {
            if (inBounds(center) || inBounds(center = new CentralSpiralMover(start, end, -dir)))
                return center;
            else if (inBounds(spiralTo) || inBounds(spiralTo = new SpiralToMover(start, end, -dir)))
                return spiralTo;
        }
        return new CombinedSpiralMover(start, end, dir);
    }

    /**
     * Checks if the spiral is in bounds. <br> <br>
     * TODO: Find a better algorithm and more effizient way to to this, by finding the crucial point in the arithmetic spiral. This also applies for other inBounds-methods
     * <br><br>This method fails miserably in the beginning and on the maps with big and fast jumps (e.g.
     *
     * @param mover
     * @return
     */
    public boolean inBounds(Mover mover) {
        boolean ret = true;
        int middle = endTime - startTime;
        for (int i = 1; ret && i < 15; i++) {
            ret = checkBounds(mover.getPointAt(startTime + (middle * i) / 16));
            //System.out.println("i: " + i + " = " + ret);
        }

        return ret;
        /*return checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .9))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .8))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .7))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .6))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .5))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .4))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .3))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .2))) &&
                checkBounds(mover.getPointAt((int) (startTime + (endTime - startTime) * .1)));
                */
    }

    /**
     * checks if a point is in bounds
     *
     * @param pos
     * @return
     */
    private boolean checkBounds(double[] pos) {
        return 0 < pos[0] && pos[0] < width && 0 < pos[1] && pos[1] < height;
    }

    @Override
    public String toString() {
        return "Spiral me right round, baby";
    }
}
