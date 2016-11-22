package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import android.Manifest;

import org.json.JSONArray;
import org.json.JSONException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.content.Context;
import android.support.v4.app.ActivityCompat;

import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

public class AudioRecorderAPI extends CordovaPlugin {

    private MediaRecorder myRecorder;
    private String outputFile;
    private CountDownTimer countDowntimer;
    private CallbackContext callbackContext;

    private final int STATE_RECORDING = -1;
    private final int STATE_NO_PERMISSION = -2;
    private final int STATE_SUCCESS = 1;


    /**
     * 用于检测是否具有录音权限
     *
     * @return
     */
    public int getRecordState() {
        int minBuffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, (minBuffer * 100));
        short[] point = new short[minBuffer];
        int readSize = 0;
        try {

            audioRecord.startRecording();//检测是否可以进入初始化状态
        } catch (Exception e) {
            if (audioRecord != null) {
                audioRecord.release();
                audioRecord = null;
                //CLog.d("CheckAudioPermission","无法进入录音初始状态");
            }
            return STATE_NO_PERMISSION;
        }
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            //6.0以下机型都会返回此状态，故使用时需要判断bulid版本
            //检测是否在录音中
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                //CLog.d("CheckAudioPermission","录音机被占用");
            }
            return STATE_RECORDING;
        } else {
            //检测是否可以获取录音结果

            readSize = audioRecord.read(point, 0, point.length);


            if (readSize <= 0) {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;

                }
                //CLog.d("CheckAudioPermission","录音的结果为空");
                return STATE_NO_PERMISSION;

            } else {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }
                return STATE_SUCCESS;
            }
        }
    }

    public boolean requestPermission() {
        try {
            if (Build.VERSION.SDK_INT < 23) {

                if (getRecordState() != STATE_SUCCESS) {
                    return false;
                }
            } else {
                //检查是否有录音权限，如果没有申请
                if (PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO)) {

                } else {
                    PermissionHelper.requestPermission(this, 0, Manifest.permission.RECORD_AUDIO);
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();
        this.callbackContext = callbackContext;

        if (action.equals("requestPermission")) {
            if (!requestPermission()) {
                callbackContext.error("未获得授权使用麦克风，请在设置中打开");
            } else {
                return true;
            }
        }

        if (action.equals("record")) {
            if (!requestPermission()) {
                callbackContext.error("未获得授权使用麦克风，请在设置中打开");
                return false;
            } else {
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
                    if (callbackContext != null) {
                        callbackContext.success("playbackComplete");
                    }
                }
            });
            mp.start();
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (callbackContext != null) {
            callbackContext.error("未获得授权使用麦克风，请在设置中打开");
        }
    }

    private void stopRecord(final CallbackContext callbackContext) {

        try {
            myRecorder.stop();
            myRecorder.release();
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    callbackContext.success(outputFile);
                }
            });
        } catch (Exception e) {

        }

    }

}
