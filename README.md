Cordova Audio Recorder API Plugin
==============================

Introduction:
--------------

This plugin is a Cordova audio recorder plugin which works as API.

Different than http://plugins.cordova.io/#/package/org.apache.cordova.media-capture this plugin doesn't request the native recorder app (system default recorder) and active recording manually.

So far, it only supports Android platform.

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
    alert('ok: ' + msg);
  }, function(msg) {
    alert('ko: ' + msg);
  });
}
recorder.record = function() {
  window.plugins.audioRecorderAPI.stop(function(msg) {
    alert('ok: ' + msg);
  }, function(msg) {
    alert('ko: ' + msg);
  }, 30); // record 30 seconds
}
```
