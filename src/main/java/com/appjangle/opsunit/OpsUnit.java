package com.appjangle.opsunit;

import java.util.List;

import one.utils.concurrent.Concurrency;

import com.appjangle.opsunit.internal.DefaultJobManager;

public class OpsUnit {

	public static JobManager createManager(final List<Job> jobs,
			final Concurrency concurrency,
			final JobExecutorFactory executorFactory, final JobContext context) {
		return new DefaultJobManager(jobs, concurrency, executorFactory,
				context);
	}

}
