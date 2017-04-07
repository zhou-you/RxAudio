package com.zhouyou.audio.demo.protocol;

/**
 * <p>描述：发送单个字节到设备</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/20 15:03<br>
 * 版本： v2.0<br>
 */
public class SingleSendProtocol extends BaseSendProtocol {
    private short command;
    private byte[] bytes;

    public SingleSendProtocol(short command, byte param) {
        this.command = command;
        this.bytes = new byte[]{param};
    }

    public SingleSendProtocol(short command) {
        this.command = command;
        this.bytes = new byte[]{};
    }

    @Override
    public final byte[] sendContent() {
        return this.bytes;
    }

    @Override
    public final short sendCommand() {
        return command;
    }
}
