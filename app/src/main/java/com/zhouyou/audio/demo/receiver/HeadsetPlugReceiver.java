package com.zhouyou.audio.demo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import com.zhouyou.audio.RxAudio;
import com.zhouyou.audio.decoder.DecoderResult;
import com.zhouyou.audio.decoder.IDecoderCallBack;
import com.zhouyou.audio.demo.protocol.SkinTestingProtocol;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.AudioVolume;
import com.zhouyou.audio.util.RxSchedulers;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

/**
 * <p>描述：广播监听音频设备插入并对音频设备进行识别</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/21 13:30<br>
 * 版本： v2.0<br>
 */
public class HeadsetPlugReceiver extends BroadcastReceiver {
    private static HeadsetPlugReceiver headPlugReceiver;
    private WeakReference<Context> contextWeakReference;
    private HeadsetJackStateLisentener headsetJackStateLisentener;
    private PublishSubject publishSubject = PublishSubject.create();
    private Subscription mSubscription;
    private AudioVolume mAudioVolume;
    private RxAudio mRxAudio;
    private boolean isLinked;
    private String[] brands = {"Xiaomi", "HUAWEI",/*"ZTE",*//*"Lenovo A850"*/};//兼容处理

    public static HeadsetPlugReceiver getInstance(Context context) {
        if (headPlugReceiver == null) {
            headPlugReceiver = new HeadsetPlugReceiver(context);
        }
        return headPlugReceiver;
    }

    private HeadsetPlugReceiver(Context context) {
        contextWeakReference = new WeakReference(context);
        mAudioVolume = AudioVolume.getInstance(contextWeakReference.get());
        mRxAudio = RxAudio.getInstance();
        mRxAudio.setReceiveCallBack(mIDecoderCallBack);
    }

    public void notifyResult(DecoderResult result) {
        if (DecoderResult.DECODE_OK == result.code) {
            if (result.data[1] == SkinTestingProtocol.Command.SIGN) {
                publishSubject.onNext("REC_OK");
            }
        }
    }

    /**
     * 恢复接收监听
     */
    public void resume() {
        if (mRxAudio != null) {
            mRxAudio.setReceiveCallBack(mIDecoderCallBack);
        }
    }

    private IDecoderCallBack mIDecoderCallBack = new IDecoderCallBack() {
        @Override
        public void callResult(DecoderResult result) {
            notifyResult(result);
        }
    };

    private void start() {
        if (Arrays.toString(brands).contains(android.os.Build.BRAND)) {
            mRxAudio.setPowerRate(5500);
        }
        mAudioVolume.volumeMax();//音量最大
        mRxAudio.startPower();//开启供电
        mRxAudio.startRecord();//开启录音
    }

    private void stop() {
        mAudioVolume.volumeRecover();//音量恢复
        if (Arrays.toString(brands).contains(android.os.Build.BRAND)) {
            mRxAudio.stopPower();
            mRxAudio.setPowerRate(20050);
            mRxAudio.startPower();
        }
        //mRxAudio.stopPower();//停止供电
        mRxAudio.stopRecord();//停止录音
    }

    private void cancleTime() {
        if (mSubscription != null) mSubscription.unsubscribe();
    }

    private void time() {
        if (mSubscription != null) mSubscription.unsubscribe();
        mSubscription = publishSubject.timeout(5, TimeUnit.SECONDS)
                .delay(100, TimeUnit.MILLISECONDS).compose(RxSchedulers._io_main())
                .subscribe(new Action1() {
                    @Override
                    public void call(Object o) {
                        cancleTime();
                        if (headsetJackStateLisentener != null)
                            headsetJackStateLisentener.haveDevice();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        AudioLog.i("耳机孔设备监听超时了！！！");
                        if (headsetJackStateLisentener != null)
                            headsetJackStateLisentener.timeOut();
                    }
                });
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
                start();
                time();//开启定时如果XXs没有收到识别信息就自动关闭掉录音
            }
        }
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {//断开
            isLinked = false;
            cancleTime();
            stop();
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

        void timeOut();
    }
}
