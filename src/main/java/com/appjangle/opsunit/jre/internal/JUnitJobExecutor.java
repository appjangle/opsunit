package com.appjangle.opsunit.jre.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.appjangle.opsunit.Job;
import com.appjangle.opsunit.JobContext;
import com.appjangle.opsunit.JobExecutor;
import com.appjangle.opsunit.Response;
import com.appjangle.opsunit.Response.Callback;

public class JUnitJobExecutor implements JobExecutor {

	private final Job job;
	private final JobContext listener;

	@Override
	public void run(final JobCallback callback) {
		listener.getListener().onStartJob(job);
		runTests(job.getResponses(), callback);
	}

	private final void runTests(final List<Response> availableResponses,
			final JobCallback callback) {

		for (final Class<?> test : job.getTests()) {
			System.out.println("run test " + test);
			listener.getListener().onStartTest(job, test);

			final Result result = JUnitCore.runClasses(test);

			if (result.getFailureCount() > 0) {
				listener.getListener().onTestFailed(job, test,
						result.getFailures().get(0).getMessage(),
						result.getFailures().get(0).getException());
				attemptFix(availableResponses, callback);
				return;
			}
		}
		callback.onDone();

	}

	private final void attemptFix(final List<Response> responses,
			final JobCallback callback) {
		// running out of possible ways to fix this execution
		if (responses.size() == 0) {
			listener.getListener().onJobFailed(job,
					new Exception("All responses have been exhausted."));
			callback.onDone();
			return;
		}

		final Response response = responses.get(0);

		final List<Response> remainingResponses = new ArrayList<Response>(
				responses);

		remainingResponses.remove(0);

		response.run(listener, new Callback() {

			@Override
			public void onSuccess() {
				new Thread() {

					@Override
					public void run() {
						runTests(remainingResponses, callback);
					}

				}.start();

			}

			@Override
			public void onFailure(final Throwable t) {
				listener.getListener().onResponseFailed(job, response, t);
				listener.getListener().onJobFailed(job, t);
				callback.onDone();
			}
		});

	}

	public JUnitJobExecutor(final Job job, final JobContext context) {
		super();
		this.job = job;
		this.listener = context;
	}

}
