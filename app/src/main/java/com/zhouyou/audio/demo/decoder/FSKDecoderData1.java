package com.zhouyou.audio.demo.decoder;

import com.zhouyou.audio.decoder.AbstractDecoder;
import com.zhouyou.audio.decoder.DecoderResult;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.CRCUtil;
import com.zhouyou.audio.util.Tools;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * <p>描述：信号调制，FFt解码</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/3/8 18:19<br>
 * 版本： v2.0<br>
 */
public class FSKDecoderData1 extends AbstractDecoder {
    private short mnMax = 32767;
    private int sampleRate;
    private int bitLen = 78;
    private int mCacheLen = 18990;//10998 14582 14336 华为p6的minbuf是7680
    private int mMaxIndex = 0;
    private short[] mCacheBuffer = new short[mCacheLen];
    private int mCaptureLen = 0;
    private int mCaptureState = 0;        // 状态ֹ
    // 在指定误差范围内检测
    private final int nDetection[] = {142, 141, 140, 139, 138, 137, 143, 144, 145};
    private byte[] dataBytes;

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
        //找出信号中符合条件的最大值
        int mMaxLimit = 7000;
        int mMax = 0;
        if (mCaptureState == 0) {
            for (int i = 0; i < bufferReadResult - 20; i += 1) {
                mMax = (short) Math.max(mMax, Math.abs(audio_data[i]));
                if (mMax >= mnMax || mMax > 1000) {
                    int tempcount = 0;
                    for (int j = 0; j < 12; j++) {
                        if (Math.abs(audio_data[i + j]) > mMaxLimit) tempcount++;
                    }
                    if (tempcount > 8) {
                        mMaxIndex = i;
                        mCaptureState = 1;
                        break;
                    }
                }
            }
        }

        //找到起始点填充buffer缓存
        if (mCaptureState == 1) {
            int nCopyLen = Math.min(bufferReadResult, mCacheLen - mCaptureLen);
            System.arraycopy(audio_data, 0, mCacheBuffer, mCaptureLen, nCopyLen);
            mCaptureLen += nCopyLen;
        }
        boolean bOk = false;
        if (mCaptureLen >= mCacheLen) {
            mCaptureState = 2;
            long mTime = System.currentTimeMillis();
            //print("mMaxIndex:"+mMaxIndex+" ,bufferReadResult"+bufferReadResult);
            for (int i = 0; i < nDetection.length; i++) {
                int nFrameLen = nDetection[i];
                bOk = transform(mCacheBuffer, mMaxIndex, nFrameLen);
                if (bOk) {
                    print("执行一次分析 >> 成功" + nFrameLen);
                    decoderDataBySuccess(dataBytes);
                    break;
                } else {
                    print("执行一次分析 >> 失败" + nFrameLen);
                }
            }
            print(">>>>>>>>>>>>>>>>" + (System.currentTimeMillis() - mTime));
            if (!bOk) {
                //decoderDataByFail(DecoderResult.REC_ERR,"decoder err!!!");
            }
            mCaptureState = 0;
            mCaptureLen = 0;
        }
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
        mCaptureState = 0;
        mCaptureLen = 0;
    }

    // 解释数据  先检查头尾为了提高效率
    public boolean transform(short[] buffer, int mMaxIndex, int nFrameLen) {
        // 数字变换
        final int nBits[] = new int[bitLen];
        short parityBit = 0;//求出校验的数据位和
        short[] shorts = new short[nFrameLen];
        //检查头
        int startLen = 4;
        for (int i = 0; i < startLen; i++) {
            System.arraycopy(buffer, mMaxIndex + i * nFrameLen, shorts, 0, nFrameLen);
            int maxindex = calculateFFT(shorts);
            int nBit = getBit(maxindex);
            if (nBit == -1) return false;
            nBits[i] = nBit;
            parityBit += nBit;
        }
        if (nBits[0] != 1 || nBits[1] != 0 || nBits[2] != 1 || nBits[3] != 0) {
            print(">>>>头部不正确！！！");
            return false;
        }

        //检查尾
        System.arraycopy(buffer, mMaxIndex + (bitLen - 1) * nFrameLen, shorts, 0, nFrameLen);
        int maxindex = calculateFFT(shorts);
        int endBit = getBit(maxindex);
        if (endBit == -1) return false;
        if (endBit != 1) {
            print(">>>>尾部不正确！！！");
            return false;
        }
        nBits[bitLen - 1] = endBit;

        //检查中间
        for (int i = startLen; i < bitLen - 1; i++) {
            System.arraycopy(buffer, mMaxIndex + i * nFrameLen, shorts, 0, nFrameLen);
            int index = calculateFFT(shorts);
            int nBit = getBit(index);
            if (nBit == -1) return false;
            nBits[i] = nBit;
            if (i < bitLen - 2) {
                parityBit += nBit;
            }
        }

        final String result = Arrays.toString(nBits).replaceAll("[\\[\\]\\s,]", "");
        print("result：" + result);
        //print("make validateBit：" + (parityBit & 0x01));
        int validate = nBits[bitLen - 2];
        if ((parityBit & 0x01) != validate) {
            print(">>>>奇偶校验失败！");
            return false;
        }
        final int bytelen = (bitLen - 6) / 8;
        dataBytes = new byte[bytelen];
        for (int i = 0; i < bytelen; i++) {
            byte dataRxByte = 0;
            for (int k = 0; k < 8; k++) {
                dataRxByte |= (nBits[4 + i * 8 + k] << (8 - k - 1));
            }
            dataBytes[i] = dataRxByte;
        }
        byte crc = CRCUtil.calcCrc8(dataBytes, 0, dataBytes.length - 1);
        print("data:" + Tools.byte2Hex(dataBytes));
        if (crc != dataBytes[dataBytes.length - 1]) {
            print(">>>>CRC校验失败:" + Tools.byte2Hex(crc) + " : " + Tools.byte2Hex(dataBytes[dataBytes.length - 1]));
            return false;
        }
        return true;
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
