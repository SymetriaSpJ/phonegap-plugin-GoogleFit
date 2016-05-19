package com.fitatu.phonegap.plugin.GoogleFit.Command;

import com.fitatu.phonegap.plugin.GoogleFit.FitnessActivity;
import com.fitatu.phonegap.plugin.GoogleFit.GoogleFitService;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
        List <FitnessActivity> activities;
        JSONArray activitiesJSON;

        try {
            activities = googleFitService.getActivities(startTime, endTime);
        } catch (Exception e) {
            callbackContext.error("Problem with connection to Google Fit API");
            return;
        }

        activities = filterActivities(activities);

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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (FitnessActivity activity : activities) {
            JSONObject activityJSON = new JSONObject();
            activityJSON.put("name", activity.getName());
            activityJSON.put("energy", activity.getCalories());
            activityJSON.put("source", activity.getSourceName());
            activityJSON.put("appName", "Endomondo");
            activityJSON.put("distance", activity.getDistance());
            activityJSON.put("activityStartedAt", dateFormat.format(activity.getStartDate()));
            activityJSON.put("activityStoppedAt", dateFormat.format(activity.getEndDate()));

            results.put(activityJSON);
        }

        return results;
    }

    private List<FitnessActivity> filterActivities(List<FitnessActivity> activities) {
        List<FitnessActivity> filteredActivities = new ArrayList<FitnessActivity>();

        for (FitnessActivity activity : activities) {
            if (activity.getCalories() <= 0) {
                continue;
            }

            if (!activity.getSourceName().equals("com.endomondo.android")) {
                continue;
            }

            filteredActivities.add(activity);
        }

        return filteredActivities;
    }
}