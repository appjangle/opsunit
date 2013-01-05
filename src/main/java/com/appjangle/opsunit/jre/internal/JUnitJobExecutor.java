package com.appjangle.opsunit.jre.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final static boolean ENABLE_LOG = false;

    @Override
    public void run(final JobCallback callback) {
        listener.getListener().onStartJob(job);
        runTests(job.getResponses(), callback);
    }

    private final void runTests(final List<Response> availableResponses,
            final JobCallback callback) {

        try {
            for (final Class<?> test : job.getTests()) {
                if (ENABLE_LOG) {
                    System.out.println(this + ": Run test: " + test);
                }
                listener.getListener().onStartTest(job, test);

                final AtomicBoolean completed = new AtomicBoolean(false);
                final AtomicBoolean crashed = new AtomicBoolean(false);

                new Thread() {

                    @Override
                    public void run() {

                        final int timoutInMin = 10;
                        try {

                            Thread.sleep(1000 * 60 * timoutInMin);
                        } catch (final InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        if (!completed.get()) {
                            crashed.set(true);

                            final Exception e = new Exception("Test [" + test
                                    + "] not completed in timeout limit ("
                                    + timoutInMin + " min).");
                            listener.getListener().onTestFailed(
                                    job,
                                    test,
                                    "Test has not been completed within timeout limit ("
                                            + timoutInMin + " min)", e);

                            attemptFix(availableResponses, e, callback);
                        }
                    }

                }.start();

                final Result result = JUnitCore.runClasses(test);

                completed.set(true);

                if (crashed.get()) {
                    if (ENABLE_LOG) {
                        System.out.println(this
                                + ": Test crashed due to violating timeout: "
                                + test + " with " + result.getFailureCount()
                                + " failures.");
                    }
                    return;
                }

                if (ENABLE_LOG) {
                    System.out.println(this + ": Test ran: " + test + " with "
                            + result.getFailureCount() + " failures.");
                }

                if (result.getFailureCount() > 0) {
                    listener.getListener().onTestFailed(job, test,
                            result.getFailures().get(0).getMessage(),
                            result.getFailures().get(0).getException());

                    attemptFix(availableResponses, result.getFailures().get(0)
                            .getException(), callback);
                    return;
                }
            }
        } catch (final Throwable t) {
            listener.getListener().onJobFailed(job,
                    new Exception("Could not run tests: " + job.getTests(), t));
            callback.onDone();
            return;
        }

        listener.getListener().onJobSuccessfullyCompleted(job);

        callback.onDone();

    }

    private final void attemptFix(final List<Response> responses,
            final Throwable lastFailure, final JobCallback callback) {
        try {
            // running out of possible ways to fix this execution
            if (responses.size() == 0) {

                if (ENABLE_LOG) {
                    System.out.println(this + ": All responses exhaused: "
                            + job.getName());
                }

                listener.getListener().onJobFailed(job, lastFailure);

                callback.onDone();
                return;
            }

            final Response response = responses.get(0);

            final List<Response> remainingResponses = new ArrayList<Response>(
                    responses);

            remainingResponses.remove(0);

            if (ENABLE_LOG) {
                System.out.println(this + ": " + job.getName()
                        + " Running response: " + response);
            }

            response.run(listener, new Callback() {

                @Override
                public void onSuccess() {
                    if (ENABLE_LOG) {
                        System.out.println(this + ": " + job.getName()
                                + " Response completed: " + response);
                    }
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
                    new Thread() {

                        @Override
                        public void run() {
                            runTests(remainingResponses, callback);
                        }

                    }.start();
                }
            });
        } catch (final Throwable t) {
            listener.getListener()
                    .onJobFailed(
                            job,
                            new Exception("Could not apply responses: "
                                    + responses, t));
            callback.onDone();
            return;
        }

    }

    public JUnitJobExecutor(final Job job, final JobContext context) {
        super();
        this.job = job;
        this.listener = context;

        // verifying instantiability of test cases
        for (final Class<?> test : job.getTests()) {

            try {
                final Object newInstance = test.getConstructors()[0]
                        .newInstance();
                assert newInstance != null;
                // System.out.println("created " + newInstance);
            } catch (final Throwable e) {
                throw new RuntimeException(e);
            }

        }

    }
}
