package com.fitatu.phonegap.plugin.GoogleFit.Command;

import com.fitatu.phonegap.plugin.GoogleFit.GoogleFitService;

import org.apache.cordova.CallbackContext;

public class HasGoogleFitPermissionCommand extends Thread {
    private GoogleFitService googleFitService;
    private CallbackContext callbackContext;

    public HasGoogleFitPermissionCommand(GoogleFitService googleFitService, CallbackContext callbackContext) {
        this.googleFitService = googleFitService;
        this.callbackContext = callbackContext;
    }

    public void run() {
        boolean isConnected = googleFitService.isConnected();

        if (isConnected) {
            callbackContext.success(1);
        } else {
            callbackContext.error("User canceled the process");
        }
    }
}