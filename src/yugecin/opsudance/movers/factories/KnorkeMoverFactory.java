// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.movers.factories;

import itdelatrisu.opsu.objects.GameObject;
import yugecin.opsudance.movers.KnorkeMover;
import yugecin.opsudance.movers.Mover;

public class KnorkeMoverFactory implements MoverFactory {

	@Override
	public Mover create(GameObject start, GameObject end, int dir) {
		return new KnorkeMover(start, end, dir);
	}

	@Override
	public String toString() {
		return "knorke";
	}

}
