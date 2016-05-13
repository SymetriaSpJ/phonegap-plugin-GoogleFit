package com.fitatu.phonegap.plugin.GoogleFit;

import android.app.Activity;
import android.content.Context;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class GoogleFitCordovaPlugin extends CordovaPlugin {

    private Activity activityContext;
    private Context appContext;
    private GoogleFitService googleFitService;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activityContext = cordova.getActivity();
        appContext = activityContext.getApplicationContext();
        googleFitService = new GoogleFitService(appContext, activityContext);

//        cordova.setActivityResultCallback(this);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getPermissions")) {
            googleFitService.getPermissions();

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

        return false;
    }

    private void echo(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
