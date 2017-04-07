package com.zhouyou.audio.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import java.lang.ref.WeakReference;

/**
 * <p>描述：广播监听音频设备插入</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/21 13:30<br>
 * 版本： v2.0<br>
 */
public class HeadsetPlugReceiver extends BroadcastReceiver {
    private static HeadsetPlugReceiver headPlugReceiver;
    private WeakReference<Context> contextWeakReference;
    private HeadsetJackStateLisentener headsetJackStateLisentener;
    private boolean isLinked;

    public static HeadsetPlugReceiver getInstance(Context context) {
        if (headPlugReceiver == null) {
            headPlugReceiver = new HeadsetPlugReceiver(context);
        }
        return headPlugReceiver;
    }

    private HeadsetPlugReceiver(Context context) {
        contextWeakReference = new WeakReference(context);
    }

    /**
     * 是否连接耳机孔设备
     *
     * @return
     */
    public boolean isLinked() {
        return isLinked;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra("state")) {
            if (intent.getIntExtra("state", 0) == 1) {//插入
                isLinked = true;
                if (headsetJackStateLisentener != null) headsetJackStateLisentener.haveDevice();
            }
        }
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {//断开
            isLinked = false;
            if (headsetJackStateLisentener != null) headsetJackStateLisentener.haveNoDevice();
        }
    }

    /**
     * 注册广播
     */
    public void registerHeadSetReceiver() {
        if (contextWeakReference.get() == null) return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        contextWeakReference.get().registerReceiver(headPlugReceiver, intentFilter);
    }

    /**
     * 取消注册
     */
    public void unregisterHeadSetReceiver() {
        if (contextWeakReference.get() == null) return;
        contextWeakReference.get().unregisterReceiver(headPlugReceiver);
    }

    /**
     * 设置耳机孔监听
     */
    public void setHeadsetJackStateLisentener(HeadsetJackStateLisentener headsetJackStateLisentener) {
        this.headsetJackStateLisentener = headsetJackStateLisentener;
    }

    /**
     * 耳机孔监听回调接口
     */
    public interface HeadsetJackStateLisentener {
        void haveDevice();

        void haveNoDevice();
    }
}
