package com.zhouyou.audio.demo;

import android.app.Application;
import android.widget.Toast;

import com.zhouyou.audio.RxAudio;
import com.zhouyou.audio.demo.decoder.FSKDecoderData;
import com.zhouyou.audio.demo.encoder.EncoderData;
import com.zhouyou.audio.record.AudioRecorder;

/**
 * <p>描述：MyApplication</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/3/11 15:30<br>
 * 版本： v2.0<br>
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RxAudio.init(new RxAudio.AudioConfig()
                .isLog(true)//是否输出日志
                .receiveTimeOut(2000)//发送数据接收超时时间单位：ms
                .retryDelay(200)//间隔xxx ms重试
                .retryCount(2)//设置重试次数,默认重试1次
                .recorderConnectListener(connectListener)//录音启动监听
                .audioEncoder(new EncoderData())//设置编码实现
                .audioDecoder(new FSKDecoderData()));//设置解码实现;
    }

    private AudioRecorder.IConnectListener connectListener = new AudioRecorder.IConnectListener() {
        @Override
        public void onSuccess() {
            showMsg("录音启动成功");
        }

        @Override
        public void onFailure(Exception e, int code) {
            showMsg("录音启动失败");
        }
    };

    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
