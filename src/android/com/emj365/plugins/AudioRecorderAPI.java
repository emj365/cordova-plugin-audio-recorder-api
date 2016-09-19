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
import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;




public class AudioRecorderAPI extends CordovaPlugin {

  private MediaRecorder   myRecorder      = null;
  private MediaPlayer     myPlayer        = null;
  private String          outputFile      = null;
  private CountDownTimer  countDowntimer  = null;
  private Integer         seconds         = 0;
  private Integer         sampleRate      = 44100;
  private Integer         bitRate         = 118000;




  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();



    if (action.equals("record")) {

      if (args.length() >= 1) { seconds = args.getInt(0); }
      else                    { seconds = 0; }

      if (args.length() >= 2) { sampleRate = args.getInt(1); }
      else                    { sampleRate = 44100; }

      if (args.length() >= 3) { bitRate = args.getInt(2); }
      else                    { bitRate = 118000; }

      try {
        outputFile      = context.getExternalFilesDir(null).getAbsoluteFile() + "/" + UUID.randomUUID().toString() + ".m4a";
        myRecorder      = new MediaRecorder();
        countDowntimer  = new CountDownTimer(seconds * 1000, 1000) {
          public void onTick(long millisUntilFinished) {}
          public void onFinish() { stopRecord(callbackContext); }
        };

        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            myRecorder.setAudioSamplingRate(sampleRate);
            myRecorder.setAudioChannels(1);
            myRecorder.setAudioEncodingBitRate(bitRate);
            myRecorder.setOutputFile(outputFile);

            try {
              myRecorder.prepare();
            } catch (final IOException e) {
              callbackContext.error(e.getMessage());
            }

            myRecorder.start();

            if(seconds > 0) { countDowntimer.start(); }
          }
        });
      } catch (final Exception e) {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() { callbackContext.error(e.getMessage()); }
        });
      }

      return true;
    }




    if (action.equals("stop")) {

      try {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            if(countDowntimer != null) {
              countDowntimer.cancel();
              countDowntimer = null;
            }

            stopRecord(callbackContext);
          }
        });
      } catch (final Exception e) {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            callbackContext.error(e.getMessage());
          }
        });
      }

      return true;
    }




    if (action.equals("playback")) {
      try {
        myPlayer = new MediaPlayer();

        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            FileInputStream fis = null;

            myPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            myPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
              public void onCompletion(MediaPlayer mp) {
                callbackContext.success("playbackComplete");
              }
            });

            try {
              fis = new FileInputStream(new File(outputFile));
            } catch (final FileNotFoundException e) {
              callbackContext.error(e.getMessage());
            }

            try {
              myPlayer.setDataSource(fis.getFD());
            } catch (final IOException e) {
              callbackContext.error(e.getMessage());
            }

            try {
              myPlayer.prepare();
            } catch (final IOException e) {
              callbackContext.error(e.getMessage());
            }

            myPlayer.start();
          }
        });
      } catch (final Exception e) {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            callbackContext.error(e.getMessage());
          }
        });
      }

      return true;
    }




    // Unknown action

    return false;
  }




  private void stopRecord(final CallbackContext callbackContext) {
    try {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          if (myRecorder != null) {
            myRecorder.stop();
            myRecorder.release();
            myRecorder = null;
          }

          callbackContext.success(outputFile);
        }
      });
    } catch (final Exception e) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          callbackContext.error(e.getMessage());
        }
      });
    }
  }

}
