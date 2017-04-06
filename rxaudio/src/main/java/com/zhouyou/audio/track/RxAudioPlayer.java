package com.zhouyou.audio.track;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.zhouyou.audio.exception.PlayException;
import com.zhouyou.audio.exception.RetryWhenProcess;
import com.zhouyou.audio.record.AudioRecorder;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.Channel;
import com.zhouyou.audio.util.RxSchedulers;
import com.zhouyou.audio.util.SimpleSubscribe;
import com.zhouyou.audio.util.Utils;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import rx.Observable;

import static android.R.attr.delay;


/**
 * <p>描述：播放实现类</p>
 * AudioTrack api有很多的功能，本类AudioPlayer封装只是针对给硬件发送数据使用的，具有特定的使用场景。<br>
 * 1.支持AudioTrack.MODE_STATIC模式和AudioTrack.MODE_STREAM<br>
 * 2.不支持音乐播放<br>
 * 3.支持发送错误，重试次数<br>
 * <p>
 * 使用举例：<br>
 * RxAudioPlayer audioPlayer = new RxAudioPlayer.Builder()
 * .streamType(AudioManager.STREAM_MUSIC)
 * .rate(8000)
 * .channelConfig(AudioFormat.CHANNEL_OUT_MONO)
 * .audioFormat(AudioFormat.ENCODING_PCM_16BIT)
 * .channel(Channel.LEFT)//左声道通信
 * .build();<br>
 * audioPlayer.rxSend((byte) 0x5A)//发送单个数据<br>
 * audioPlayer.rxSend(new byte[])//发送数组<br>
 * audioPlayer.rxSend((byte) 0x5A, 2, 100)//发送单个数据，失败重试2次，每次间隔100ms<br>
 * audioPlayer.rxSend(new byte[], 2, 100)//发送数组，失败重试2次，每次间隔100ms<br>
 * </p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/5 17:38<br>
 * 版本： v2.0<br>
 */
public final class RxAudioPlayer {
    public static final int DEFAULT_SAMPLERATEINHZ = 8000;//播放频率
    public static final int DEFAULT_CHANNELCONFIG = AudioFormat.CHANNEL_OUT_MONO; //单声道
    public static final int DEFAULT_AUDIOFORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_STREAMTYPE = AudioManager.STREAM_MUSIC;
    public static final int DEFAULT_MODE = AudioTrack.MODE_STATIC;
    private AudioTrack audioTrack = null;
    private int minBufferSize;
    private int mode = DEFAULT_MODE;
    private int streamType = DEFAULT_STREAMTYPE;
    private int rate = DEFAULT_SAMPLERATEINHZ;
    private int channelConfig = DEFAULT_CHANNELCONFIG;
    private int audioFormat = DEFAULT_AUDIOFORMAT;
    private int blocksize_buf;
    private IAudioEncoder mIAudioEncoder;
    private boolean isLog = true;
    private boolean isPlay;
    private final ReadWriteLock mLock = new ReentrantReadWriteLock();
    private Channel channel = Channel.LEFT;

    private volatile static RxAudioPlayer singleton = null;

    private RxAudioPlayer(RxAudioPlayer.Builder builder) {
        config(builder);
    }

    public static RxAudioPlayer getInstance() {
        if (singleton == null) {
            synchronized (AudioRecorder.class) {
                if (singleton == null) {
                    singleton = new RxAudioPlayer.Builder().build();
                }
            }
        }
        return singleton;
    }

    /**
     * 创建一个新的builder参数
     *
     * @param builder
     */
    public void newBulder(RxAudioPlayer.Builder builder) {
        config(builder);
    }

    private void config(Builder builder) {
        streamType = builder.streamType;
        rate = builder.rate;
        channelConfig = builder.channelConfig;
        audioFormat = builder.audioFormat;
        blocksize_buf = builder.blocksize_buf;
        channel = builder.channel;
        mode = builder.mode;
        isLog = builder.isLog;
    }

    private void iniAudioTrack() {
        try {
            minBufferSize = AudioTrack.getMinBufferSize(rate, channelConfig, audioFormat);
            if (minBufferSize == AudioTrack.ERROR_BAD_VALUE || minBufferSize == AudioTrack.ERROR) {
                throw new PlayException(minBufferSize, "initialization failed!!!");
            }
            final int blocksize_size = (blocksize_buf > minBufferSize ? blocksize_buf : minBufferSize) * 2;
            audioTrack = new AudioTrack(streamType, rate, channelConfig,
                    audioFormat, blocksize_size,
                    mode);
            print("blocksize_size:" + blocksize_size);
            if (audioTrack.getPlayState() != AudioTrack.STATE_INITIALIZED) {
                audioTrack.release();
                audioTrack = null;
                throw new PlayException(audioTrack.getPlayState(), "initialization failed!!!");
            }
        } catch (Exception e) {
            throw new PlayException(-1, "initialization failed!!!");
        }
    }

    /**
     * 发送数据到设备 没有错误重试
     *
     * @param datas
     * @return
     */
    public Observable<Boolean> rxSend(final byte[] datas) {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            public Boolean execute() throws Throwable {
                return send(datas);
            }
        }).compose(RxSchedulers.<Boolean>_io_main());
    }

    /**
     * 发送数据到设备
     *
     * @param datas      发送的字节数组
     * @param retryCount 发送失败，自动重试次数
     * @param retryDelay      重试间隔时间
     * @return
     */
    public Observable<Boolean> rxSend(final byte[] datas, int retryCount, long retryDelay) {
        if(retryCount<1){
            throw new IllegalArgumentException("retryCount<1");
        }
        if(retryDelay<0){
            throw new IllegalArgumentException("retryDelay<0");
        }
        return this.rxSend(datas).retryWhen(new RetryWhenProcess(retryCount, delay));
    }

    /**
     * 发送数据
     *
     * @param data
     */
    public boolean send(byte[] data) {
        mLock.readLock().lock();
        try {
            print("Thread name:"+Thread.currentThread().getName());
            if (isPlay) {
                print("send Already running!!!!!!!!!!!");
                return false;
            }
            isPlay = true;
            boolean isSend;
            stop();//停掉播放
            isSend = play(data);
            mIAudioEncoder.finishEncoder();
            return isSend;
        } finally {
            isPlay = false;
            mLock.readLock().unlock();
        }
    }


    /**
     * 播放发送的数据
     *
     * @param datas
     */
    private boolean play(byte[] datas) {
        if (mIAudioEncoder == null) {
            Utils.checkNotNull(mIAudioEncoder, "mIAudioEncoder == null");
        }
        final byte[] sendData = datas;
        mIAudioEncoder.startEncoder(rate);//开始编码
        byte[] encoderData = mIAudioEncoder.encoderData(sendData);
        if (encoderData == null || encoderData.length == 0) {
            Utils.checkNotNull(encoderData, "encoderData == null or encoderData len == 0");
        }
        print("encodeData len :" + encoderData.length + "," + Arrays.toString(encoderData));
        if (mode == AudioTrack.MODE_STATIC) {//STATIC方式发送
            int resert_buf = blocksize_buf;
            if(encoderData.length>blocksize_buf)blocksize_buf=encoderData.length;//对blocksize_buf做一个动态的修改
            iniAudioTrack();
            int writeReslut = audioTrack.write(encoderData, 0, encoderData.length);
            audioTrack.flush();
            setChannel(channel);
            audioTrack.play();
            blocksize_buf = resert_buf;//发送完成以后，恢复blocksize_buf
            if (writeReslut == encoderData.length) {
                print("write success!!!!" + writeReslut);
                return true;
            } else {
                throw new PlayException(-1, "Play failed !!!" + writeReslut);
            }
        } else if (mode == AudioTrack.MODE_STREAM) {//STREAM方式发送
            iniAudioTrack();
            audioTrack.play();
            final int length = encoderData.length / datas.length;
            if (minBufferSize < length && blocksize_buf < length) {
                throw new IllegalStateException("blocksize_buf is too small!!!");
            }
            boolean isSend = false;
            for (int i = 0; i < datas.length; i++) {
                int writeReslut = audioTrack.write(encoderData, i * length, length);
                if (writeReslut == length) {
                    isSend = true;
                } else {
                    throw new PlayException(-1, "Play failed !!!" + writeReslut);
                }
                print("writeReslut :"+writeReslut);
            }
            print("write success!!!!");
            return isSend;
        }
        return false;
    }

    public void setIAudioEncoder(IAudioEncoder IAudioEncoder) {
        mIAudioEncoder = IAudioEncoder;
    }

    /**
     * 是否在播放
     *
     * @return
     */
    public boolean isPlay() {
        return isPlay;
    }

    public void print(String msg) {
        if (isLog) AudioLog.i(msg);
    }

    /**
     * 释放资源
     */
    public void stop() {
        try {
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
        } catch (Exception e) {
            print("Exception:" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置声道
     *
     * @param channel
     */
    public void setChannel(Channel channel) {
        if (audioTrack != null) {
            switch (channel) {
                case LEFT:
                    audioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMinVolume());
                    break;
                case RIGHT:
                    audioTrack.setStereoVolume(AudioTrack.getMinVolume(), AudioTrack.getMaxVolume());
                    break;
                case DOUBLE:
                    audioTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
                    break;
            }
        }
    }

    public static final class Builder {
        private int streamType = DEFAULT_STREAMTYPE;
        private int rate = DEFAULT_SAMPLERATEINHZ;
        private int channelConfig = DEFAULT_CHANNELCONFIG;
        private int audioFormat = DEFAULT_AUDIOFORMAT;
        private int blocksize_buf = 1024;
        private int mode = DEFAULT_MODE;
        private Channel channel = Channel.LEFT;
        private boolean isLog = true;

        public Builder() {
        }

        public Builder streamType(int val) {
            streamType = val;
            return this;
        }
        public Builder isLog(boolean val) {
            isLog = val;
            return this;
        }

        public Builder mode(int val) {
            mode = val;
            return this;
        }

        public Builder rate(int val) {
            rate = val;
            return this;
        }

        public Builder channelConfig(int val) {
            channelConfig = val;
            return this;
        }

        public Builder audioFormat(int val) {
            audioFormat = val;
            return this;
        }

        public Builder blocksizeBuf(int val) {
            blocksize_buf = val;
            return this;
        }

        public Builder channel(Channel val) {
            channel = val;
            return this;
        }

        public RxAudioPlayer build() {
            return new RxAudioPlayer(this);
        }
    }

    /**
     * <p>描述：编码接口</p>
     * 作者： ~若相惜<br>
     * 日期： 2017/2/8 20:01<br>
     * 版本： v2.0<br>
     */
    public interface IAudioEncoder {

        /**
         * 初始化编码数据
         *
         * @param sampleRate 设置的采样率
         */
        void startEncoder(int sampleRate);

        /**
         * 编码发送的数据
         *
         * @return 返回编码后的数组
         */
        byte[] encoderData(byte[] bytes);

        /**
         * 结束编码数据
         */
        void finishEncoder();

    }
}
