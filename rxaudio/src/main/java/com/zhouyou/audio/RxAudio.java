package com.zhouyou.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.zhouyou.audio.base.ICallBack;
import com.zhouyou.audio.decoder.AbstractDecoder;
import com.zhouyou.audio.decoder.DecoderResult;
import com.zhouyou.audio.decoder.IDecoderCallBack;
import com.zhouyou.audio.exception.ReceiveException;
import com.zhouyou.audio.exception.RetryWhenProcess;
import com.zhouyou.audio.power.PowerSupply;
import com.zhouyou.audio.record.AudioRecorder;
import com.zhouyou.audio.track.RxAudioPlayer;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.Channel;
import com.zhouyou.audio.util.RxSchedulers;
import com.zhouyou.audio.util.SimpleSubscribe;
import com.zhouyou.audio.util.Utils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * <p>描述：音频入口类</p>
 * 主要功能：<br>
 * 1.支持录音<br>
 * 2.支持供电<br>
 * 3.支持发送<br>
 * 4.发送失败自动重试、重试次数、重试间隔<br>
 * 5.支持发送等待回应<br>
 * 6.发送响应不需要等待回应<br>
 * 7.可以设置接收超时时间<br>
 * 8.设置接收回调<br>
 * 9.设置解码实现<br>
 * 10.设置编码实现<br>
 * 11.配置其它相关参数<br>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/5 14:52<br>
 * 版本： v2.0<br>
 */
public final class RxAudio {
    private static AudioRecorder.Builder mRecorderBuilder;
    private static PowerSupply.Builder mPowerBuilder;
    private static RxAudioPlayer.Builder mAudioPlayerBuilder;
    private static AudioRecorder mAudioRecorder;
    private static PowerSupply mPowerSupply;
    private static RxAudioPlayer mAudioPlayer;
    private volatile static RxAudio singleton = null;
    private static int retryCount = 1;//发送失败错误重试次数
    private static int retryDelay = 100;//间隔xxms重试
    private static int receiveTimeOut = 2000;//xxms发送超时
    private static boolean isLog;
    private static RxAudioPlayer.IAudioEncoder audioEncoder;
    private static AbstractDecoder abstractDecoder;
    private static AudioRecorder.IConnectListener recorderConnectListener;
    private boolean isGetRes = false;//是否收到响应消息
    private boolean isSending = false;// 发送命令结束标记
    private DecoderResult mReceiveResult;
    private Subscription mSubscription;

    public static RxAudio getInstance() {
        if (singleton == null) {
            synchronized (RxAudio.class) {
                if (singleton == null) {
                    singleton = new RxAudio();
                }
            }
        }
        return singleton;
    }

    private RxAudio() {
        //默认录音配置
        mRecorderBuilder = new AudioRecorder.Builder().reate(44100)
                .audioEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .audioSource(MediaRecorder.AudioSource.MIC)
                .channelConfiguration(AudioFormat.CHANNEL_CONFIGURATION_MONO);

        //默认供电配置
        mPowerBuilder = new PowerSupply.Builder().sampleRate(48000)
                .channelConfig(AudioFormat.CHANNEL_OUT_MONO)
                .audioFormat(AudioFormat.ENCODING_PCM_16BIT)
                .channel(Channel.RIGHT)
                .powerRate(20050);//设置输出21KHZ正玄波频率 VIVO手机不能设置成21050否则驱动不了电池

        //默认发送配置
        mAudioPlayerBuilder = new RxAudioPlayer.Builder().streamType(AudioManager.STREAM_MUSIC)
                .rate(8000)
                .channelConfig(AudioFormat.CHANNEL_OUT_MONO)
                .audioFormat(AudioFormat.ENCODING_PCM_16BIT)
                .channel(Channel.LEFT)//左声道通信
                .mode(AudioTrack.MODE_STATIC);

        mRecorderBuilder.connectListener(recorderConnectListener);
        mRecorderBuilder.isLog(isLog);
        mPowerBuilder.isLog(isLog);
        mAudioPlayerBuilder.isLog(isLog);

        mAudioRecorder = mRecorderBuilder.build();
        mPowerSupply = mPowerBuilder.build();
        mAudioPlayer = mAudioPlayerBuilder.build();

        //设置解码
        if (abstractDecoder != null) {
            final IDecoderCallBack callBack = abstractDecoder.getReceiveCallBack();
            if (callBack != null) {//获取之前的callback
                abstractDecoder.setReceiveCallBack(new BaseReceiveCallBack(callBack));
            }
            mAudioRecorder.setAudioDecoder(abstractDecoder);
        }

        //设置编码
        if (this.audioEncoder != null) {
            mAudioPlayer.setIAudioEncoder(this.audioEncoder);
        }
    }

    /**
     * 初始化配置
     *
     * @param config
     */
    public static void init(AudioConfig config) {
        Utils.checkIllegalArgument(config, "config ==null");
        if (config.receiveTimeOut < 0) {
            throw new IllegalArgumentException("receiveTimeOut<0");
        }
        if (config.retryCount < 1) {
            throw new IllegalArgumentException("retryCount<1");
        }
        if (config.retryDelay < 0) {
            throw new IllegalArgumentException("retryDelay<0");
        }
        abstractDecoder = config.abstractDecoder;
        receiveTimeOut = config.receiveTimeOut;
        retryCount = config.retryCount;
        retryDelay = config.retryDelay;
        isLog = config.isLog;
        audioEncoder = config.audioEncoder;
        recorderConnectListener = config.recorderConnectListener;
    }

    /**
     * 设置解码
     *
     * @param abstractDecoder
     * @return
     */
    public RxAudio setAbstractDecoder(AbstractDecoder abstractDecoder) {
        //设置解码
        Utils.checkIllegalArgument(abstractDecoder, "abstractDecoder ==null");
        final IDecoderCallBack callBack = abstractDecoder.getReceiveCallBack();
        if (callBack != null) {//获取之前的callback
            abstractDecoder.setReceiveCallBack(new BaseReceiveCallBack(callBack));
        }
        mAudioRecorder.setAudioDecoder(abstractDecoder);
        return this;
    }

    /**
     * 设置编码
     *
     * @param audioEncoder
     * @return
     */
    public RxAudio setAudioEncoder(RxAudioPlayer.IAudioEncoder audioEncoder) {
        Utils.checkIllegalArgument(audioEncoder, "audioEncoder ==null");
        mAudioPlayer.setIAudioEncoder(audioEncoder);
        return this;
    }

    /**
     * 设置全局接收监听
     *
     * @param callBack
     */
    public RxAudio setReceiveCallBack(IDecoderCallBack callBack) {
        abstractDecoder.setReceiveCallBack(new BaseReceiveCallBack(callBack));
        return this;
    }

    /**
     * 开启录音
     */
    public void startRecord() {
        mAudioRecorder.start();
        abstractDecoder.start();//解码线程
    }

    /**
     * 暂停录音
     */
    public void pasuseRecord() {
        mAudioRecorder.pause();
    }

    /**
     * 继续录音
     */
    public void resumeRecord() {
        mAudioRecorder.resume();
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        abstractDecoder.stop();//解码线程
        mAudioRecorder.stop();
    }

    /**
     * 开启供电
     */
    public void startPower() {
        mPowerSupply.start();
    }

    /**
     * 关闭供电
     */
    public void stopPower() {
        mPowerSupply.stop();
    }

    /**
     * 设置供电频率
     *
     * @param sin_rate
     */
    public void setPowerRate(int sin_rate) {
        mPowerSupply.setpowerRate(sin_rate);
    }


    private class BaseReceiveCallBack implements IDecoderCallBack {
        private IDecoderCallBack callBack;

        public BaseReceiveCallBack(IDecoderCallBack callBack) {
            this.callBack = callBack;
        }

        @Override
        public void callResult(DecoderResult result) {
            isGetRes = true;
            mReceiveResult = result;
            if (this.callBack != null) callBack.callResult(result);
        }
    }

    /**
     * 发送命令给设备，不需要响应
     *
     * @param datas
     */
    public void send(final byte[] datas) {
        mAudioPlayer.rxSend(datas).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                print("send success!!!");
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                print("send err!!!" + throwable.getMessage());
            }
        });
    }

    /**
     * 给设备发送命令,并且等待设备回应接收数据
     *
     * @param datas
     * @param callBack
     */
    public void send(final byte[] datas, final ICallBack callBack) {
        if (isSending) {
            print("Already sending!!!!!!!!!!!");
            return;
        }
        isSending = true;
        mSubscription = this.rxSend(datas).doOnTerminate(new Action0() {
            @Override
            public void call() {
                mAudioPlayer.stop();
                isSending = false;
            }
        }).subscribe(new Action1<DecoderResult>() {
            @Override
            public void call(DecoderResult receiveResult) {
                if (receiveResult.code == DecoderResult.DECODE_OK) {
                    callBack.onSuccess(receiveResult.data);
                } else {
                    callBack.onFailure(new Throwable(receiveResult.errMsg));
                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                callBack.onFailure(throwable);
            }
        });
    }

    /**
     * 取消发送
     */
    public void cancleSend() {
        isSending = false;
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
    }

    private Observable<DecoderResult> rxSend(final byte[] datas) {
        isGetRes = false;
        return Observable.create(new SimpleSubscribe<DecoderResult>() {
            @Override
            public DecoderResult execute() throws Throwable {
                boolean result = mAudioPlayer.send(datas);
                print("send result:" + result);
                int timeIndex = 0;
                while (!isGetRes && timeIndex * 50 < receiveTimeOut) {//等待结果
                    Thread.sleep(50);
                    timeIndex++;
                }
                if (mReceiveResult != null && mReceiveResult.code != 0) {
                    throw new ReceiveException(mReceiveResult.code, mReceiveResult.errMsg);
                }
                return mReceiveResult;
            }
        }).timeout(this.receiveTimeOut, TimeUnit.MILLISECONDS)
                .retryWhen(new RetryWhenProcess(retryCount, retryDelay))
                .compose(RxSchedulers.<DecoderResult>_io_main());
    }

    /**
     * 退出音频模块
     */
    public void exitAudio() {
        stopRecord();
        stopPower();
        System.gc();
    }

    public void print(String msg) {
        if (isLog) AudioLog.i(msg);
    }

    /**
     * 配置参数
     */
    public final static class AudioConfig {
        private int retryCount = 0;//发送失败错误重试次数
        private int retryDelay = 100;//100ms
        private int receiveTimeOut = 500;//500ms发送超时
        private boolean isLog;
        private RxAudioPlayer.IAudioEncoder audioEncoder;
        private AbstractDecoder abstractDecoder;
        private AudioRecorder.IConnectListener recorderConnectListener;

        /**
         * 设置录音配置
         */
        public AudioConfig recordBuilder(AudioRecorder.Builder builder) {
            mRecorderBuilder = builder;
            return this;
        }

        /**
         * 设置播放配置
         */
        public AudioConfig trackBuilder(RxAudioPlayer.Builder builder) {
            mAudioPlayerBuilder = builder;
            return this;
        }

        /**
         * 设置供电配置
         */
        public AudioConfig powerBuilder(PowerSupply.Builder builder) {
            mPowerBuilder = builder;
            return this;
        }

        public AudioConfig retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public AudioConfig retryDelay(int retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public AudioConfig receiveTimeOut(int timeOut) {
            this.receiveTimeOut = timeOut;
            return this;
        }

        public AudioConfig isLog(boolean log) {
            this.isLog = log;
            return this;
        }

        public AudioConfig audioEncoder(RxAudioPlayer.IAudioEncoder audioEncoder) {
            this.audioEncoder = audioEncoder;
            return this;
        }

        public AudioConfig audioDecoder(AbstractDecoder audioDecoder) {
            this.abstractDecoder = audioDecoder;
            return this;
        }

        public AudioConfig recorderConnectListener(AudioRecorder.IConnectListener recorderConnectListener) {
            this.recorderConnectListener = recorderConnectListener;
            return this;
        }
    }
}
