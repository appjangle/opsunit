package com.appjangle.opsunit.configuration;

import java.util.Arrays;
import java.util.List;

import com.appjangle.opsunit.Response;

public class JobUtils {

	public static List<Response> asList(final Response... responses) {

		return Arrays.asList(responses);
	}

	public static List<Class<?>> asList(final Class<?>... tests) {
		return Arrays.asList(tests);
	}
}
