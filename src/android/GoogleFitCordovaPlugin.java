package com.fitatu.phonegap.plugin.GoogleFit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fitatu.phonegap.plugin.GoogleFit.Command.DisconnectCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetActivitiesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetBMRValuesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetGMSActivitiesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetGMSDailyActivitiesCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.GetGoogleFitPermissionCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.IsConnectedCommand;
import com.fitatu.phonegap.plugin.GoogleFit.Command.SetUserSettingsCommand;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class GoogleFitCordovaPlugin extends CordovaPlugin {

    private GoogleFitService googleFitService;
    private Activity activityContext;
    private Context appContext;
    private CallbackContext getGoogleFitPermissionCallbackContext;
    private CallbackContext getPermissionCallbackContext;

    public static final int RC_REQUEST_GOOGLE_FIT_PERMISSION = 1001;
    public static final int RC_REQUEST_PERMISSION = 1002;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activityContext = cordova.getActivity();
        appContext = activityContext.getApplicationContext();
        googleFitService = new GoogleFitService(appContext, activityContext);
        cordova.setActivityResultCallback(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_REQUEST_GOOGLE_FIT_PERMISSION) {
            cordova.getThreadPool().execute(
                new IsConnectedCommand(
                        googleFitService,
                        getGoogleFitPermissionCallbackContext
                )
            );
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (RC_REQUEST_PERMISSION == requestCode) {
            handleHasPermission(getPermissionCallbackContext);
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

        if (action.equals("disconnect")) {
            handleDisconnect(callbackContext);

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
        if (!hasPermission()) {
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
                        appContext,
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

    private void handleDisconnect(CallbackContext callbackContext) {
        cordova.setActivityResultCallback(this);
        getGoogleFitPermissionCallbackContext = callbackContext;

        cordova.getThreadPool().execute(
                new DisconnectCommand(
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
        getPermissionCallbackContext = callbackContext;
        List<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsList.add(Manifest.permission.ACTIVITY_RECOGNITION);
        } else {
            permissionsList.add("com.google.android.gms.permission.ACTIVITY_RECOGNITION");
        }

        String[] permissions = new String[permissionsList.size()];
        permissions = permissionsList.toArray(permissions);

        if (!hasPermission()) {
            ActivityCompat.requestPermissions(activityContext, permissions, RC_REQUEST_PERMISSION);
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
            return activityContext.checkCallingOrSelfPermission("com.google.android.gms.permission.ACTIVITY_RECOGNITION") == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(activityContext, "com.google.android.gms.permission.ACTIVITY_RECOGNITION") == PackageManager.PERMISSION_GRANTED;
        }

        return ContextCompat.checkSelfPermission(activityContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }
}
