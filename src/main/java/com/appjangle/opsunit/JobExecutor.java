package com.appjangle.opsunit;

/**
 * The executor used to run unit tests and possible responses.
 * 
 * @author mroh004
 * 
 */
public interface JobExecutor {

	public interface JobCallback {
		public void onDone();
	}

	public void run(JobCallback callback);

}
