package com.zhouyou.audio.util;

/**
 * <p>描述：工具类</p>
 * 作者： ~若相惜<br>
 * 日期： 2016/12/20 10:33<br>
 * 版本： v2.0<br>
 */
public class Utils {
    public static <T> T checkNotNull(T t, String message) {
        if (t == null) {
            throw new NullPointerException(message);
        }
        return t;
    }

    public static <T> T checkIllegalArgument(T t, String message) {
        if (t == null) {
            throw new IllegalArgumentException(message);
        }
        return t;
    }
}
