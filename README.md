Cordova Audio Recorder API Plugin
==============================

Introduction:
--------------

This plugin is a Cordova audio recorder plugin that works as API.

Different than http://plugins.cordova.io/#/package/org.apache.cordova.media-capture this plugin does not request the native recorder app (system default recorder) and active recording manually.

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

Where are files save?
---------------------

iOS: `/var/mobile/Applications/<UUID>/Library/NoCloud/<file-id>.m4a`
Android: `/data/data/<app-id>/files/<file-id>.m4a`

Copy File to Another Place
----------------------------

Example with file plugin: http://ngcordova.com/docs/plugins/file/

iOS: `/var/mobile/Applications/<UUID>/Documents/new_file.m4a`
Android: `<sdcard>/new_file.m4a`

```javascript
window.plugins.audioRecorderAPI.record(function(savedFilePath) {
  var fileName = savedFilePath.split('/')[savedFilePath.split('/').length - 1];
  var directory;
  if (cordova.file.documentsDirectory) {
    directory = cordova.file.documentsDirectory; // for iOS
  } else {
    directory = cordova.file.externalRootDirectory; // for Android
  }
  $cordovaFile.copyFile(
    cordova.file.dataDirectory, fileName,
    directory, "new_file.m4a"
  )
    .then(function (success) {
      alert(JSON.stringify(success));
    }, function (error) {
      alert(JSON.stringify(error));
    });
}, function(msg) {
  alert('ko: ' + msg);
}, 3);
```
