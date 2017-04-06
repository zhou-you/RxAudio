package com.zhouyou.audio.protocol;

import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.CRCUtil;
import com.zhouyou.audio.util.Tools;

import java.nio.ByteBuffer;

import static com.zhouyou.audio.protocol.SkinTestingProtocol.ProtocolInfo.AUDIO_HEAD_LENGTH;
import static com.zhouyou.audio.protocol.SkinTestingProtocol.ProtocolInfo.PROCOTOL_CRC_LENGTH;

/**
 * <p>描述：发送命令的基类</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/16 14:26<br>
 * 版本： v2.0<br>
 */
public abstract class BaseSendProtocol {

    public final byte[] toBytes() {
        final byte[] sendBtyes = sendContent();
        final short command = sendCommand();
        int contentLen = 0;
        if (sendBtyes != null && sendBtyes.length > 0) contentLen = sendBtyes.length;
        //AudioLog.i("command:" + command);
        final int allLength = AUDIO_HEAD_LENGTH + PROCOTOL_CRC_LENGTH + contentLen;
        final ByteBuffer allSendBuff = ByteBuffer.allocate(allLength * 2);
        //写入头信息
        try {
            allSendBuff.put(new SkinTestingProtocol.AudioProtocolHead(command, (byte) contentLen).toBytes());
        } catch (Exception e) {
            AudioLog.i("==" + e.getMessage());
        }
        //写入内容
        if (sendBtyes != null && sendBtyes.length > 0) {
            allSendBuff.put(sendBtyes);
            AudioLog.i("content bytes:" + Tools.byte2Hex(sendBtyes));
        }
        allSendBuff.flip();
        int len = allSendBuff.limit();
        byte[] sourceBytes = new byte[len];
        allSendBuff.get(sourceBytes);
        byte[] crcBytes = new byte[len - 1];//第一个不参与crc校验
        System.arraycopy(sourceBytes, 1, crcBytes, 0, crcBytes.length);
        // 写入crc检验码
        byte crc = CRCUtil.calcCrc8(crcBytes);
        allSendBuff.compact();
        allSendBuff.position(len);
        allSendBuff.put(crc);
        allSendBuff.flip();
        //读取完整的数据内容
        byte[] sourceAllBytes = new byte[allLength];
        allSendBuff.rewind();
        allSendBuff.get(sourceAllBytes);
        AudioLog.i("send all bytes:" + Tools.byte2Hex(sourceAllBytes));
        return sourceAllBytes;
    }

    /**
     * 发送内容
     *
     * @return
     */
    public abstract byte[] sendContent();

    /**
     * 发送命令
     *
     * @return
     */
    public abstract short sendCommand();

}
