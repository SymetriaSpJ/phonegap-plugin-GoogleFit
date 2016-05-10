function GoogleFit() {

}

GoogleFit.prototype.connect = function (successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "connect"
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

GoogleFit.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.fitatuGoogleFit = new GoogleFit();
    return window.plugins.fitatuGoogleFit;
};

cordova.addConstructor(GoogleFit.install);
