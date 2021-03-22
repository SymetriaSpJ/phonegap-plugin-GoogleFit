package com.fitatu.phonegap.plugin.GoogleFit;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import org.apache.cordova.CallbackContext;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleFitService {
    public static final String TAG = "fitatu-googlefit";
    private static final String GMS_SOURCE_NAME = "com.google.android.gms";
    private static final String FIT_SOURCE_NAME = "com.google.android.apps.fitness";
    private static final int UNKNOWN_TYPE = 4;
    private static final int STILL_TYPE = 3;
    private static final int WALKING_TYPE = 7;
    private static final long DAY_IN_MICROSECONDS = 86400000;
    private static final int GMS_MIN_AUTOMATIC_ACTIVITY_ENERGY = 1;
    private static final int GMS_WALKING_MIN_AUTOMATIC_ACTIVITY_SECONDS = 180;

    private Context appContext;
    private Activity activityContext;
    private GoogleApiClient googleApiClient;
    private FitnessOptions fitnessOptions;

    public GoogleFitService(
            Context appContext,
            Activity activityContext
    ) {
        this.appContext = appContext;
        this.activityContext = activityContext;

        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_BASAL_METABOLIC_RATE, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_WORKOUT_EXERCISE, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE)
            .build();
    }

    public synchronized void getPermissions(CallbackContext callbackContext) {
        if (!isConnected()) {
            GoogleSignIn.requestPermissions(
                    activityContext,
                    GoogleFitCordovaPlugin.RC_REQUEST_GOOGLE_FIT_PERMISSION,
                    GoogleSignIn.getLastSignedInAccount(activityContext),
                    fitnessOptions);
        } else {
            callbackContext.success();
        }
    }

    public synchronized boolean isConnected() {
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activityContext), fitnessOptions);
    }

    public synchronized List<FitnessActivity> getActivities(long startTime, long endTime) throws Exception {
        return rewriteBucketToFitnessActivities(getActivitiesBucketList(startTime, endTime), getBasalValues(endTime));
    }

    public synchronized List<FitnessActivity> getGMSActivities(long startTime, long endTime) throws Exception {
        List<FitnessActivity> basalValues = getBasalValues(endTime);

        List<FitnessActivity> allActivities = rewriteBucketToFitnessActivities(getActivitiesBucketList(startTime, endTime), basalValues);
        List<FitnessActivity> mergedActivities = mergeSiblingActivities(allActivities);
        List<FitnessActivity> gmsActivities = filterActivitiesBySource(mergedActivities, GMS_SOURCE_NAME);
        List<FitnessActivity> fitActivities = filterActivitiesBySource(mergedActivities, FIT_SOURCE_NAME);
        gmsActivities.addAll(fitActivities);
        List<FitnessActivity> dailyActivities = getGMSDailyActivities(startTime, endTime, basalValues);
        List<FitnessActivity> splittedActivities = splitActivities(gmsActivities, dailyActivities);

        return splittedActivities;
    }

    public synchronized List<FitnessActivity> getGMSDailyActivities(long startTime, long endTime) throws Exception {
        List<FitnessActivity> basalValues = getBasalValues(endTime);
        List<FitnessActivity> dailyActivities = getGMSDailyActivities(startTime, endTime, basalValues);

        return dailyActivities;
    }

    public synchronized List<FitnessActivity> getBMRValues(long endTime) throws Exception {
        return getBasalValues(endTime);
    }

    private synchronized List<FitnessActivity> getGMSDailyActivities(long startTime, long endTime, List<FitnessActivity> basalValues) throws Exception {
        Calendar c = Calendar.getInstance();

        c.setTimeInMillis(startTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        startTime = c.getTimeInMillis();

        c.setTimeInMillis(endTime);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        endTime = c.getTimeInMillis();

        DataSource filteredStepsSource = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName(GMS_SOURCE_NAME)
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(filteredStepsSource, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleApiClient, readRequest).await(60, TimeUnit.SECONDS);

        List<Bucket> bucketList = dataReadResult.getBuckets();

        List<FitnessActivity> activities = new ArrayList<>();

        for (Bucket bucket : bucketList) {
            List<DataSet> dataSets = bucket.getDataSets();

            FitnessActivity activity = new FitnessActivity();

            for (DataSet dataSet : dataSets) {
                for (DataPoint dp : dataSet.getDataPoints()) {
                    activity.setSourceName(GMS_SOURCE_NAME);
                    activity.setStartDate(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
                    activity.setEndDate(new Date(dp.getEndTime(TimeUnit.MILLISECONDS)));

                    for(Field field : dp.getDataType().getFields()) {
                        if (field.getName().equals(Field.FIELD_CALORIES.getName())) {
                            activity.setCalories(dp.getValue(field).asFloat());
                        } else if (field.getName().equals(Field.FIELD_DISTANCE.getName())) {
                            activity.setDistance(dp.getValue(field).asFloat());
                        } else if (field.getName().equals(Field.FIELD_STEPS.getName())) {
                            activity.setSteps(dp.getValue(field).asInt());
                        }
                    }


                }
            }

            c.setTime(activity.getStartDate());
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            Date activityStartTime = c.getTime();

            c.setTime(activity.getStartDate());
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
            Date activityEndTime = c.getTime();

            float basal = getBasalForDate(basalValues, activityStartTime, activityEndTime);
            float activeCalories = activity.getCalories() - basal;

            activity.setDaily(true);
            activity.setBasalCalories(basal);
            activity.setActiveCalories(activeCalories < 0 ? 0 : activeCalories);

            activities.add(activity);
        }

        return activities;
    }

    private synchronized List<Bucket> getActivitiesBucketList(long startTime, long endTime) {
        DateFormat dateFormat = DateFormat.getDateInstance();

        Calendar c = Calendar.getInstance();

        c.setTimeInMillis(startTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        startTime = c.getTimeInMillis();

        c.setTimeInMillis(endTime);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        endTime = c.getTimeInMillis();

        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByActivitySegment(1, TimeUnit.SECONDS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleApiClient, readRequest).await(60, TimeUnit.SECONDS);

        return dataReadResult.getBuckets();
    }

    public synchronized Status insertData(DataSet dataSet) {
        return Fitness.HistoryApi.insertData(googleApiClient, dataSet).await(1, TimeUnit.MINUTES);
    }

    private List<FitnessActivity> rewriteBucketToFitnessActivities(List<Bucket> bucketList, List<FitnessActivity> basalValues) {
        List<FitnessActivity> activities = new ArrayList<>();

        for (Bucket bucket : bucketList) {
            List<DataSet> dataSets = bucket.getDataSets();

            FitnessActivity activity = new FitnessActivity();

            for (DataSet dataSet : dataSets) {
                for (DataPoint dp : dataSet.getDataPoints()) {
                    String appPkgName = dp.getOriginalDataSource().getAppPackageName();

                    if (appPkgName != null) {
                        PackageManager pm = this.activityContext.getPackageManager();

                        try {
                            activity.setSourceLabel((String) pm.getApplicationLabel(pm.getApplicationInfo(appPkgName, 0)));
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }

                        activity.setSourceName(appPkgName);
                    }

                    activity.setStartDate(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
                    activity.setEndDate(new Date(dp.getEndTime(TimeUnit.MILLISECONDS)));

                    for(Field field : dp.getDataType().getFields()) {
                        if (field.getName().equals(Field.FIELD_CALORIES.getName())) {
                            activity.setCalories(dp.getValue(field).asFloat());
                        } else if (field.getName().equals(Field.FIELD_DISTANCE.getName())) {
                            activity.setDistance(dp.getValue(field).asFloat());
                        } else if (field.getName().equals(Field.FIELD_ACTIVITY.getName())) {
                            Value value = dp.getValue(field);
                            int activityType = value.asInt();
                            activity.setTypeId(activityType);
                            activity.setName(value.asActivity());
                        } else if (field.getName().equals(Field.FIELD_STEPS.getName())) {
                            activity.setSteps(dp.getValue(field).asInt());
                        }
                    }

                    float basal = getBasalForDate(basalValues, activity.getStartDate(), activity.getEndDate());
                    float activeCalories = activity.getCalories() - basal;

                    activity.setBasalCalories(basal);
                    activity.setActiveCalories(activeCalories < 0 ? 0 : activeCalories);
                }
            }

            activities.add(activity);
        }

        return activities;
    }

    private List<FitnessActivity> filterActivitiesBySource(List<FitnessActivity> activities, String sourceName) {
        List<FitnessActivity> filteredActivities = new ArrayList<>();

        for (FitnessActivity activity : activities) {
            if (activity.getSourceName().equalsIgnoreCase(sourceName)) {
                filteredActivities.add(activity);
            }
        }

        return filteredActivities;
    }

    private List<FitnessActivity> splitActivities(List<FitnessActivity> activities, List<FitnessActivity> dailyActivities) {
        List<FitnessActivity> splittedActivities = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        long dailyActivityStartDate = 0;
        long dailyActivityEndDate = 0;

        for (FitnessActivity activity : activities) {
            if (isIndependentActivity(activity)) {
                splittedActivities.add(activity);

                for (FitnessActivity dailyActivity : dailyActivities) {
                    calendar.setTime(dailyActivity.getStartDate());
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    dailyActivityStartDate = calendar.getTimeInMillis();

                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    calendar.set(Calendar.MILLISECOND, 999);
                    dailyActivityEndDate = calendar.getTimeInMillis();

                    if (activity.getStartDate().getTime() > dailyActivityStartDate && activity.getStartDate().getTime() < dailyActivityEndDate) {
                        dailyActivity.setDistance(dailyActivity.getDistance() - activity.getDistance());
                        dailyActivity.setSteps(dailyActivity.getSteps() - activity.getSteps());
                        dailyActivity.setCalories(dailyActivity.getCalories() - activity.getCalories());
                        dailyActivity.setActiveCalories(dailyActivity.getActiveCalories() - activity.getActiveCalories());
                        dailyActivity.setBasalCalories(dailyActivity.getBasalCalories() - activity.getBasalCalories());
                    }
                }
            }
        }

        for (FitnessActivity activity : dailyActivities) {
            if (activity.getDistance() < 0) activity.setDistance(0);
            if (activity.getSteps() < 0) activity.setSteps(0);
            if (activity.getCalories() < 0) activity.setCalories(0);
            if (activity.getActiveCalories() < 0) activity.setActiveCalories(0);
            if (activity.getBasalCalories() < 0) activity.setBasalCalories(0);

            splittedActivities.add(activity);
        }

        return splittedActivities;
    }

    private Boolean isIndependentActivity(FitnessActivity activity) {
        return (activity.getSourceName().equals(FIT_SOURCE_NAME)
                || ((activity.getActiveCalories() >= GMS_MIN_AUTOMATIC_ACTIVITY_ENERGY)
                    && (activity.getTypeId() != WALKING_TYPE || activity.getDurationSeconds() >= GMS_WALKING_MIN_AUTOMATIC_ACTIVITY_SECONDS)))
                    && activity.getTypeId() != STILL_TYPE && activity.getTypeId() != UNKNOWN_TYPE;
    }

    private List<FitnessActivity> mergeSiblingActivities(List<FitnessActivity> allActivities) {
        List<FitnessActivity> activities = new ArrayList<>();
        int allActivitiesSize = allActivities.size();

        for (int i = 0; i < allActivitiesSize; i++) {
            FitnessActivity activity = allActivities.get(i);

            for (int j = i + 1; j < allActivitiesSize; j++) {
                FitnessActivity nextActivity = allActivities.get(j);

                if ((activity.getSourceName().equalsIgnoreCase(GMS_SOURCE_NAME) || activity.getSourceName().equalsIgnoreCase(FIT_SOURCE_NAME)) && (nextActivity.getSourceName().equalsIgnoreCase(GMS_SOURCE_NAME) || nextActivity.getSourceName().equalsIgnoreCase(FIT_SOURCE_NAME)) && activity.getTypeId() == nextActivity.getTypeId()) {
                    if (activity.getSourceName().equalsIgnoreCase(GMS_SOURCE_NAME) || nextActivity.getSourceName().equalsIgnoreCase(GMS_SOURCE_NAME)) {
                        activity.setSourceName(GMS_SOURCE_NAME);
                    }
                    activity.setCalories(activity.getCalories() + nextActivity.getCalories());
                    activity.setBasalCalories(activity.getBasalCalories() + nextActivity.getBasalCalories());
                    activity.setActiveCalories(activity.getActiveCalories() + nextActivity.getActiveCalories());
                    activity.setDistance(activity.getDistance() + nextActivity.getDistance());
                    activity.setSteps(activity.getSteps() + nextActivity.getSteps());
                    activity.setEndDate(nextActivity.getEndDate());

                    i++;
                } else {


                    break;
                }
            }

            activities.add(activity);
        }

        return activities;
    }

    private List<FitnessActivity> getBasalValues(long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(endTime);
        calendar.add(Calendar.MONTH, -24);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_BASAL_METABOLIC_RATE, DataType.AGGREGATE_BASAL_METABOLIC_RATE_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(calendar.getTimeInMillis(), endTime, TimeUnit.MILLISECONDS)
                .build();

        DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleApiClient, readRequest).await(60, TimeUnit.SECONDS);

        List<Bucket> bucketList = dataReadResult.getBuckets();

        List<FitnessActivity> basalValues = new ArrayList<>();

        for (Bucket bucket : bucketList) {
            List<DataSet> dataSets = bucket.getDataSets();

            FitnessActivity activity = new FitnessActivity();

            for (DataSet dataSet : dataSets) {
                for (DataPoint dp : dataSet.getDataPoints()) {
                    activity.setStartDate(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
                    activity.setBasalCalories(dp.getValue(Field.FIELD_AVERAGE).asFloat());
                }
            }

            if (activity.getBasalCalories() > 0) {
                basalValues.add(activity);
            }
        }

        return basalValues;
    }

    private float getBasalForDate(List<FitnessActivity> basalValues, Date startDate, Date endDate) {
        float basal = 0;
        int findIndex = -1;
        int basalValuesSize = basalValues.size();

        for (int i = 0; i < basalValuesSize; i++) {
            if (basalValues.get(i).getStartDate().after(startDate)) {
                findIndex = i - 1;
                break;
            }
        }

        if (findIndex > -1) {
            basal = basalValues.get(findIndex).getBasalCalories();
        } else if (basalValuesSize > 0) {
            basal = basalValues.get(basalValuesSize - 1).getBasalCalories();
        }

        return (float)(endDate.getTime() - startDate.getTime()) / DAY_IN_MICROSECONDS * basal;
    }
}
