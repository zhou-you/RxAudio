package com.zhouyou.audio.exception;

public class RecorderException extends Exception {
    public int errCode;
    public String message;

    public RecorderException(int errCode, String msg) {
        super(msg);
        this.errCode = errCode;
        this.message = msg;
    }

    public int getErrCode() {
        return errCode;
    }
}