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

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import android.os.Handler;
import android.util.Log;

public class AudioRecorderAPI extends CordovaPlugin {

    private MediaRecorder myRecorder;
    private String outputFile;
    private CountDownTimer countDowntimer;
    private CallbackContext _volumeCallbackContext;
    private Timer volumeTimer;
    private TimerTask timerTask;

    final Handler handler = new Handler();

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
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    stopRecord(callbackContext);
                }
            };
            countDowntimer.start();
            callbackContext.success("recording started");
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

        if (action.equals("getvolume")) {
            _volumeCallbackContext = callbackContext;
            this.getvolume();
            return true;
        }

        return false;
    }

    private void stopRecord(final CallbackContext callbackContext) {

        if (volumeTimer != null) {
            volumeTimer.cancel();
            volumeTimer = null;
        }

        myRecorder.stop();
        myRecorder.release();

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                callbackContext.success(outputFile);
            }
        });
    }

    private void getvolume() {
        PluginResult result = new PluginResult(PluginResult.Status.OK, "0");
        result.setKeepCallback(true);
        _volumeCallbackContext.sendPluginResult(result);

        volumeTimer = new Timer();
        initializeTimerTask();
        volumeTimer.schedule(timerTask, 0, 100);
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {

                        double ref = 0.00002;
                        int x = myRecorder.getMaxAmplitude();
                        double pressure = x/51805.5336;
                        double db = (20 * Math.log10(pressure/ref));

                        if(db < 0)
                        {
                            db = 0;
                        }

                        PluginResult result = new PluginResult(PluginResult.Status.OK, Double.toString(-100 - (db * -1) + -15));
                        result.setKeepCallback(true);
                        _volumeCallbackContext.sendPluginResult(result);
                    }
                });
            }
        };
    }
}
