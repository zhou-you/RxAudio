package com.zhouyou.audio.util;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import static android.media.AudioManager.FLAG_SHOW_UI;
import static android.media.AudioManager.STREAM_MUSIC;

/**
 * <p>描述：音量调节</p>
 * 1.正常<br>
 * 2.静音<br>
 * 3.震动<br>
 * 4.增加<br>
 * 5.减小<br>
 * 6.最大<br>
 * 7.恢复<br>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/4 16:40<br>
 * 版本： v2.0<br>
 */
public class AudioVolume {
    private static AudioVolume instance;
    private  AudioManager mAudioManager;
    private Context mContext;
    private int max;
    private int current;
    private AudioVolume(Context context) {
        this.mContext = context;
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        current = mAudioManager.getStreamVolume(STREAM_MUSIC);
    }

    public static AudioVolume getInstance(Context context) {
        if (instance == null) {
            instance = new AudioVolume(context);
        }
        return instance;
    }

    /**
     * 正常模式
     */
    public  void volumeNormal(){
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }
    
    /**
     * 震动模式
     */
    public  void volumeVibrate(){
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    }
    
    /**
     * 静音模式
     */
    public  void volumeSilent(){
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    }
    
    /**
     * 增大音量
     */
    public  void volumeUp(){
        mAudioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0);
    }
    
    /**
     * 减小音量
     */
    public  void volumeDown(){
        mAudioManager.adjustVolume(AudioManager.ADJUST_LOWER, 0);
    }

    /**
     * 最大音量
     */
    public  void volumeMax() {
        max = mAudioManager.getStreamMaxVolume(STREAM_MUSIC);
        current = mAudioManager.getStreamVolume(STREAM_MUSIC);
        AudioLog.i("max : " + max + " current : " + current);
        mAudioManager.setStreamVolume(STREAM_MUSIC, max, 0);
        if (Build.VERSION.SDK_INT >= 18 && mAudioManager.getStreamVolume(STREAM_MUSIC) < max) {
            mAudioManager.setStreamVolume(STREAM_MUSIC, max, FLAG_SHOW_UI);
        }
    }
    
    /**
     * 恢复音量
     */
    public  void volumeRecover() {
        mAudioManager.setStreamVolume(STREAM_MUSIC, current, 0);
    }
}