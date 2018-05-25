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
import android.Manifest;
import android.content.pm.PackageManager;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

public class AudioRecorderAPI extends CordovaPlugin {  
  private MediaRecorder myRecorder;
  private String outputFile;
  private CountDownTimer countDowntimer;
  
  private CallbackContext myCallbackContext;
 
  public static final int P_REC_AUDIO = 0; 	
  public static final int P_MOD_AUDIO = 1;
  public static final int P_READ_PHONE = 2;
  
  public String [] permissionArray = {
            Manifest.permission.RECORD_AUDIO,
			Manifest.permission.MODIFY_AUDIO_SETTINGS,
			Manifest.permission.READ_PHONE_STATE};

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();
    Integer seconds;
	myCallbackContext=callbackContext;
    if (args.length() >= 1) {
      seconds = args.getInt(0);
    } else {
      seconds = 7;
    }
	
	if (action.equals("doPermissions"))
	{		
		for( int i = 0; i < permissionArray.length - 1; i++)
		{			
			if (!cordova.hasPermission(permissionArray[i]))
			{
				 cordova.requestPermission(this, i, permissionArray[i]);
			}
		}

		return true;
	}
	
    if (action.equals("record")) {
      outputFile = context.getFilesDir().getAbsoluteFile() + "/"
        + UUID.randomUUID().toString() + ".m4a";
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

  private void stopRecord(final CallbackContext callbackContext) {
    myRecorder.stop();
    myRecorder.release();
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        callbackContext.success(outputFile);
      }
    });
  }
  
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for(int r:grantResults)
        {
            if(r == PackageManager.PERMISSION_DENIED)
            {
				myCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "PERMS_REFUSED"));
				return;
            }
        }
		myCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "PERMS_GRANTED"));		
  }

}
