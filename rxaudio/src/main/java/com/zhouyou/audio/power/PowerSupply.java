package com.zhouyou.audio.power;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.zhouyou.audio.base.BaseAudio;
import com.zhouyou.audio.record.AudioRecorder;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.Channel;
import com.zhouyou.audio.util.SinWave;

import static com.zhouyou.audio.util.Channel.RIGHT;


/**
 * <p>描述：正弦波产生类，通过正弦波向目标板供电</p>
 * 1.这种方式可以输出21KHZ频率固定的正玄波<br>
 * 2.不同的设备输出的电压不同。
 * 3.所有回调都在主线程。
 * <p>
 * 使用举例：<br>
 * PowerSupply mSuply = new PowerSupply.Builder()<br>
 * .sampleRate(44100)<br>
 * .channelConfig(AudioFormat.CHANNEL_CONFIGURATION_MONO)<br>
 * .audioFormat(AudioFormat.ENCODING_PCM_16BIT)<br>
 * .channel(PowerSupply.Channel.RIGHT)<br>
 * .powerRate(21500)//设置输出正玄波频率<br>
 * .build();<br>
 * mSuply.start()<br>
 * mSuply.stop()<br>
 * </p>
 * <p>
 * <p>
 * 注意：如果想输出21KHZ的正玄波，只能用自定义的48000不能用44100.<br>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/5 17:38<br>
 * 版本： v2.0<br>
 */
public final class PowerSupply extends BaseAudio {
    public static final int DEFAULT_SAMPLERATEINHZ = 44100;//播放频率
    public static final int DEFAULT_CHANNELCONFIG = AudioFormat.CHANNEL_OUT_MONO; //单声道
    public static final int DEFAULT_AUDIOFORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_STREAMTYPE = AudioManager.STREAM_MUSIC;
    public static final int DEFAULT_MODE = AudioTrack.MODE_STATIC;
    private AudioTrack audioTrack = null;
    private int minBufferSize;
    private int streamType = DEFAULT_STREAMTYPE;
    private int sampleRate = DEFAULT_SAMPLERATEINHZ;
    private int channelConfig = DEFAULT_CHANNELCONFIG;
    private int audioFormat = DEFAULT_AUDIOFORMAT;
    private int sin_rate;
    //默认声道
    private Channel channel = RIGHT;
    private boolean isLog = true;
    private volatile static PowerSupply singleton = null;

    private PowerSupply(PowerSupply.Builder builder) {
        config(builder);
    }

    public static PowerSupply getInstance() {
        if (singleton == null) {
            synchronized (AudioRecorder.class) {
                if (singleton == null) {
                    singleton = new PowerSupply.Builder().build();
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
    public void newBulder(PowerSupply.Builder builder) {
        config(builder);
    }

    private void config(Builder builder) {
        isLog = builder.isLog;
        streamType = builder.streamType;
        sampleRate = builder.sampleRate;
        channelConfig = builder.channelConfig;
        audioFormat = builder.audioFormat;
        sin_rate = builder.sin_rate;
        channel = builder.channel;
    }

    public void setpowerRate(int sin_rate) {
        this.sin_rate = sin_rate;
    }

    @Override
    protected void doStart() {
        print("PowerSupply doStart!!!!!!!!!!!!!");
        trackStop();
        if (sin_rate < 0) {
            throw new IllegalStateException("please Set positive wave frequency!!! sin_rate<0");
        }
        minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        if (minBufferSize == AudioTrack.ERROR_BAD_VALUE || minBufferSize == AudioTrack.ERROR) {
            return;
        }
        final int numSamples = sampleRate;
        audioTrack = new AudioTrack(streamType, sampleRate, channelConfig,
                audioFormat, numSamples, DEFAULT_MODE);
        int state = audioTrack.getPlayState();
        if (state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release();
            audioTrack = null;
            return;
        }
        try {
            final byte[] wave = SinWave.sin(sin_rate, sampleRate, numSamples);
            audioTrack.write(wave, 0, numSamples * 2);
            audioTrack.flush();
            setChannel(channel);
            audioTrack.setLoopPoints(0, numSamples / 2, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doStop() {
        print("PowerSupply doStop!!!!!!!!!!!!!");
        trackStop();
    }

    private void trackStop() {
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doRun() {
        try {
            if (audioTrack != null) {
                audioTrack.play();
            }
        } catch (Exception e) {
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

    public void print(String msg) {
        if (isLog) AudioLog.i(msg);
    }

    public static final class Builder {
        private boolean isLog = true;
        private int streamType = DEFAULT_STREAMTYPE;
        private int sampleRate = DEFAULT_SAMPLERATEINHZ;
        private int channelConfig = DEFAULT_CHANNELCONFIG;
        private int audioFormat = DEFAULT_AUDIOFORMAT;
        private int sin_rate;
        private Channel channel = Channel.RIGHT;

        public Builder() {
        }

        public Builder isLog(boolean isLog) {
            this.isLog = isLog;
            return this;
        }

        public Builder streamType(int streamType) {
            this.streamType = streamType;
            return this;
        }

        public Builder sampleRate(int rate) {
            sampleRate = rate;
            return this;
        }

        public Builder channelConfig(int channelConfig) {
            this.channelConfig = channelConfig;
            return this;
        }

        public Builder audioFormat(int audioFormat) {
            this.audioFormat = audioFormat;
            return this;
        }

        public Builder powerRate(int sin_rate) {
            this.sin_rate = sin_rate;
            return this;
        }

        public Builder channel(Channel val) {
            channel = val;
            return this;
        }

        public PowerSupply build() {
            return new PowerSupply(this);
        }
    }
}
