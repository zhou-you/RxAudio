package com.zhouyou.audio.demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.zhouyou.audio.RxAudio;
import com.zhouyou.audio.base.ICallBack;
import com.zhouyou.audio.decoder.DecoderResult;
import com.zhouyou.audio.decoder.IDecoderCallBack;
import com.zhouyou.audio.demo.protocol.SingleSendProtocol;
import com.zhouyou.audio.demo.protocol.SkinTestingProtocol;
import com.zhouyou.audio.demo.receiver.HeadsetPlugReceiver;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.Tools;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RxAudio rxAudio;
    private TextView mTextView, mWaterOil;
    private HeadsetPlugReceiver mHeadsetPlugReceiver;
    private final StringBuilder mBuilder = new StringBuilder();
    //private PowerSin mPowerSin;
    @SuppressLint("StringFormatMatches")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.record).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.bind).setOnClickListener(this);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.btn4).setOnClickListener(this);
        findViewById(R.id.btn5).setOnClickListener(this);
        findViewById(R.id.btn6).setOnClickListener(this);
        findViewById(R.id.btn7).setOnClickListener(this);
        findViewById(R.id.btn8).setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
        mTextView = (TextView) findViewById(R.id.txt);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mWaterOil = (TextView) findViewById(R.id.wateroil);
        mWaterOil.setText(String.format(getString(R.string.water_msg), 0f, 0f));
        AudioLog.customTagPrefix = "AudioTest";
        AudioLog.allowI = true;
        rxAudio = RxAudio.getInstance();
        //mPowerSin = new PowerSin();
        //mPowerSin.start(22050);
        //rxAudio.startPower();
        //注册音频识别广播
        mHeadsetPlugReceiver = HeadsetPlugReceiver.getInstance(this);
        mHeadsetPlugReceiver.registerHeadSetReceiver();
        mHeadsetPlugReceiver.setHeadsetJackStateLisentener(new HeadsetPlugReceiver.HeadsetJackStateLisentener() {
            @Override
            public void haveDevice() {
                showMsg("识别到设备插入");
                rxAudio.setReceiveCallBack(new IDecoderCallBack() {
                    @Override
                    public void callResult(DecoderResult result) {
                        mHeadsetPlugReceiver.notifyResult(result);
                        if (result.code == DecoderResult.DECODE_OK) {
                            mBuilder.append((result.data.length <= 0 ? "" : Tools.byte2Hex(result.data))).append("\n");
                            mTextView.setText(mBuilder.toString());
                            response(result.data);
                        }
                    }
                });
            }

            @Override
            public void haveNoDevice() {
                showMsg("设备拔出");
            }

            @Override
            public void timeOut() {
                showMsg("监听超时，重新插拔耳机");
            }
        });

        rxAudio.setReceiveCallBack(new IDecoderCallBack() {
            @Override
            public void callResult(DecoderResult result) {
                mHeadsetPlugReceiver.notifyResult(result);
                if (result.code == DecoderResult.DECODE_OK) {
                    mBuilder.append((result.data.length <= 0 ? "" : Tools.byte2Hex(result.data))).append("\n");
                    mTextView.setText(mBuilder.toString());
                    response(result.data);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //rxAudio.resumeRecord();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //rxAudio.pasuseRecord();
    }

    @SuppressLint("StringFormatMatches")
    private void response(byte[] datas) {
        short cmmand=datas[1];
        if (cmmand == SkinTestingProtocol.Command.TESTING_START_RESP) {
            showMsg("开始测试响应");
        } else if (cmmand == SkinTestingProtocol.Command.TESTING_CONTACT) {
            showMsg("测试中");
            rxAudio.send(new SingleSendProtocol(SkinTestingProtocol.Command.TESTING_CONTACT_RESP).toBytes());
        } else if (cmmand == SkinTestingProtocol.Command.TESTING_END) {
            showMsg("测试完成");
            rxAudio.send(new SingleSendProtocol(SkinTestingProtocol.Command.TESTING_END_RESP).toBytes());
            byte[] data = datas;
            float water = Tools.calculateValue(data[4] & 0xff, data[5] & 0xff);
            float oil = Tools.calculateValue(data[6] & 0xff, data[7] & 0xff);
            mWaterOil.setText(String.format(getString(R.string.water_msg), water, oil));
        }
    }

    private void showMsg(String msg) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHeadsetPlugReceiver.unregisterHeadSetReceiver();
        rxAudio.exitAudio();
        //rxAudio.stopPower();
        //mPowerSin.stop();
    }

    private ICallBack mICallBack = new ICallBack() {
        @Override
        public void onSuccess(byte[] recData) {
//            response(recData, recCommand);
        }

        @Override
        public void onFailure(Throwable throwable) {
            showMsg("测试失败");
        }
    };

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.record) {
            rxAudio.startRecord();
        } else if (view.getId() == R.id.stop) {
            rxAudio.stopRecord();
        } else if(view.getId() == R.id.clear){
            mBuilder.setLength(0);
            mTextView.setText("");
        }else if (view.getId() == R.id.bind) {
            if(!HeadsetPlugReceiver.getInstance(this).isLinked()){
                Toast.makeText(this,"请先插入耳机孔设备！",Toast.LENGTH_SHORT).show();
                return;
            }
            rxAudio.send(new SingleSendProtocol(SkinTestingProtocol.Command.BINDING).toBytes(), mICallBack);
        } else {
            if(!HeadsetPlugReceiver.getInstance(this).isLinked()){
                Toast.makeText(this,"请先插入耳机孔设备！",Toast.LENGTH_SHORT).show();
                return;
            }
            int param = 1;
            switch (view.getId()) {
                case R.id.btn1:
                    param = 1;
                    break;
                case R.id.btn2:
                    param = 2;
                    break;
                case R.id.btn3:
                    param = 3;
                    break;
                case R.id.btn4:
                    param = 4;
                    break;
                case R.id.btn5:
                    param = 5;
                    break;
                case R.id.btn6:
                    param = 6;
                    break;
                case R.id.btn7:
                    param = 7;
                    break;
                case R.id.btn8:
                    param = 8;
                    break;
            }
            rxAudio.send(new SingleSendProtocol(SkinTestingProtocol.Command.TESTING_START, (byte) param).toBytes(), mICallBack);
        }
    }
}
