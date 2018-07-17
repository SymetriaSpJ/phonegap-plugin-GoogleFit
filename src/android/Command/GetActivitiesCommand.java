package com.fitatu.phonegap.plugin.GoogleFit.Command;

import com.fitatu.phonegap.plugin.GoogleFit.FitnessActivity;
import com.fitatu.phonegap.plugin.GoogleFit.GoogleFitService;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.result.DataReadResult;

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
        try {
            googleFitService
                .getActivities(startTime, endTime)
                .setResultCallback(new ResultCallback<DataReadResult>() {
                    @Override
                    public void onResult(DataReadResult dataReadResult) {
                        List <FitnessActivity> activities;
                        JSONArray activitiesJSON;

                        Status status = dataReadResult.getStatus();

                        if (status.isSuccess()) {
                            activities = googleFitService
                                .rewriteBucketToFitnessActivities(dataReadResult.getBuckets());
                            activities = filterActivities(activities);

                            try {
                                activitiesJSON = activitiesToJSONArray(activities);
                            } catch (JSONException e) {
                                callbackContext.error("Problem with formatting results");
                                return;
                            }

                            callbackContext.success(activitiesJSON);
                        } else {
                            callbackContext.error(
                                "Problem with Google Fit API: " + status.getStatusMessage()
                            );
                        }
                    }
                });
        } catch (Exception e) {
            callbackContext.error("Problem with Google Fit API: " + e.getMessage());
            return;
        }
    }

    private JSONArray activitiesToJSONArray(List<FitnessActivity> activities) throws JSONException {
        JSONArray results = new JSONArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (FitnessActivity activity : activities) {
            JSONObject activityJSON = new JSONObject();
            activityJSON.put("name", activity.getName());
            activityJSON.put("energy", activity.getCalories());
            activityJSON.put("source", activity.getSourceName());
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

            filteredActivities.add(activity);
        }

        return filteredActivities;
    }
}
