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
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import org.apache.cordova.CallbackContext;

import java.text.DateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleFitService extends Thread {
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

    private void buildGoogleApiClient(GoogleApiClient.OnConnectionFailedListener onConnectionFiled) {
        googleApiClient = new GoogleApiClient.Builder(appContext)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                .addScope(new Scope(Scopes.FITNESS_NUTRITION_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!");
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        onConnectionFiled
                )
                .build();
    }

    public synchronized void getPermissions() {
        buildGoogleApiClient(
                new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i(TAG, "Connection failed. Cause: " + result.toString());
                        if (!result.hasResolution()) {
                            // Show the localized error dialog
                            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), activityContext, 0).show();
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

        googleApiClient.connect();
    }

    public synchronized void getActivities(long startTime, long endTime) {
        try {
            establishConnection();
        } catch (Exception e) {
            Log.i(TAG, "Connection failed");

            return;
        }

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

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleApiClient, readRequest).await(10, TimeUnit.SECONDS);

        for (Bucket bucket : dataReadResult.getBuckets()) {
            Log.i(TAG, "--- Bucket --- ");
            List<DataSet> dataSets = bucket.getDataSets();
            for (DataSet dataSet : dataSets) {
                dumpDataSet(dataSet);
            }
        }
    }

    private void establishConnection() throws Exception {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            return;
        }

        buildGoogleApiClient(new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult result) {
                Log.i(TAG, "Connection failed. Cause: " + result.toString());
            }
        });

        googleApiClient.connect();

        while (googleApiClient.isConnecting()) {
            Thread.sleep(50);
        }

        if (!googleApiClient.isConnected()) {
            throw new Exception("Problem with connection");
        }
    }

    private static void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            if (dp.getOriginalDataSource().getAppPackageName() != null && dp.getOriginalDataSource().getAppPackageName().equals("com.endomondo.android")) {
                for(Field field : dp.getDataType().getFields()) {
                    if (field.getName().equals("calories")) {
//                        calories += dp.getValue(field).asFloat();
                    }
                }
            }
            Log.i(TAG, "Data point ---");
            Log.i(TAG, "\tApp: " + dp.getOriginalDataSource().getAppPackageName());
            Log.i(TAG, "\tActivityName: " + dp.getOriginalDataSource().describeContents());
            Log.i(TAG, "\tActivityName: " + dp.getOriginalDataSource().getType());
            Log.i(TAG, "\tActivityName: " + dp.getOriginalDataSource().getStreamIdentifier());
            Log.i(TAG, "\tgetDataType: " + dp.getDataType());
            Log.i(TAG, "\tzzuo: " + dp.getDataType().zzuo());
            Log.i(TAG, "\tzzug: " + dp.zzug());
            Log.i(TAG, "\tzzuh: " + dp.zzuh());
            Log.i(TAG, "\tzzuj: " + dp.zzuj());
            Log.i(TAG, "\ttotring: " + dp.toString());
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }
}
