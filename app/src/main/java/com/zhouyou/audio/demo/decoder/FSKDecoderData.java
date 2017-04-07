package com.zhouyou.audio.demo.decoder;

import com.zhouyou.audio.decoder.AbstractDecoder;
import com.zhouyou.audio.decoder.DecoderResult;
import com.zhouyou.audio.util.AudioLog;

import org.jtransforms.fft.DoubleFFT_1D;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * <p>描述：信号调制，FFt解码</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/3/8 18:19<br>
 * 版本： v2.0<br>
 */
public class FSKDecoderData extends AbstractDecoder {
    private int sampleRate;

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isRuning() {
        return false;
    }

    @Override
    public void startDecoder(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public void decoderData(short[] audio_data, int bufferReadResult) throws Exception {
        //具体的解码，涉及到业务具体通信相关已经取消
    }

    /**
     * 失败消息
     *
     * @param errCode
     * @param errMsg
     */
    private void decoderDataByFail(final int errCode, final String errMsg) {
        print(errMsg);
        Observable.just(errCode).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Object>() {
            @Override
            public void call(Object o) {
                if (mRecCallBack != null)
                    mRecCallBack.callResult(new DecoderResult(errCode, errMsg));
            }
        });
    }

    /**
     * 成功消息
     *
     * @param decodData 解码的数据
     */
    private void decoderDataBySuccess(final byte[] decodData) {
        Observable.just(decodData).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Object>() {
            @Override
            public void call(Object o) {
                if (mRecCallBack != null)
                    mRecCallBack.callResult(new DecoderResult(DecoderResult.DECODE_OK, decodData));
            }
        });
    }


    @Override
    public void finishDecoder() {

    }

    @Override
    public void recoderState(boolean isPause) {
    }
    
    /**
     * 傅里叶变化从原始信号中获取频率信息
     * @param signal
     * @return 
     */
    private int calculateFFT(short[] signal) {
        int fftLent = 256;
        DoubleFFT_1D fft = new DoubleFFT_1D(fftLent);
        double[] trama = new double[fftLent * 2];
        for (int i = 0; i < signal.length - 1 && i < (fftLent - 1); i++) {
            trama[i * 2] = (double) signal[i];
            trama[i * 2 + 1] = 0.0f;
        }
        fft.realForwardFull(trama);
        double[] mag = new double[signal.length / 2];
        for (int i = 0; i < (signal.length / 2) - 1; i++) {
            double real = trama[2 * i];
            double imag = trama[2 * i + 1];
            mag[i] = Math.sqrt((real * real) + (imag * imag));
        }

        double max_mag = 0.0;
        int max_index = -1;
        for (int j = 0; j < (signal.length / 2) - 1; j++) {
            if (mag[j] > max_mag) {
                max_mag = mag[j];
                max_index = j;
            }
        }
        //final int peak = max_index * this.sampleRate / fftLent * 2;
        //print("Peak Frequency = " + max_index + " = " + peak);
        return max_index;
    }

    //检测是位1还是位0
    public int getBit(int nMaxIndex) {
        if (nMaxIndex == 6 || nMaxIndex == 7 || nMaxIndex == 8 || nMaxIndex == 9)
            return 1;
        if (nMaxIndex == 3 || nMaxIndex == 4 || nMaxIndex == 5 || nMaxIndex == 2)
            return 0;
        //print(String.format("nMaxIndex=%d", nMaxIndex));
        return -1;
    }

    public void print(String msg) {
        AudioLog.i(msg);
    }
}
