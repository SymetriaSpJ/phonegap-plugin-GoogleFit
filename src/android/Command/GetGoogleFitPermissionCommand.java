package com.fitatu.phonegap.plugin.GoogleFit.Command;

import com.fitatu.phonegap.plugin.GoogleFit.GoogleFitService;

import org.apache.cordova.CallbackContext;

public class GetGoogleFitPermissionCommand extends Thread {
    private GoogleFitService googleFitService;
    private CallbackContext callbackContext;

    public GetGoogleFitPermissionCommand(GoogleFitService googleFitService, CallbackContext callbackContext) {
        this.googleFitService = googleFitService;
        this.callbackContext = callbackContext;
    }

    public void run() {
        googleFitService.getPermissions(callbackContext);
    }
}