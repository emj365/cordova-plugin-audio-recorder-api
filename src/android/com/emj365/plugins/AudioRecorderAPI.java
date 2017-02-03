package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.os.Environment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

public class AudioRecorderAPI extends CordovaPlugin {
  private static final String RECORD = Manifest.permission.RECORD_AUDIO;
  private static final int AUDIO_RECORD_PERMISSION_CALLBACK = 0;

  private MediaRecorder myRecorder;
  private String outputFile;
  private CountDownTimer countDowntimer;

  private CallbackContext callbackContext;
  private Integer seconds;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();

    if (args.length() >= 1) {
      seconds = args.getInt(0);
    } else {
      seconds = -1;
    }
    if (action.equals("record")) {
      this.callbackContext = callbackContext;

      if (!cordova.hasPermission(RECORD)) {
        cordova.requestPermission(this, AUDIO_RECORD_PERMISSION_CALLBACK, RECORD);

        return true;
      }
      else {
        return record(context);
      }
    }

    if (action.equals("stop")) {
      if (countDowntimer != null) {
        countDowntimer.cancel();
        countDowntimer = null;
      }
      stopRecord(callbackContext);
      return true;
    }

    if (action.equals("playback")) {
      MediaPlayer mp = new MediaPlayer();
      mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
      try {
        FileInputStream fis = new FileInputStream(new File(outputFile));
        mp.setDataSource(fis.getFD());
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        mp.prepare();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
          callbackContext.success("playbackComplete");
        }
      });
      mp.start();
      return true;
    }

    return false;
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    for (int r: grantResults){
      if (r == PackageManager.PERMISSION_DENIED) {
        this.callbackContext.error("Permission was denied");
      }

      switch(requestCode) {
        case AUDIO_RECORD_PERMISSION_CALLBACK:
          this.record(cordova.getActivity().getApplicationContext());
          break;
      }
    }
  }

  private boolean record(Context context) {
    outputFile = context.getFilesDir().getAbsoluteFile() + "/" + UUID.randomUUID().toString() + ".m4a";
    myRecorder = new MediaRecorder();
    myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    myRecorder.setAudioSamplingRate(8000);
    myRecorder.setAudioChannels(1);
    myRecorder.setAudioEncodingBitRate(12000);
    myRecorder.setOutputFile(outputFile);

    try {
      myRecorder.prepare();
      myRecorder.start();
    } catch (final Exception e) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          callbackContext.error(e.getMessage());
        }
      });
      return false;
    }
    if (seconds != -1) {
      countDowntimer = new CountDownTimer(seconds * 1000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish() {
          stopRecord(callbackContext);
        }
      };
      countDowntimer.start();
    }
    return true;
  }

  private void stopRecord(final CallbackContext callbackContext) {
    myRecorder.stop();
    myRecorder.release();
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        callbackContext.success(outputFile);
      }
    });
  }

}
