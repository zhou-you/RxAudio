package com.zhouyou.audio.base;

/**
 * <p>描述：发送响应数据回调</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/25 15:52<br>
 * 版本： v2.0<br>
 */
public interface ICallBack {
    /**
     * 成功回调
     * @param recData  回调的数据
     */
    void onSuccess(byte[] recData);
    /**
     * 失败回调
     * @param throwable
     */
    void onFailure(Throwable throwable);
}
