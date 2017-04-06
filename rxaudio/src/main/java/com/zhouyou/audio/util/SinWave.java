package com.zhouyou.audio.util;

/**
 * <p>描述：生成用于发送设备的正弦波</p><br>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/5 17:38<br>
 * 版本： v2.0<br>
 */
public class SinWave {
    public static byte[] sin(double freqOfTone, int sampleRate,int numSamples) {
        final byte[] wave = new byte[2 * numSamples];
        final double samples[] = new double[numSamples];
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            samples[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
        }
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (double dVal : samples) {
            // scale to maximum amplitude
            short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            wave[idx++] = (byte) (val & 0x00ff);
            wave[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return wave;
    }
}

