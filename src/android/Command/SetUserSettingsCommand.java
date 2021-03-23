package com.fitatu.phonegap.plugin.GoogleFit.Command;

import android.content.Context;

import com.fitatu.phonegap.plugin.GoogleFit.GoogleFitService;
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
            boolean status = googleFitService.insertData(dataSet);

            if (!status) {
                throw new Exception("insertData failed");
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

        DataSet.Builder dataSetBuilder = DataSet.builder(dataSource);
        dataSetBuilder.add(DataPoint.builder(dataSource).setTimeInterval(
                0,
                now.getTime(),
                TimeUnit.MILLISECONDS
        ).setFloatValues((float)weight).build());

        this.dataSets.add(dataSetBuilder.build());

        dataSource = new DataSource.Builder()
                .setAppPackageName(context)
                .setDataType(DataType.TYPE_HEIGHT)
                .setType(DataSource.TYPE_RAW)
                .build();

        dataSetBuilder = DataSet.builder(dataSource);
        dataSetBuilder.add(DataPoint.builder(dataSource).setTimeInterval(
                0,
                now.getTime(),
                TimeUnit.MILLISECONDS
        ).setFloatValues((float)height).build());

        this.dataSets.add(dataSetBuilder.build());
    }
}
