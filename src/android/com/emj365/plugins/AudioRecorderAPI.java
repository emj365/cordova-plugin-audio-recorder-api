package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;
import android.Manifest;
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
import java.io.IOException;

public class AudioRecorderAPI extends CordovaPlugin {

  private MediaRecorder myRecorder;
  private String outputFile;
  private CountDownTimer countDowntimer;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();

    if (action.equals("requestPermission")) {
	    //检查是否有录音权限
      try{
        if(PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO)){
          callbackContext.success("true");
        }else{
          PermissionHelper.requestPermission(this, 0, Manifest.permission.RECORD_AUDIO);
          callbackContext.success("false");
        }
      }catch(Exception e){
        callbackContext.error("未获得授权使用麦克风，请在设置中打开");
        return false;
      }
      return true;
    }

    if (action.equals("record")) {
      if(!PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO)){
        PermissionHelper.requestPermission(this, 0, Manifest.permission.RECORD_AUDIO);
        callbackContext.error("未获得授权使用麦克风，请在设置中打开");
        return false;
      }else{
        try {
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
              myRecorder.prepare();
              myRecorder.start();
        } catch (final Exception e) {
          callbackContext.error("未获得授权使用麦克风，请在设置中打开");
          return false;
        }
        callbackContext.success("recordSuccess");
      }

      return false;
    }

    if (action.equals("stop")) {
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
	
	try{
		myRecorder.stop();
		myRecorder.release();
		cordova.getThreadPool().execute(new Runnable() {
		  public void run() {
			callbackContext.success(outputFile);
		  }
		});
	}catch(Exception e){

	}
    
  }

}
