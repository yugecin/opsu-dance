package awlex.ospu.movers.factories;

import awlex.ospu.movers.CombinedSpiralMover;
import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.Mover;
import yugecin.opsudance.movers.factories.MoverFactory;

/**
 * Created by Alex Wieser on 26.10.2016.
 */
public class CombinedSpiralMoverFactory implements MoverFactory {

    @Override
    public Mover create(GameObject start, GameObject end, int dir) {
        return new CombinedSpiralMover(start, end, dir);
    }

    @Override
    public String toString() {
        return "Spiral even more";
    }
}
