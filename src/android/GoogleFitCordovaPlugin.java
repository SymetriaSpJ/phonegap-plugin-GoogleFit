package com.fitatu.phonegap.plugin.GoogleFit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fitatu.phonegap.plugin.GoogleFit.Command.GetActivitiesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetBMRValuesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetGMSActivitiesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetGMSDailyActivitiesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetGoogleFitPermissionCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.IsConnectedCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.HasGoogleFitPermissionCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.SetUserSettingsCommand;

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
    private Activity activityContext;
    private CallbackContext getGoogleFitPermissionCallbackContext;

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
        if (requestCode == 1 && !getGoogleFitPermissionCallbackContext.isFinished()) {
            cordova.getThreadPool().execute(
                    new HasGoogleFitPermissionCommand(
                            googleFitService,
                            getGoogleFitPermissionCallbackContext
                    )
            );
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("isConnected")) {
            handleIsConnected(callbackContext);

            return true;
        }

        if (action.equals("setUserSettings")) {
            handleSetUserSettings(args, callbackContext);

            return true;
        }

        if (action.equals("getActivities")) {
            handleGetActivities(args, callbackContext);

            return true;
        }

        if (action.equals("getGMSActivities")) {
            handleGetGMSActivities(args, callbackContext);

            return true;
        }

        if (action.equals("getGMSDailyActivities")) {
            handleGetGMSDailyActivities(args, callbackContext);

            return true;
        }

        if (action.equals("getBMRValues")) {
            handleBMRValues(args, callbackContext);

            return true;
        }

        if (action.equals("getGoogleFitPermission")) {
            handleGetGoogleFitPermission(callbackContext);

            return true;
        }

        if (action.equals("hasPermission")) {
            handleHasPermission(callbackContext);

            return true;
        }

        if (action.equals("getPermission")) {
            handleGetPermission(callbackContext);

            return true;
        }

        return false;
    }

    private void handleIsConnected(CallbackContext callbackContext) {
        if (!hasLocationPermission()) {
            callbackContext.success(0);
        }

        cordova.getThreadPool().execute(
                new IsConnectedCommand(
                        googleFitService,
                        callbackContext
                )
        );
    }

    private void handleSetUserSettings(JSONArray args, CallbackContext callbackContext) throws JSONException {
        double weight = args.getJSONObject(0).getDouble("weight");
        double height = args.getJSONObject(0).getDouble("height");

        cordova.getThreadPool().execute(
                new SetUserSettingsCommand(
                        googleFitService,
                        callbackContext,
                        activityContext.getApplicationContext(),
                        weight,
                        height
                )
        );
    }

    private void handleGetGoogleFitPermission(CallbackContext callbackContext) {
        cordova.setActivityResultCallback(this);
        getGoogleFitPermissionCallbackContext = callbackContext;

        cordova.getThreadPool().execute(
                new GetGoogleFitPermissionCommand(
                        googleFitService,
                        callbackContext
                )
        );
    }

    private void handleHasPermission(CallbackContext callbackContext) {
        if (hasPermission()) {
            callbackContext.success(1);
        } else {
            callbackContext.success(0);
        }
    }

    private void handleGetPermission(CallbackContext callbackContext) {
        if (!hasPermission()) {
            ActivityCompat.requestPermissions(activityContext,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION},
                    1);
        }

        handleHasPermission(callbackContext);
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

    private void handleGetGMSActivities(JSONArray args, CallbackContext callbackContext) throws JSONException {
        long startTime = args.getJSONObject(0).getLong("startTime");
        long endTime = args.getJSONObject(0).getLong("endTime");

        cordova.getThreadPool().execute(
                new GetGMSActivitiesCommand(
                        googleFitService,
                        startTime,
                        endTime,
                        callbackContext)
        );
    }

    private void handleGetGMSDailyActivities(JSONArray args, CallbackContext callbackContext) throws JSONException {
        long startTime = args.getJSONObject(0).getLong("startTime");
        long endTime = args.getJSONObject(0).getLong("endTime");

        cordova.getThreadPool().execute(
                new GetGMSDailyActivitiesCommand(
                        googleFitService,
                        startTime,
                        endTime,
                        callbackContext)
        );
    }

    private void handleBMRValues(JSONArray args, CallbackContext callbackContext) throws JSONException {
        long endTime = args.getJSONObject(0).getLong("endTime");

        cordova.getThreadPool().execute(
                new GetBMRValuesCommand(
                        googleFitService,
                        endTime,
                        callbackContext)
        );
    }

    private boolean hasPermission() {
        return hasLocationPermission() && hasActivityRecognitionPermission();
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return activityContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(activityContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return activityContext.checkCallingOrSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(activityContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }
}
