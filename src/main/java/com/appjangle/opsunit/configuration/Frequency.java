package com.appjangle.opsunit.configuration;

public class Frequency {

    public static int minutes(final int minutes) {
        return 1000 * 60 * minutes;
    }

    public static int seconds(final int seconds) {
        return 1000 * seconds;
    }
}
