// Copyright 2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core;

import java.util.LinkedList;

public class JobContainer extends Thread
{
	private final LinkedList<Runnable> jobs;

	JobContainer()
	{
		this.jobs = new LinkedList<>();
		this.setName("Job Container");
		this.setDaemon(true);
		this.start();
	}

	public void submitJob(Runnable job)
	{
		synchronized (this.jobs) {
			this.jobs.add(job);
		}
	}

	@Override
	public void run()
	{
		while (!this.isInterrupted()) {
			Runnable job = null;
			synchronized (this.jobs) {
				if (!this.jobs.isEmpty()) {
					job = this.jobs.removeFirst();
				}
			}
			if (job != null) {
				job.run();
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}
	}
}
