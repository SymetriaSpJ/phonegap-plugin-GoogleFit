package com.fitatu.phonegap.plugin.GoogleFit;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.apache.cordova.CallbackContext;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
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

    public synchronized void disconnect(CallbackContext callbackContext) {
        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_BASAL_METABOLIC_RATE, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE)
                        .build();

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder().addExtension(fitnessOptions).build();

        Task<Void> revokeAccessTask = GoogleSignIn.getClient(appContext, googleSignInOptions).revokeAccess();

        try {
            Tasks.await(revokeAccessTask, 60, TimeUnit.SECONDS);

            callbackContext.success(1);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            callbackContext.success(0);
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
        StartAndEndTimeFormatter dateTime = new StartAndEndTimeFormatter(startTime, endTime);

        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(appContext, fitnessOptions);

        DataSource filteredStepsSource = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName(GMS_SOURCE_NAME)
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .setTimeRange(dateTime.getStartTime(), dateTime.getEndTime(), TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(filteredStepsSource)
                .build();

        Task<DataReadResponse> response = Fitness.getHistoryClient(appContext, googleSignInAccount)
                .readData(readRequest);

        DataReadResponse readDataResult = Tasks.await(response, 60, TimeUnit.SECONDS);

        List<Bucket> bucketList = readDataResult.getBuckets();

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

            Calendar calendar = Calendar.getInstance();

            calendar.setTime(activity.getStartDate());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date activityStartTime = calendar.getTime();

            calendar.setTime(activity.getStartDate());
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date activityEndTime = calendar.getTime();

            float basal = getBasalForDate(basalValues, activityStartTime, activityEndTime);
            float activeCalories = activity.getCalories() - basal;

            activity.setDaily(true);
            activity.setBasalCalories(basal);
            activity.setActiveCalories(activeCalories < 0 ? 0 : activeCalories);

            activities.add(activity);
        }

        return activities;
    }

    private synchronized List<Bucket> getActivitiesBucketList(long startTime, long endTime) throws InterruptedException, ExecutionException, TimeoutException {
        DateFormat dateFormat = DateFormat.getDateInstance();

        StartAndEndTimeFormatter dateTime = new StartAndEndTimeFormatter(startTime, endTime);

        Log.i(TAG, "Range Start: " + dateFormat.format(dateTime.getStartTime()));
        Log.i(TAG, "Range End: " + dateFormat.format(dateTime.getEndTime()));

        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(appContext, fitnessOptions);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .setTimeRange(dateTime.getStartTime(), dateTime.getEndTime(), TimeUnit.MILLISECONDS)
                .bucketByActivitySegment(1, TimeUnit.SECONDS)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                .build();

        Task<DataReadResponse> response = Fitness.getHistoryClient(appContext, googleSignInAccount)
                .readData(readRequest);

        DataReadResponse readDataResult = Tasks.await(response, 60, TimeUnit.SECONDS);

        return readDataResult.getBuckets();
    }

    public synchronized boolean insertData(DataSet dataSet) {
        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.TYPE_HEIGHT, FitnessOptions.ACCESS_WRITE)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(appContext, fitnessOptions);

        Task<Void> response = Fitness.getHistoryClient(appContext, googleSignInAccount)
                .insertData(dataSet);

        try {
            Tasks.await(response, 60, TimeUnit.SECONDS);

            return true;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            return false;
        }
    }

    private List<FitnessActivity> rewriteBucketToFitnessActivities(List<Bucket> bucketList, List<FitnessActivity> basalValues) {
        List<FitnessActivity> activities = new ArrayList<>();

        for (Bucket bucket : bucketList) {
            List<DataSet> dataSets = bucket.getDataSets();

            FitnessActivity activity = new FitnessActivity();

            for (DataSet dataSet : dataSets) {
                for (DataPoint dp : dataSet.getDataPoints()) {
                    String appPkgName = dp.getOriginalDataSource().getAppPackageName();
                    Device device = dp.getOriginalDataSource().getDevice();

                    if (appPkgName != null) {
                        PackageManager pm = this.activityContext.getPackageManager();

                        try {
                            activity.setSourceLabel((String) pm.getApplicationLabel(pm.getApplicationInfo(appPkgName, 0)));
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }

                        activity.setSourceName(appPkgName);
                    }

                    if(device != null) {
                        activity.setDevice(device.getModel());
                    }

                    activity.setStartDate(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
                    activity.setEndDate(new Date(dp.getEndTime(TimeUnit.MILLISECONDS)));

                    for(Field field : dp.getDataType().getFields()) {
                        String name = field.getName();
                        Value value = dp.getValue(field);

                        if (name.equals(Field.FIELD_CALORIES.getName())) {
                            activity.setCalories(value.asFloat());
                        } else if (name.equals(Field.FIELD_DISTANCE.getName())) {
                            activity.setDistance(value.asFloat());
                        } else if (name.equals(Field.FIELD_ACTIVITY.getName())) {
                            activity.setTypeId(value.asInt());
                            activity.setName(value.asActivity());
                        } else if (name.equals(Field.FIELD_STEPS.getName())) {
                            activity.setSteps(value.asInt());
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

        for (FitnessActivity activity : activities) {
            if (isIndependentActivity(activity)) {
                splittedActivities.add(activity);

                for (FitnessActivity dailyActivity : dailyActivities) {
                    StartAndEndTimeFormatter dateTime = new StartAndEndTimeFormatter(dailyActivity.getStartDate().getTime(), dailyActivity.getStartDate().getTime());

                    if (activity.getStartDate().getTime() > dateTime.getStartTime() && activity.getStartDate().getTime() < dateTime.getEndTime()) {
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

    private List<FitnessActivity> getBasalValues(long endTime) throws InterruptedException, ExecutionException, TimeoutException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(endTime);
        calendar.add(Calendar.MONTH, -24);

        GoogleSignInOptionsExtension fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_BASAL_METABOLIC_RATE, FitnessOptions.ACCESS_READ)
                        .build();

        GoogleSignInAccount googleSignInAccount =
                GoogleSignIn.getAccountForExtension(appContext, fitnessOptions);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .setTimeRange(calendar.getTimeInMillis(), endTime, TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .aggregate(DataType.TYPE_BASAL_METABOLIC_RATE)
                .build();

        Task<DataReadResponse> response = Fitness.getHistoryClient(appContext, googleSignInAccount)
                .readData(readRequest);

        DataReadResponse readDataResult = Tasks.await(response, 60, TimeUnit.SECONDS);

        List<Bucket> bucketList = readDataResult.getBuckets();

        List<FitnessActivity> basalValues = new ArrayList<>();

        for (Bucket bucket : bucketList) {
            List<DataSet> dataSets = bucket.getDataSets();

            FitnessActivity activity = new FitnessActivity();

            for (DataSet dataSet : dataSets) {
                for (DataPoint dp : dataSet.getDataPoints()) {
                    activity.setStartDate(new Date(dp.getStartTime(TimeUnit.MILLISECONDS)));
                    activity.setBasalCalories(dp.getValue(Field.FIELD_MAX).asFloat());
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
