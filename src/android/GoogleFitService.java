package com.fitatu.phonegap.plugin.GoogleFit;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import org.apache.cordova.CallbackContext;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleFitService {
    public static final String TAG = "fitatu-googlefit";

    private Context appContext;
    private Activity activityContext;
    private GoogleApiClient googleApiClient;
    private boolean authInProgressFlag = false;

    public GoogleFitService(
            Context appContext,
            Activity activityContext
    ) {
        this.appContext = appContext;
        this.activityContext = activityContext;
    }

    private void buildGoogleApiClient(
            GoogleApiClient.ConnectionCallbacks connectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener onConnectionFiled
    ) {
        googleApiClient = new GoogleApiClient.Builder(appContext)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_NUTRITION_READ_WRITE))
                .addConnectionCallbacks(
                        connectionCallbacks
                )
                .addOnConnectionFailedListener(
                        onConnectionFiled
                )
                .build();
    }

    public synchronized void getPermissions(CallbackContext callbackContext) {
        buildGoogleApiClient(
                new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.i(TAG, "Connected!");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        logConnectionSuspended(i);
                    }
                },
                new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(TAG, "Connection failed. Cause: " + result.toString());

                        if (!result.hasResolution()) {
                            return;
                        }

                        if (!authInProgressFlag) {
                            try {
                                Log.i(TAG, "Attempting to resolve failed connection");
                                authInProgressFlag = true;
                                result.startResolutionForResult(activityContext, 1);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(TAG, "Exception while starting resolution activity", e);
                            }
                        }

                        authInProgressFlag = false;
                    }
                }
        );

        if (googleApiClient.blockingConnect().isSuccess()) {
            callbackContext.success();
        }
    }

    public synchronized boolean isConnected() {
        try {
            establishConnection();
        } catch (Exception e) {
            return false;
        }

        return googleApiClient.isConnected();
    }

    public synchronized PendingResult<DataReadResult> getActivities(long startTime, long endTime) throws Exception {
        establishConnection();

        DateFormat dateFormat = DateFormat.getDateInstance();

        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByActivitySegment(1, TimeUnit.SECONDS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        return Fitness.HistoryApi.readData(googleApiClient, readRequest);
    }

    public synchronized Status insertData(DataSet dataSet) {
        return Fitness.HistoryApi.insertData(googleApiClient, dataSet).await(1, TimeUnit.MINUTES);
    }

    public List<FitnessActivity> rewriteBucketToFitnessActivities(List<Bucket> bucketList) {
        List<FitnessActivity> activities = new ArrayList<FitnessActivity>();

        bucketLoop:
        for (Bucket bucket : bucketList) {
            FitnessActivity activity = new FitnessActivity();

            List<DataSet> dataSets = bucket.getDataSets();
            for (DataSet dataSet : dataSets) {

                for (DataPoint dp : dataSet.getDataPoints()) {
                    if (dp.getOriginalDataSource().getAppPackageName() == null) {
                        continue bucketLoop;
                    }

                    activity.setSourceName(dp.getOriginalDataSource().getAppPackageName());

                    activity.setStartDate(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
                    activity.setEndDate(new Date(dp.getEndTime(TimeUnit.MILLISECONDS)));

                    for(Field field : dp.getDataType().getFields()) {
                        if (field.getName().equals("calories")) { // TODO: constants calories
                            activity.setCalories(dp.getValue(field).asFloat());
                        } else if (field.getName().equals("distance")) {
                            activity.setDistance(dp.getValue(field).asFloat());
                        } else if (field.getName().equals("activity")) {
                            Value value = dp.getValue(field);
                            int activityType = value.asInt();
                            activity.setTypeId(activityType);
                            activity.setName(value.asActivity());
                        }
                    }
                }
            }

            activities.add(activity);
        }

        return activities;
    }

    private void establishConnection() throws Exception {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            return;
        }

        buildGoogleApiClient(
                new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.i(TAG, "Connected!");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        logConnectionSuspended(i);
                    }
                },
                new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(TAG, "Connection failed. Cause: " + result.toString());
                    }
                });

        googleApiClient.blockingConnect();
    }

    private void logConnectionSuspended(int i) {
        if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
            Log.i(TAG, "Connection lost.  Cause: Network Lost.");
        } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
        } else {
            Log.i(TAG, "Connection lost.  Reason: Connection Suspended");
        }
    }
}
