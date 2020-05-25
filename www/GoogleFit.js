function GoogleFit() {

}

GoogleFit.prototype.isConnected = function (successCallback) {
    cordova.exec(
        successCallback,
        null,
        "GoogleFit",
        "isConnected",
        []
    );
};

GoogleFit.prototype.getGoogleFitPermission = function (successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "getGoogleFitPermission",
        []
    );
};

GoogleFit.prototype.getLocationPermission = function (successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "getLocationPermission",
        []
    );
};

GoogleFit.prototype.hasLocationPermission = function (successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "hasLocationPermission",
        []
    );
};

GoogleFit.prototype.setUserSettings = function (weight, height, successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "setUserSettings",
        [{
            "weight" : weight,
            "height" : height
        }]
    );
};

GoogleFit.prototype.getActivities = function (startTime, endTime, successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "getActivities",
        [{
            "startTime" : startTime,
            "endTime" : endTime
        }]
    );
};

GoogleFit.prototype.getGMSActivities = function (startTime, endTime, successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "getGMSActivities",
        [{
            "startTime" : startTime,
            "endTime" : endTime
        }]
    );
};

GoogleFit.prototype.getGMSDailyActivities = function (startTime, endTime, successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "getGMSDailyActivities",
        [{
            "startTime" : startTime,
            "endTime" : endTime
        }]
    );
};

GoogleFit.prototype.getBMRValues = function (endTime, successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "getBMRValues",
        [{
            "endTime" : endTime
        }]
    );
};

GoogleFit.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.fitatuGoogleFit = new GoogleFit();
    return window.plugins.fitatuGoogleFit;
};

cordova.addConstructor(GoogleFit.install);