package com.zhouyou.audio.decoder;

/**
 * <p>描述：解码操作后发送结果回调</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/15 16:32<br>
 * 版本： v2.0<br>
 */
public interface IDecoderCallBack {
    /**
     * 回调结果
     * @return
     */
    void callResult(DecoderResult result);
}
