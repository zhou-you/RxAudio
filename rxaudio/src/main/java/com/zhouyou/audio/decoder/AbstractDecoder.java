package com.zhouyou.audio.decoder;

import com.zhouyou.audio.base.IAudio;
import com.zhouyou.audio.record.AudioRecorder;
import com.zhouyou.audio.util.AudioLog;

/**
 * <p>描述：解码基类</p>
 * 1.设置回调接口<br>
 * 2.解码业务都需要继承AbstractDecoder，同时将结果用IReceiveCallBack返回<br>
 * 3.如果解码用线程，只需要在IAudio的start(),stop()方法中开启线程，结束线程就可以了。代码中会自动调用开启线程<br>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/27 19:11<br>
 * 版本： v2.0<br>
 */
public abstract class AbstractDecoder implements AudioRecorder.IAudioDecoder,IAudio {
    private boolean isLog = true;
    public IDecoderCallBack mRecCallBack;

    public AbstractDecoder() {
    }

    public AbstractDecoder(IDecoderCallBack recCallBack) {
        mRecCallBack = recCallBack;
    }

    /**
     * 设置接收回调监听
     * @param recCallBack
     */
    public void setReceiveCallBack(IDecoderCallBack recCallBack) {
        mRecCallBack = recCallBack;
    }

    /**
     * 获取接收回调监听
     * @return
     */
    public IDecoderCallBack getReceiveCallBack() {
        return mRecCallBack;
    }

    public void print(String msg) {
        if (isLog) AudioLog.i(msg);
    }
}
