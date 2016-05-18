package com.fitatu.phonegap.plugin.GoogleFit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

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

    private GoogleFitService googleFitService;
    private boolean breakGetPermissionsLoop = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Activity activityContext = cordova.getActivity();
        Context appContext = activityContext.getApplicationContext();
        googleFitService = new GoogleFitService(appContext, activityContext);
        cordova.setActivityResultCallback(this);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("isConnected")) {
            cordova.getThreadPool().execute(
                    new IsConnectedCommand(
                            googleFitService,
                            callbackContext
                    )
            );

            return true;
        }

        if (action.equals("getActivities")) {
            long startTime = args.getJSONObject(0).getLong("startTime");
            long endTime = args.getJSONObject(0).getLong("endTime");

            cordova.getThreadPool().execute(
                    new GetActivitiesCommand(
                            googleFitService,
                            startTime,
                            endTime,
                            callbackContext)
            );

            return true;
        }

        if (action.equals("getPermissions")) {
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

            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 0) {
            breakGetPermissionsLoop = true;
        }
    }
}
