function AudioRecorderAPI() {
}

AudioRecorderAPI.prototype.init = function (successCallback, errorCallback, duration) {
  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "init", []);
};

AudioRecorderAPI.prototype.record = function (successCallback, errorCallback, duration, sampleRate, bitRate) {
  var parameters = [];

  if(duration) {
    parameters.push(duration);
    if(sampleRate) {
      parameters.push(sampleRate);
      if(bitRate) {
        parameters.push(bitRate);
      }
    }
  }

  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "record", parameters);
};

AudioRecorderAPI.prototype.stop = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "stop", []);
};

AudioRecorderAPI.prototype.playback = function (successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, "AudioRecorderAPI", "playback", []);
};

AudioRecorderAPI.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.audioRecorderAPI = new AudioRecorderAPI();
  return window.plugins.audioRecorderAPI;
};

cordova.addConstructor(AudioRecorderAPI.install);
