package com.fitatu.phonegap.plugin.GoogleFit.Command;

import android.content.Context;
import android.util.Log;

import com.fitatu.phonegap.plugin.GoogleFit.GoogleFitService;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;

import org.apache.cordova.CallbackContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SetUserSettingsCommand extends Thread {

    private final static String TAG = "SetUserSettingsCommand";

    private GoogleFitService googleFitService;
    private CallbackContext callbackContext;
    private Context context;

    private double weight;
    private double height;

    private List<DataSet> dataSets = new ArrayList<DataSet>();

    public SetUserSettingsCommand(
            GoogleFitService googleFitService,
            CallbackContext callbackContext,
            Context context,
            double weight,
            double height
    ) {
        this.googleFitService = googleFitService;
        this.callbackContext = callbackContext;
        this.context = context;

        this.weight = weight;
        this.height = height / 100;
    }

    public void run() {
        if (!googleFitService.isConnected()) {
            callbackContext.error("You are not connected to Google Fit");
            return;
        }

        try {
            validate();
            prepareDataSets();
            insertData();
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
            return;
        }

        callbackContext.success();
    }

    private void validate() throws Exception {
        if (weight < 1) {
            throw new Exception(String.format(Locale.ENGLISH, "Invalid weight field (%f)", weight));
        }

        if (height < 1) {
            throw new Exception(String.format(Locale.ENGLISH, "Invalid height field (%f)", height));
        }
    }

    private void insertData() throws Exception {
        for (DataSet dataSet : dataSets) {
            Status status = googleFitService.insertData(dataSet);
            Log.i(TAG, "InsertDataSet: " + status.getStatusMessage());

            if (!status.isSuccess()) {
                throw new Exception(status.getStatusMessage());
            }
        }
    }

    private void prepareDataSets() {
        Date now = new Date();

        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(context)
                .setDataType(DataType.TYPE_WEIGHT)
                .setType(DataSource.TYPE_RAW)
                .build();

        DataSet dataSet = DataSet.create(dataSource);
        DataPoint dataPoint = dataSet.createDataPoint().setTimeInterval(
                0,
                now.getTime(),
                TimeUnit.MILLISECONDS
        ).setFloatValues((float)weight);

        dataSet.add(dataPoint);
        this.dataSets.add(dataSet);

        // Height
        dataSource = new DataSource.Builder()
                .setAppPackageName(context)
                .setDataType(DataType.TYPE_HEIGHT)
                .setType(DataSource.TYPE_RAW)
                .build();

        dataSet = DataSet.create(dataSource);
        dataPoint = dataSet.createDataPoint().setTimeInterval(
                0,
                now.getTime(),
                TimeUnit.MILLISECONDS
        ).setFloatValues((float)height);

        dataSet.add(dataPoint);
        this.dataSets.add(dataSet);
    }
}
