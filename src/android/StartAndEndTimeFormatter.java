package com.fitatu.phonegap.plugin.GoogleFit;

import java.util.Calendar;

final public class StartAndEndTimeFormatter {
    private final long startTime;
    private final long endTime;

    public StartAndEndTimeFormatter(long startTime, long endTime) {
        Calendar c = Calendar.getInstance();

        c.setTimeInMillis(startTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        this.startTime = c.getTimeInMillis();

        c.setTimeInMillis(endTime);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        this.endTime = c.getTimeInMillis();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
