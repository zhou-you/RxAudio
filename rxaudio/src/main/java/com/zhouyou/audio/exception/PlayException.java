package com.zhouyou.audio.exception;

public class PlayException extends RuntimeException {
    public int errCode;
    public String message;

    public PlayException(int errCode, String msg) {
        super(msg);
        this.errCode = errCode;
        this.message = msg;
    }

    public int getErrCode() {
        return errCode;
    }
}