package com.zhouyou.audio.base;

/**
 * <p>描述：音频接口</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/4 15:35<br>
 * 版本： v2.0<br>
 */
public interface IAudio {
    /**
     * 开始
     */
    void start();

    /**
     * 停止
     */
    void stop();

    /**
     * 是否在运行中
     */
    boolean isRuning();
}
