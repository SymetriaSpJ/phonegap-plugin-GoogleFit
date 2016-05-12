function GoogleFit() {

}

GoogleFit.prototype.getPermissions = function (successCallback, failureCallback) {
    cordova.exec(
        successCallback,
        failureCallback,
        "GoogleFit",
        "getPermissions",
        []
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
