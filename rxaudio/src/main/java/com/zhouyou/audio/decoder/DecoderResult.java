package com.zhouyou.audio.decoder;

/**
 * <p>描述：解码结果</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/25 14:00<br>
 * 版本： v2.0<br>
 */
public class DecoderResult {
    public static final int DECODE_OK=0;//接收成功
    public static final int DECODE_ERR=-1;//接收失败
    public int code;//0成功
    public String errMsg;
    public byte[] data;//数据

    public DecoderResult(int code, byte[] data) {
        this.code = code;
        this.data = data;
    }

    public DecoderResult(int code, String errMsg) {
        this.errMsg = errMsg;
        this.code = code;
        this.data = null;
    }
}
