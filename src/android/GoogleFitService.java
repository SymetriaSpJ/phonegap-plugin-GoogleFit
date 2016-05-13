package com.fitatu.phonegap.plugin.GoogleFit;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.SessionReadResult;

import java.text.DateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleFitService {
    public static final String TAG = "fitatu-googlefit";

    private Context appContext;
    private Activity activityContext;
    private GoogleApiClient googleApiClient = null;
    private boolean authInProgressFlag = false;

    public GoogleFitService(Context appContext, Activity activityContext) {
        this.appContext = appContext;
        this.activityContext = activityContext;
    }

    private void buildGoogleApiClient() {
        new FragmentActivity();

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(appContext)
//                    .enableAutoManage(
//                            activityContext,
//                            new GoogleApiClient.OnConnectionFailedListener() {
//                                @Override
//                                public void onConnectionFailed(ConnectionResult result) {
//                                    Log.i(TAG, "Google Play services connection failed. Cause: " +
//                                            result.toString());
//
//                                }
//                            }
//                        )
                    .addApi(Fitness.HISTORY_API)
//                    .addApi(Fitness.SESSIONS_API)
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
                                }
                            }
                    )
                    .build();
        }
    }

    public void getPermissions() {
        buildGoogleApiClient();
    }


/*    public void getActivities(long startTime, long endTime) {
        buildGoogleApiClient();
        googleApiClient.connect();

        DateFormat dateFormat = DateFormat.getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_SPEED)
                .build();
        SessionReadResult sessionReadResult =
                Fitness.SessionsApi.readSession(googleApiClient, readRequest)
                        .await(1, TimeUnit.MINUTES);
        Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                + sessionReadResult.getSessions().size());
        for (Session session : sessionReadResult.getSessions()) {
            // Process the session
            dumpSession(session);

            // Process the data sets for this session
            List<DataSet> dataSets = sessionReadResult.getDataSet(session);
            for (DataSet dataSet : dataSets) {
                dumpDataSet(dataSet);
            }
        }
    }*/

    public void getActivities(long startTime, long endTime) {
        buildGoogleApiClient();
        googleApiClient.connect();

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
//        PendingResult<DataReadResult> dataReadResult = Fitness.HistoryApi.readData(googleApiClient, readRequest).await(10, TimeUnit.SECONDS);

        for (Bucket bucket : dataReadResult.getBuckets()) {
            Log.i(TAG, "--- Bucket --- ");
            List<DataSet> dataSets = bucket.getDataSets();
            for (DataSet dataSet : dataSets) {
                dumpDataSet(dataSet);
            }
        }
//        Log.i(TAG, dataReadResult.toString());
//        List<Bucket> temp = dataReadResult.getBuckets();
//        Bucket tempBucket = temp.get(1);
//
//        tempBucket.getActivity();
//        tempBucket.getSession();


//        dumpDataSet(dataReadResult.getDataSet(DataType.AGGREGATE_CALORIES_EXPENDED));
    }

    private static float calories = 0;

    private static void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = DateFormat.getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            if (dp.getOriginalDataSource().getAppPackageName() != null && dp.getOriginalDataSource().getAppPackageName().equals("com.endomondo.android")) {
                for(Field field : dp.getDataType().getFields()) {
                    if (field.getName().equals("calories")) {
                        calories += dp.getValue(field).asFloat();
                    }
                }
            }
            Log.i(TAG, "Data point --- calories: " + calories);
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

    private void dumpSession(Session session) {
        DateFormat dateFormat = DateFormat.getTimeInstance();
        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tStart: " + dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
                + "\n\tEnd: " + dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));
    }

}
