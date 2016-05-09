function GoogleFit() {};

GoogleFit.prototype.connect = function (successCallback, failureCallback) {
  cordova.exec(successCallback,
               failureCallback,
               "GoogleFit",
               "connect");
};

GoogleFit.prototype.getActivities = function (startTime, endTime, successCallback, failureCallback) {
  cordova.exec(successCallback,
               failureCallback,
               "GoogleFit",
               "getActivities",
               [{
                 "startTime" : startTime,
                 "endTime" : endTime,
               }]);
};

GoogleFit.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.fitatu.plugin.googleFit = new GoogleFit();
  return window.fitatu.plugin.googleFit;
};

cordova.addConstructor(GoogleFit.install);