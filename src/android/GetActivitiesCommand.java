package com.fitatu.phonegap.plugin.GoogleFit;

import org.apache.cordova.CallbackContext;

public class GetActivitiesCommand extends Thread {

    private GoogleFitService googleFitService;
    private CallbackContext callbackContext;
    private long startTime;
    private long endTime;

    public GetActivitiesCommand(
            GoogleFitService googleFitService,
            long startTime,
            long endTime,
            CallbackContext callbackContext
    ) {
        this.googleFitService = googleFitService;
        this.callbackContext = callbackContext;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void run() {
        googleFitService.getActivities(startTime, endTime);
    }
}
