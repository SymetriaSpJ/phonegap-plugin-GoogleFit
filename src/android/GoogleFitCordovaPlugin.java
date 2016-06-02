package com.fitatu.phonegap.plugin.GoogleFit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.fitatu.phonegap.plugin.GoogleFit.Command.GetActivitiesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.IsConnectedCommand;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * This class echoes a string called from JavaScript.
 */
public class GoogleFitCordovaPlugin extends CordovaPlugin {

    private final static String TAG = "GoogleFitCordovaPlugin";
    private GoogleFitService googleFitService;
    private boolean breakGetPermissionsLoop = false;
    private Activity activityContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activityContext = cordova.getActivity();
        Context appContext = activityContext.getApplicationContext();
        googleFitService = new GoogleFitService(appContext, activityContext);
        cordova.setActivityResultCallback(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 0) {
            breakGetPermissionsLoop = true;
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("isConnected")) {
            handleIsConnected(callbackContext);

            return true;
        }

        if (action.equals("getActivities")) {
            handleGetActivities(args, callbackContext);

            return true;
        }

        if (action.equals("getGoogleFitPermission")) {
            handleGetGoogleFitPermission(callbackContext);

            return true;
        }

        if (action.equals("getLocationPermission")) {
            handleGetLocationPermission(callbackContext);

            return true;
        }

        if (action.equals("hasLocationPermission")) {
            handleHasLocationPermission(callbackContext);

            return true;
        }

        return false;
    }

    private void handleIsConnected(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(
                new IsConnectedCommand(
                        googleFitService,
                        callbackContext
                )
        );
    }

    private void handleGetGoogleFitPermission(CallbackContext callbackContext) {
        cordova.setActivityResultCallback(this);
        googleFitService.getPermissions(callbackContext);

        while (!googleFitService.isConnected() && !breakGetPermissionsLoop) {
            SystemClock.sleep(100);
        }

        breakGetPermissionsLoop = false;
        if (googleFitService.isConnected()) {
            callbackContext.success();
        } else {
            callbackContext.error("No permissions");
        }
    }

    private void handleHasLocationPermission(CallbackContext callbackContext) {
        int result = activityContext.checkCallingOrSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        if (result == PackageManager.PERMISSION_GRANTED) {
            callbackContext.success(1);
        } else {
            callbackContext.success(0);
        }
    }

    private void handleGetLocationPermission(CallbackContext callbackContext) {
        if (ContextCompat.checkSelfPermission(activityContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activityContext,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        handleHasLocationPermission(callbackContext);
    }

    private void handleGetActivities(JSONArray args, CallbackContext callbackContext) throws JSONException {
        long startTime = args.getJSONObject(0).getLong("startTime");
        long endTime = args.getJSONObject(0).getLong("endTime");

        cordova.getThreadPool().execute(
                new GetActivitiesCommand(
                        googleFitService,
                        startTime,
                        endTime,
                        callbackContext)
        );
    }

}
