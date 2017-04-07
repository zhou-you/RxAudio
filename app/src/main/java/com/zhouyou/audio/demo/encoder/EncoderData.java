package com.zhouyou.audio.demo.encoder;

import com.zhouyou.audio.track.RxAudioPlayer;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.SinWave;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * <p>描述：实现发送的数据组装</p>
 * 1.将byte数据转换为硬件协议需要的15位二进制01，起始码（4）+数据长度8位（8）<br>
 * 2.发送一个字节按照协议编码后对应的二进制01<br>
 * 3.把二进制的01转化成频率的正玄波数组<br>
 * 4.间歇发送数据
 * 作者： ~若相惜<br>
 * 日期： 2017/2/9 13:39<br>
 * 版本： v2.0<br>
 */
public class EncoderData implements RxAudioPlayer.IAudioEncoder {
    private static final String START_CODE = "1010";//起始码
    private static final String END_CODE = "01";//结束码
    private static final int bit = 8;//数据长度8位
    private static final int validate = 1;//校验码1位
    private static final int all_len = START_CODE.length() + bit + validate + END_CODE.length();//硬件发送数据最长是15
    private static final int HIGH_FREQ = 2000;
    private static final int LOW_FREQ = 1000;
    private static final int duration = 3;//3ms
    private static final int INTERIM = 10;//发送两个字节之间间歇的时间 duration*INTERIM
    private static byte[] interim_buf;
    private int audioBufLength;
    private int duration_len;
    private int sampleRate;
    private ByteBuffer mByteDataBuf;
    private byte[] LOW_FREQ_BYTE;
    private byte[] HIGH_FREQ_BYTE;

    @Override
    public void startEncoder(int sampleRate) {
        this.sampleRate = sampleRate;
        duration_len = duration * sampleRate / 1000;
        audioBufLength = duration_len * 2 * all_len;
        interim_buf = new byte[duration_len * INTERIM];
        LOW_FREQ_BYTE = SinWave.sin(LOW_FREQ, this.sampleRate, duration_len);
        HIGH_FREQ_BYTE = SinWave.sin(HIGH_FREQ, this.sampleRate, duration_len);
    }

    @Override
    public byte[] encoderData(byte[] bytes) {
        mByteDataBuf = ByteBuffer.allocate(audioBufLength * bytes.length + interim_buf.length * bytes.length);
        for (byte aByte : bytes) {
            byte[] single_wave_all = bitToFreqWave(aByte);
            mByteDataBuf.put(single_wave_all);
            mByteDataBuf.put(interim_buf);//间隔10ms发送数据，这里采用的办法是用0填充
        }
        return mByteDataBuf.array();
    }

    /**
     * 把二进制的01转化成频率的正玄波数组
     *
     * @param data
     * @return
     */
    private byte[] bitToFreqWave(byte data) {
        final short[] audioBit = bitShort(data);
        final String strAlldata = Arrays.toString(audioBit).replaceAll("[\\[\\]\\s,]", "");
        print("strAlldata:" + strAlldata);
        ByteBuffer mByteDataBuf = ByteBuffer.allocate(audioBufLength);
        byte[] waveLen = new byte[duration_len * 2];
        for (short i : audioBit) {
            switch (i) {
                case 0:
                    waveLen = LOW_FREQ_BYTE;
                    break;
                case 1:
                    waveLen = HIGH_FREQ_BYTE;
                    break;
            }
            mByteDataBuf.put(waveLen);
        }
        mByteDataBuf.flip();
        int all_length = mByteDataBuf.limit();
        byte[] wave_all = new byte[all_length];
        mByteDataBuf.get(wave_all);
        return wave_all;
    }

    @Override
    public void finishEncoder() {
        mByteDataBuf = null;
    }

    /**
     * 发送一个字节按照协议编码后对应的二进制01
     *
     * @param audioData 字节数据
     * @return 返回编码后对应的二进制数组
     */
    private short[] bitShort(byte audioData) {
        short[] audioBit = new short[all_len];
        byte paritybit = 0;//校验值
        //填写起始位
        for (int i = 0; i < START_CODE.length(); i++) {//起始码
            audioBit[i] = Short.valueOf(START_CODE.substring(i, i + 1));
            paritybit += audioBit[i];
        }
        int dataStartIndex = START_CODE.length();
        //填写数据位
        final int srcDataLen = dataStartIndex + bit;
        for (int counter_i = dataStartIndex; counter_i < srcDataLen; counter_i++) {
            if (((audioData >> (srcDataLen - counter_i - 1) & 0x01) == 0x01))
                audioBit[counter_i] = 1;
            else
                audioBit[counter_i] = 0;
            paritybit += audioBit[counter_i];
        }

        print("paritybit:" + (paritybit & 0x01));
        //填写校验位
        if ((paritybit & 0x01) == 0x01) {
            audioBit[dataStartIndex + bit] = 1;
        } else {
            audioBit[dataStartIndex + bit] = 0;
        }
        //填写结束位
        int endIndex = dataStartIndex + bit + 1;
        for (int i = endIndex; i < (endIndex + END_CODE.length()); i++) {//结束码
            audioBit[i] = Short.valueOf(END_CODE.substring(i - endIndex, i - endIndex + 1));
            paritybit += audioBit[i];
        }
        return audioBit;
    }

    private void print(String msg) {
        AudioLog.i(msg);
    }
}
