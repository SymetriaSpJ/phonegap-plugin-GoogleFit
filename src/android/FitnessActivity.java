package com.fitatu.phonegap.plugin.GoogleFit;

import java.util.Date;

public class FitnessActivity {
    private int typeId;
    private String name;
    private float calories;
    private float basalCalories;
    private float activeCalories;
    private float distance;
    private float steps;
    private Date startDate;
    private Date endDate;
    private String sourceName;
    private String sourceLabel;
    private Boolean daily = false;

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getCalories() {
        return calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }

    public float getBasalCalories() {
        return basalCalories;
    }

    public void setBasalCalories(float calories) {
        this.basalCalories = calories;
    }

    public float getActiveCalories() {
        return activeCalories;
    }

    public void setActiveCalories(float calories) {
        this.activeCalories = calories;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getSteps() {
        return steps;
    }

    public void setSteps(float steps) {
        this.steps = steps;
    }

    public float getDurationSeconds() {
        return (endDate.getTime()-startDate.getTime())/1000;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public Boolean getDaily() {
        return daily;
    }

    public void setDaily(Boolean daily) {
        this.daily = daily;
    }
}
