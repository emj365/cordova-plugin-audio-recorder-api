Cordova Audio Recorder API Plugin
==============================

Introduction:
--------------

This plugin is a Cordova audio recorder plugin which works as API.

Different than http://plugins.cordova.io/#/package/org.apache.cordova.media-capture this plugin doesn't request the native recorder app (system default recorder) and active recording manually.

Supports platforms:
--------------------

- iOS
- Android

Install:
---------

```bash
$ cordova plugin add cordova-plugin-audio-recorder-api
```

How to use:
------------

```javascript
var recorder = new Object;
recorder.stop = function() {
  window.plugins.audioRecorderAPI.stop(function(msg) {
    // success
    alert('ok: ' + msg);
  }, function(msg) {
    // failed
    alert('ko: ' + msg);
  });
}
recorder.record = function() {
  window.plugins.audioRecorderAPI.record(function(msg) {
    // complete
    alert('ok: ' + msg);
  }, function(msg) {
    // failed
    alert('ko: ' + msg);
  }, 30); // record 30 seconds
}
recorder.playback = function() {
  window.plugins.audioRecorderAPI.playback(function(msg) {
    // complete
    alert('ok: ' + msg);
  }, function(msg) {
    // failed
    alert('ko: ' + msg);
  });
}
```
