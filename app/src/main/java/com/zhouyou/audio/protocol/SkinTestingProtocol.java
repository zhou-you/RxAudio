package com.zhouyou.audio.protocol;

import static com.zhouyou.audio.protocol.SkinTestingProtocol.ProtocolInfo.AUDIO_HEAD_LENGTH;


/**
 * <p>描述：测肤仪协议</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/15 14:54<br>
 * 版本： v2.0<br>
 */
public class SkinTestingProtocol {
    /**
     * 协议常量
     */
    public static final class ProtocolInfo {
        /*协议版本标识*/
        public static final byte AUDIO_PROCOTOL_VERSION = (byte) 0x10;

        /* 包头长度 */
        public static final int AUDIO_HEAD_LENGTH = 4;

        /*CRC校验位长度*/
        public static final int PROCOTOL_CRC_LENGTH = 1;
    }

    /**
     * 协议命令
     */
    public static class Command {
        /* 识别设备 */
        public static final short SIGN = (short) 0x10;
        /* 识别设备响应 */
        public static final short SIGN_RESP = (short) 0x11;
        
        /* 绑定 */
        public static final short BINDING = (short) 0x20;
        /* 绑定响应 */
        public static final short BINDING_RESP = (short) 0x21;

        /* 测试开始 */
        public static final short TESTING_START = (short) 0x30;
        /* 测试开始响应 */
        public static final short TESTING_START_RESP = (short) 0x31;
        
        /* 接触皮肤 */
        public static final short TESTING_CONTACT = (short) 0x40;
        /* 接触皮肤响应 */
        public static final short TESTING_CONTACT_RESP = (short) 0x41;

        /* 测试结束 */
        public static final short TESTING_END = (short) 0x50;
        /* 测试结束响应 */
        public static final short TESTING_END_RESP = (short) 0x51;
    }

    /**
     * 协议包头
     */
    public static class AudioProtocolHead {
        //最新的协议起始头5A可以不要的，但是oppo和荣耀6手机发送第一个字节时衰减由小到大，硬件解不出来。因此前面多放一个字节
        public byte pStart = ProtocolInfo.AUDIO_PROCOTOL_VERSION;
        public byte pVersion = ProtocolInfo.AUDIO_PROCOTOL_VERSION;
        public short pCommand = 0;
        public byte pBodyLen = 0;

        public AudioProtocolHead(short pCommand, byte pBodyLen) {
            this.pCommand = pCommand;
            this.pBodyLen = pBodyLen;
        }

        public AudioProtocolHead(final byte[] head_byte) {
            pStart = head_byte[0];
            pVersion = head_byte[1];
            pCommand = head_byte[2];
            pBodyLen = head_byte[3];
        }

        // 转字节数组
        public byte[] toBytes() throws Exception {
            byte[] bytes = new byte[AUDIO_HEAD_LENGTH];
            bytes[0] = pStart;
            bytes[1] = pVersion;
            bytes[2] = (byte) pCommand;
            bytes[3] = pBodyLen;
            return bytes;
        }

    }
}
