/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.utils;

public class FPSMeter {

	private final int targetTimeBetweenUpdates;

	private int[] measurements;
	private int timeBetweenUpdates;
	private int currentMeasureIndex;
	private long lastcall = System.nanoTime();

	public FPSMeter(int measurements) {
		targetTimeBetweenUpdates = 1000 / measurements;
		this.measurements = new int[measurements];
	}

	public void update(int delta) {
		timeBetweenUpdates += delta;
		while (timeBetweenUpdates >= targetTimeBetweenUpdates) {
			timeBetweenUpdates -= targetTimeBetweenUpdates;
			//measurements[currentMeasureIndex] = 0;
			currentMeasureIndex = ++currentMeasureIndex % measurements.length;
		}
		for (int i = 0; i < measurements.length; i++) {
			/*
			if (i == currentMeasureIndex) {
				continue;
			}
			measurements[i]++;
			*/
		}
		long nownano = System.nanoTime();
		measurements[currentMeasureIndex] = 1000;
		if (delta > 0) {
			measurements[currentMeasureIndex] = 1000 / delta;
		} else {
			measurements[currentMeasureIndex] = (int) (1000000000L / (nownano - lastcall));
		}
		lastcall = nownano;
	}

	public int getValue() {
		int val = 0;
		for (int i = 0; i < measurements.length; i++) {
			val += measurements[i];
		}
		return val / measurements.length;
	}

}
