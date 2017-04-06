package com.zhouyou.audio.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;

import com.zhouyou.audio.base.BaseAudio;
import com.zhouyou.audio.exception.RecorderException;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.Utils;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * <p>描述：录音实现类</p>
 * <p>
 * 使用举例：<br>
 * AudioRecorder recorder = new AudioRecorder.Builder()<br>
 * .reate(8000)<br>
 * .audioEncoding(AudioFormat.ENCODING_PCM_16BIT)<br>
 * .audioSource(MediaRecorder.AudioSource.MIC)<br>
 * .channelConfiguration(AudioFormat.CHANNEL_IN_MONO)<br>
 * .build();<br>
 * recorder.start()<br>
 * recorder.stop()<br>
 * </p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/4 16:40<br>
 * 版本： v2.0<br>
 */
public final class AudioRecorder extends BaseAudio {
    private AudioRecord mAudioRecord = null;
    public static final int DEFAULT_AUDIOSOURCE = MediaRecorder.AudioSource.MIC;
    public static final int DEFAULT_RATE = 8000;
    public static final int DEFAULT_CHANNELCONFIGURATION = AudioFormat.CHANNEL_IN_MONO;
    public static final int DEFAULT_AUDIOENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int audioSource = DEFAULT_AUDIOSOURCE;
    private int reate = DEFAULT_RATE;
    private int channelConfiguration = DEFAULT_CHANNELCONFIGURATION;
    private int audioEncoding = DEFAULT_AUDIOENCODING;
    private int minBufferSize;
    private int bufferReadResult;
    private int readBufferSzie = 1024;
    private IConnectListener mConnectListener;
    private IAudioDecoder mAudioDecoder;
    private boolean isLog = true;
    private volatile static AudioRecorder singleton = null;
    private boolean mPause;
    private AudioRecorder(Builder builder) {
        config(builder);
    }

    public static AudioRecorder getInstance() {
        if (singleton == null) {
            synchronized (AudioRecorder.class) {
                if (singleton == null) {
                    singleton = new Builder().build();
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
    public void newBulder(Builder builder) {
        config(builder);
    }

    private void config(Builder builder) {
        reate = builder.reate;
        channelConfiguration = builder.channelConfiguration;
        audioSource = builder.audioSource;
        audioEncoding = builder.audioEncoding;
        readBufferSzie = builder.readBufferSzie;
        mConnectListener = builder.connectListener;
        isLog = builder.isLog;
    }

    @Override
    public void doStart() {
        print("recorder doStart !!!!!!!!!!!!!");
    }

    private void initAudioRecorder() {
        try {
            minBufferSize = AudioRecord.getMinBufferSize(reate, channelConfiguration, audioEncoding);
            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
                if (mConnectListener != null) {
                    Observable.just(minBufferSize).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer o) {
                            mConnectListener.onFailure(new RecorderException(o, "initialization err"), o);
                        }
                    });
                }
                return;
            }
            mAudioRecord = new AudioRecord(audioSource, reate, channelConfiguration, audioEncoding, minBufferSize * 10);
            int nState = mAudioRecord.getState();
            if (nState != AudioRecord.STATE_INITIALIZED) {
                mAudioRecord.release();
                mAudioRecord = null;
                if (mConnectListener != null) {
                    Observable.just(nState).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer o) {
                            mConnectListener.onFailure(new RecorderException(o, "initialization err"), o);
                        }
                    });
                }
                return;
            }
            if (readBufferSzie <= 0 || readBufferSzie > minBufferSize) {
                readBufferSzie = minBufferSize;
            }
            nState = mAudioRecord.getRecordingState();
            if (nState != AudioRecord.STATE_INITIALIZED) {
                mAudioRecord.release();
                mAudioRecord = null;
                if (mConnectListener != null) {
                    Observable.just(nState).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer o) {
                            mConnectListener.onFailure(new RecorderException(o, "initialization err"), o);
                        }
                    });
                }
                return;
            }
            mAudioRecord.startRecording();
            //阻塞直到开始转态
            while (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                // Wait until we can start recording...
            }
            if (mConnectListener != null) {
                Observable.just("SUCCESS").observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
                    @Override
                    public void call(String o) {
                        mConnectListener.onSuccess();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (mConnectListener != null) {
                Observable.just(e).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Exception>() {
                    @Override
                    public void call(Exception e) {
                        mConnectListener.onFailure(e, -1);
                    }
                });
            }
        }
    }

    @Override
    public void doRun() {
        try {
            Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            initAudioRecorder();
            if (mAudioDecoder != null) mAudioDecoder.startDecoder(reate);
            final short[] audio_data = new short[readBufferSzie];
            while (isRuning()) {
                //如果是暂停状态不录音
                if (mPause) {
                    if (mAudioDecoder != null)mAudioDecoder.recoderState(mPause);
                    Thread.sleep(100);
                    continue;
                }
                bufferReadResult = mAudioRecord.read(audio_data, 0, audio_data.length);
                //print(Arrays.toString(audio_data));
                if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION || bufferReadResult == AudioRecord.ERROR_BAD_VALUE) {
                    print("bufferReadResult err !!!!!!!!!!!!!");
                    continue;
                }
                if (bufferReadResult > 0) {
                    if (mAudioDecoder != null)
                        mAudioDecoder.decoderData(audio_data, bufferReadResult);
                } else {
                    print("error: " + bufferReadResult);
                }
            }
            if (mAudioDecoder != null) mAudioDecoder.finishDecoder();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            print("err!!:"+e.getMessage());
            e.printStackTrace();
        } finally {
            if (mAudioRecord != null) {
                try {
                    mAudioRecord.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
    }

    @Override
    public void doStop() {
        print("recorder doStop !!!!!!!!!!!!!");
    }

    public void setAudioDecoder(IAudioDecoder audioDecoder) {
        mAudioDecoder = audioDecoder;
    }

    public void print(String msg) {
        if (isLog) AudioLog.i(msg);
    }

    /**
     * 暂停
     */
    public void pause() {
        mPause = true;
    }

    /**
     * 继续
     */
    public void resume() {
        mPause = false;
    }
    public static final class Builder {
        private int reate;
        private int channelConfiguration;
        private int audioEncoding;
        private int audioSource;
        private int readBufferSzie;
        private boolean isLog = true;
        private IConnectListener connectListener;
        public Builder() {
            this.reate = DEFAULT_RATE;
            this.channelConfiguration = DEFAULT_CHANNELCONFIGURATION;
            this.audioEncoding = DEFAULT_AUDIOENCODING;
            this.audioSource = DEFAULT_AUDIOSOURCE;
        }

        public Builder isLog(boolean val) {
            isLog = val;
            return this;
        }

        public Builder connectListener(IConnectListener connectListener) {
            Utils.checkNotNull(connectListener, "connectListener==null");
            this.connectListener = connectListener;
            return this;
        }

        /**
         * 录音来源
         *
         * @param val
         * @return
         */
        public Builder audioSource(int val) {
            audioSource = val;
            return this;
        }

        /**
         * 设置采样率 默认：8000  录制频率，可以为8000hz或者11025hz等，不同的硬件设备这个值不同
         *
         * @param val
         * @return
         */
        public Builder reate(int val) {
            reate = val;
            return this;
        }

        /**
         * 录制通道，可以为AudioFormat.CHANNEL_CONFIGURATION_MONO和AudioFormat.CHANNEL_CONFIGURATION_STEREO
         *
         * @param val
         * @return
         */
        public Builder channelConfiguration(int val) {
            channelConfiguration = val;
            return this;
        }

        /**
         * 录制编码格式，可以为AudioFormat.ENCODING_16BIT和8BIT,其中16BIT的仿真性比8BIT好，
         * 但是需要消耗更多的电量和存储空间
         *
         * @param val
         * @return
         */
        public Builder audioEncoding(int val) {
            audioEncoding = val;
            return this;
        }

        /**
         * readBufferSzie<=0则用minBufferSize，否则用minBufferSize
         *
         * @param val
         * @return
         */
        public Builder readBufferSzie(int val) {
            readBufferSzie = val;
            return this;
        }

        public AudioRecorder build() {
            return new AudioRecorder(this);
        }
    }

    /**
     * <p>描述：解码接口</p>
     * 1.所有回调方法都在线程中执行<br>
     * 作者： ~若相惜<br>
     * 版本： v2.0<br>
     */
    public interface IAudioDecoder {
        /**
         * 开始解码
         */
        void startDecoder(int sampleRate);

        /**
         * 回调信号数据
         *
         * @param audio_data       读取的信号数据
         * @param bufferReadResult 读取的实际数据长度
         */
        void decoderData(short[] audio_data, int bufferReadResult)throws Exception;

        /**
         * 结束解码
         */
        void finishDecoder();

        /**
         * 录音转态
         * @param isPause //是否是暂停
         */
        void recoderState(boolean isPause);
    }

    /**
     * <p>描述：录音连接转态</p>
     * 作者： ~若相惜<br>
     * 版本： v2.0<br>
     */
    public interface IConnectListener {
        /**
         * 连接成功
         */
        void onSuccess();

        /**
         * 连接失败
         *
         * @param code
         */
        void onFailure(Exception e, int code);
    }
}