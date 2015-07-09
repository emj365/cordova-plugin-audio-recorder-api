package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.content.Context;
import java.util.UUID;

public class AudioRecorderAPI extends CordovaPlugin {

  private MediaRecorder myRecorder;
  private String outputFile;
  private CountDownTimer countDowntimer;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();
    Integer seconds;
    if (args.length() >= 1) {
      seconds = args.getInt(0);
    } else {
      seconds = 7;
    }
    if (action.equals("record")) {
      outputFile = context.getDir(Environment.DIRECTORY_MUSIC, Context.MODE_WORLD_READABLE)
        .getAbsoluteFile() + "/" + UUID.randomUUID().toString();
      myRecorder = new MediaRecorder();
      myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
      myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
      myRecorder.setAudioSamplingRate(44100);
      myRecorder.setAudioChannels(1);
      myRecorder.setAudioEncodingBitRate(32000);
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

      countDowntimer = new CountDownTimer(seconds * 1000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish() {
          stopRecord(callbackContext);
        }
      };
      countDowntimer.start();
      return true;
    }

    if (action.equals("stop")) {
      countDowntimer.cancel();
      stopRecord(callbackContext);
      return true;
    }

    return false;
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
