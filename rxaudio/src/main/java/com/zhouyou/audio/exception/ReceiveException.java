package com.zhouyou.audio.exception;

public class ReceiveException extends RuntimeException {
    public int errCode;
    public String message;

    public ReceiveException(int errCode, String msg) {
        super(msg);
        this.errCode = errCode;
        this.message = msg;
    }

    public int getErrCode() {
        return errCode;
    }
}