package com.fitatu.phonegap.plugin.GoogleFit.Command;

import com.fitatu.phonegap.plugin.GoogleFit.FitnessActivity;
import com.fitatu.phonegap.plugin.GoogleFit.GoogleFitService;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class GetBMRValuesCommand extends Thread {

    private GoogleFitService googleFitService;
    private CallbackContext callbackContext;
    private long endTime;

    public GetBMRValuesCommand(
            GoogleFitService googleFitService,
            long endTime,
            CallbackContext callbackContext
    ) {
        this.googleFitService = googleFitService;
        this.callbackContext = callbackContext;
        this.endTime = endTime;
    }

    public void run() {
        List <FitnessActivity> activities;
        JSONArray activitiesJSON;

        try {
            activities = googleFitService.getBMRValues(endTime);
        } catch (Exception e) {
            callbackContext.error("Problem with Google Fit API: " + e.getMessage());
            return;
        }

        try {
            activitiesJSON = activitiesToJSONArray(activities);
        } catch (JSONException e) {
            callbackContext.error("Problem with formatting results");
            return;
        }

        callbackContext.success(activitiesJSON);
    }

    private JSONArray activitiesToJSONArray(List<FitnessActivity> activities) throws JSONException {
        JSONArray results = new JSONArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (FitnessActivity activity : activities) {
            JSONObject activityJSON = new JSONObject();
            activityJSON.put("basalEnergy", activity.getBasalCalories());
            activityJSON.put("date", dateFormat.format(activity.getStartDate()));

            results.put(activityJSON);
        }

        return results;
    }
}
