package com.appjangle.opsunit.internal;

import java.util.LinkedList;
import java.util.List;

import one.utils.concurrent.Concurrency;
import one.utils.concurrent.OneTimer;
import one.utils.server.ShutdownCallback;

import com.appjangle.opsunit.Job;
import com.appjangle.opsunit.JobContext;
import com.appjangle.opsunit.JobExecutor;
import com.appjangle.opsunit.JobExecutor.JobCallback;
import com.appjangle.opsunit.JobExecutorFactory;
import com.appjangle.opsunit.JobManager;

public class DefaultJobManager implements JobManager {

	private final List<Job> jobs;
	private final Concurrency concurrency;
	private final JobExecutorFactory executorFactory;
	private final JobContext listener;
	private final List<JobExecutor> activeExecutors;
	private final List<OneTimer> timers;

	private volatile boolean started = false;
	private volatile boolean stopping = false;

	@Override
	public void start() {
		if (started) {
			throw new IllegalStateException(
					"Cannot start an already started job manager.");
		}

		if (stopping) {
			throw new IllegalStateException(
					"Cannot start an job manager, which is shutting down.");
		}

		for (final Job job : jobs) {

			final JobExecutor executor = executorFactory.createExecutor(job,
					listener);

			final OneTimer jobTimer = concurrency.newTimer().scheduleRepeating(
					0, job.getFrequency(), new Runnable() {

						@Override
						public void run() {
							if (stopping) {
								return;
							}
							if (activeExecutors.contains(executor)) {
								return;
							}
							activeExecutors.add(executor);

							executor.run(new JobCallback() {

								@Override
								public void onDone() {

									activeExecutors.remove(executor);
								}
							});
						}
					});
			this.timers.add(jobTimer);
		}

		started = true;
	}

	@Override
	public void stop(final ShutdownCallback callback) {
		if (!started) {
			throw new IllegalStateException(
					"Cannot stop an already stopped job manager.");

		}

		stopping = true;

		for (final OneTimer timer : timers) {

			timer.stop();

		}

		timers.clear();

		while (activeExecutors.size() > 0) {
			// System.out.println(activeExecutors.size());
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {

				callback.onFailure(e);
				return;
			}
			Thread.yield();
		}

		started = false;
		stopping = false;

		callback.onShutdownComplete();

	}

	public DefaultJobManager(final List<Job> jobs,
			final Concurrency concurrency,
			final JobExecutorFactory executorFactory,
			final JobContext jobContext) {
		super();
		this.jobs = jobs;
		this.concurrency = concurrency;
		this.listener = jobContext;
		this.executorFactory = executorFactory;
		this.timers = new LinkedList<OneTimer>();
		this.activeExecutors = concurrency.newCollection().newThreadSafeList(
				JobExecutor.class);
	}

}
