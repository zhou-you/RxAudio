# RxAudio
基于Rxjava实现的android音频库,主要用于手机和音频设备之间通信，支持录音、发送、供电、发送失败自动重试（可以指定重试次数），设置接收超时、自定义编解码，自定义配置参数等功能，使用本库只需要关注与业务相关的自定义编解码。

## 版本说明

#### 当前版本

- V1.0.2 增加了耳机孔插入监听

#### 历史版本
- V1.0.1 初始版本

##  用法介绍

## build.gradle设置

```
dependencies {
 compile 'com.zhouyou:rxaudio:1.0.2'
}
```



## 全局初始化

全局初始化，在Application的onCreate()放在中初始化，或者是在你使用音频前进行初始化。
 ```
RxAudio.init(new RxAudio.AudioConfig()
                .isLog(true)//是否输出日志
                .receiveTimeOut(2000)//发送数据接收超时时间单位：ms
                .retryDelay(200)//间隔xxx ms重试
                .retryCount(2)//设置重试次数,默认重试1次
                .recorderConnectListener(connectListener)//录音启动监听
                .audioEncoder(new CustomEncoderData())//设置编码实现,需要自己定义与业务相关的实现
                .audioDecoder(new CustomDecoderData()));//设置解码实现,需要自己定义与业务相关的实现
```



*注：*

*1.参数也可以不设置，但是audioEncoder()和audioDecoder()是必须设置的，不设置就不能编解码，也就无意义，也可以不使用全局设置，用Rxaudio的setAbstractDecoder和setAudioEncoder方法里设置编解码*

*2.每个业务都不一样需要自定义编解码实现，耳机孔设备就采用库里已经提供好的new EncoderData()和new FSKDecoderData()*


## 设备识别

在需要监听设备插入的地方注册监听，如果是Activity就在onCreate(Bundle savedInstanceState) 中注册。
```
//注册音频识别广播
HeadsetPlugReceiver mHeadsetPlugReceiver = HeadsetPlugReceiver.getInstance(this);
mHeadsetPlugReceiver.registerHeadSetReceiver();//注册广播
mHeadsetPlugReceiver.setHeadsetJackStateLisentener(new HeadsetPlugReceiver.HeadsetJackStateLisentener() {
    @Override
    public void haveDevice() {
        //识别到设备插入
    }

    @Override
    public void haveNoDevice() {
        //设备拔出
    }
});

```
在不使用的时候要取消注册，如果是Activity就在onDestroy() 中取消注册。

```
mHeadsetPlugReceiver.unregisterHeadSetReceiver();
```
*注：*
*1.设备识别插入耳机孔设备录音功能调用RxAudio相关功能，设备拔出时停掉录音。*
*2.如果想要能够具备识别功能，是普通耳机（听歌）还是你的耳机孔设备，最好是插入后给3s通信，收到消息就是耳机孔设备，否则超时了就是普通耳机*

## 接收数据

创建RxAudio对象，设置接收回调监听

```
private RxAudio rxAudio;
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    rxAudio = RxAudio.getInstance();
    rxAudio.setReceiveCallBack(new IDecoderCallBack() {
        @Override
        public void callResult(DecoderResult result) {
            if (result.code == DecoderResult.DECODE_OK) {
                //解码成功
            }
        }
    });
}
```

*注：*
*1.接收的数据都在DecoderResult中，从DecoderResult里拿设备发送的数据。*
*2.setReceiveCallBack接收监听是单列模式，只能设置一次，设置第二次时会把第一次的监听覆盖掉，多个地方都需要接收的话，可以采用接收后发送event事件自己处理*
*3.在设置setReceiveCallBack()监听的页面，退出时需要调用mHeadsetPlugReceiver.resume()恢复设备识别监听。*

## 发送数据
发送数据给设备，不需要监听是否接收到数据
```
 rxAudio.send(byte[] bytes);
```
发送数据给设备，设置监听回调
```
rxAudio.send(bytes, new ICallBack() {
    @Override
    public void onSuccess(byte[] recData) {
    }
    @Override
    public void onFailure(Throwable throwable) {
    }
});
```
*注：发送回调监听，在setReceiveCallBack全局监听中也可以收到消息。*
## 音频退出
在应用退出时调用
```
rxAudio.exitAudio();
```
## 编解码使用方式
全局设置
```
RxAudio.init(new RxAudio.AudioConfig()
                .audioEncoder(new EncoderData())//设置编码实现
                .audioDecoder(new FSKDecoderData()));//设置解码实现;
```
通过RxAudio对象设置
```
RxAudio rxAudio = RxAudio.getInstance();
rxAudio.setAbstractDecoder(new FSKDecoderData());//设置解码
rxAudio.setAudioEncoder(new EncoderData());//设置解码
```

## 自定义解码

本库中默认采用FSKDecoderData解码，如果需要自定义实现解码需要继承AbstractDecoder类



```

public class CustomDecoder extends AbstractDecoder {
    @Override
    public void start() {
        //1.解码类启动，如果解码耗时需要开线程，那么在此处调用线程就行了，不需要你自己手动启动，框架会帮你启动
        //2.如果启动不需要操作则可以空实现
    }
    @Override
    public void stop() {
        //解码类停止，如果有线程，在这里停止，不需要外面手动停止，录音停止时会帮你停止你的线程操作
        //2.如果关闭没有其它操作，可以空实现
    }

    @Override
    public boolean isRuning() {//自己实现解码是否在运行，如果自己开了线程，可以返回当前线程的转态，外面调用isRuning()
        return false;
    }

    @Override
    public void startDecoder(int sampleRate) {
        //真正的开始解码了，sampleRate是录音时你设置的采样率，如果不使用不用关心
        //解码前的初始化操作
    }
    @Override
    public void decoderData(short[] audio_data, int bufferReadResult) throws Exception {
        //decoderData已经是在线程中执行了，不需要再开线程
        //audio_data录音的原始信号，用于解码,每次从系统读取的buffer
        //bufferReadResult读取的实际长度，例如你开了1024的缓冲区，实际系统就给你了480。
    }

    @Override

    public void finishDecoder() {
        //解码结束
    }

    @Override
    public void recoderState(boolean isPause) {
        //是否处于暂停录音
    }
}

```

使用方式参考上面编解码使用方式，例如：

```
RxAudio rxAudio = RxAudio.getInstance();
rxAudio.setAbstractDecoder(new CustomDecoder());//设置解码
```

## 自定义编码

本库中默认采用EncoderData编码后发送给设备，如果需要自定义实现解码需要实现IAudioEncoder接口
```
public class CustomEncoder implements RxAudioPlayer.IAudioEncoder{
    @Override
    public void startEncoder(int sampleRate) {
        //开始编码，sampleRate编码的采样率
    }

    @Override
    public byte[] encoderData(byte[] bytes) {
        //在这里封装你给设备发送的所有数据，按照协议发送
        //把要发给设备的字节bytes重新组装，编码成设备可以识别的正玄波的byte[]
        return new byte[0];
    }

    @Override
    public void finishEncoder() {
        //结束编码
    }
}
```

使用方式参考上面编解码使用方式，例如：

```
RxAudio rxAudio = RxAudio.getInstance();
rxAudio.setAudioEncoder(new CustomEncoder());//设置编码
```
## 其它场景使用
以上所有实现都是主要围绕RxAudio来说明，RxAudio是集合了供电、录音、发送三大模块实现，每个模块也可独立使用。
供电PowerSupply类
```
PowerSupply mSuply = new PowerSupply.Builder()
.sampleRate(44100)
.channelConfig(AudioFormat.CHANNEL_CONFIGURATION_MONO)
.audioFormat(AudioFormat.ENCODING_PCM_16BIT)
.channel(PowerSupply.Channel.RIGHT)
.powerRate(21500)//设置输出正玄波频率
.build()

mSuply.start()
mSuply.stop()
```
录音AudioRecorder类
```
AudioRecorder recorder = new AudioRecorder.Builder()
.reate(8000)
.audioEncoding(AudioFormat.ENCODING_PCM_16BIT)
.audioSource(MediaRecorder.AudioSource.MIC)
.channelConfiguration(AudioFormat.CHANNEL_IN_MONO)
.build();

recorder.start()
recorder.stop()
```
发送用RxAudioPlayer类
```
RxAudioPlayer audioPlayer = new RxAudioPlayer.Builder()
.streamType(AudioManager.STREAM_MUSIC)
.rate(8000)
.channelConfig(AudioFormat.CHANNEL_OUT_MONO)
.audioFormat(AudioFormat.ENCODING_PCM_16BIT)
.channel(Channel.LEFT)//左声道通信
.build();

audioPlayer.rxSend((byte) 0x5A)//发送单个数据
audioPlayer.rxSend(new byte[])//发送数组
audioPlayer.rxSend((byte) 0x5A, 2, 100)//发送单个数据，失败重试2次，每次间隔100ms
audioPlayer.rxSend(new byte[], 2, 100)//发送数组，失败重试2次，每次间隔100ms
```

## RxAudio讲解

RxAudio是本库的一个音频入口类具有供电、接收、发送三大能。

#### 使用方式

RxAudio rxAudio = RxAudio.getInstance();

#### 功能介绍

#### 初始化配置
```
RxAudio.init(AudioConfig config);
```
#### 开启录音
```
rxAudio.startRecord();
```
#### 关闭录音

```
rxAudio.stopRecord();
```

#### 开启供电
```
rxAudio.startPower();
```

#### 关闭供电

```
rxAudio.stopPower();
```

#### 设置编码

```
rxAudio.setAudioEncoder(RxAudioPlayer.IAudioEncoder audioEncoder);
```

#### 设置解码

```
rxAudio.setAbstractDecoder(AbstractDecoder abstractDecoder);
```

#### 设置监听回调

```
rxAudio.setReceiveCallBack(IDecoderCallBack callBack);
```

#### 设置供电频率

```
rxAudio.setPowerRate(int sin_rate);//默认21KHZ,不用设置
```

#### 发送命令给设备，不需要响应

```
rxAudio.send(final byte[] datas) ;
```
#### 发送命令给设备，并且等待设备回应接收数据

```
rxAudio.send(final byte[] datas, final ICallBack callBack)
```

#### 取消发送

```
rxAudio.cancleSend() ;
```

#### 退出音频

```
rxAudio.exitAudio();
```

#### 自定义供电配置

```
PowerSupply.Builder mPowerBuilder = new PowerSupply.Builder().sampleRate(48000)
                .channelConfig(AudioFormat.CHANNEL_OUT_MONO)
                .audioFormat(AudioFormat.ENCODING_PCM_16BIT)
                .channel(Channel.RIGHT)
                .powerRate(20050);//设置输出21KHZ正玄波频率 VIVO手机不能设置成21050否则驱动不了电池
RxAudio.init(new RxAudio.AudioConfig().powerBuilder(mPowerBuilder));
```

#### 自定义录音配置

```
AudioRecorder.Builder mRecorderBuilder = new AudioRecorder.Builder().reate(44100)
                .audioEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .audioSource(MediaRecorder.AudioSource.MIC)
                .channelConfiguration(AudioFormat.CHANNEL_CONFIGURATION_MONO);
RxAudio.init(new RxAudio.AudioConfig().recordBuilder(mRecorderBuilder));
```
#### 自定义发送配置

```
RxAudioPlayer.Builder mAudioPlayerBuilder = new RxAudioPlayer.Builder().streamType(AudioManager.STREAM_MUSIC)
                .rate(8000)
                .channelConfig(AudioFormat.CHANNEL_OUT_MONO)
                .audioFormat(AudioFormat.ENCODING_PCM_16BIT)
                .channel(Channel.LEFT)//左声道通信
                .mode(AudioTrack.MODE_STATIC);
RxAudio.init(new RxAudio.AudioConfig().trackBuilder(mAudioPlayerBuilder));
```

#### RxAudio默认配置参数介绍：

```
//默认录音配置
mRecorderBuilder = new AudioRecorder.Builder().reate(44100)//设置采样率
        .audioEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .audioSource(MediaRecorder.AudioSource.MIC)
        .channelConfiguration(AudioFormat.CHANNEL_CONFIGURATION_MONO);

//默认供电配置
mPowerBuilder = new PowerSupply.Builder().sampleRate(48000)//设置采样率
        .channelConfig(AudioFormat.CHANNEL_OUT_MONO)//设置单声道
        .audioFormat(AudioFormat.ENCODING_PCM_16BIT)
        .channel(Channel.RIGHT)//左声道供电
        .powerRate(20050);//设置输出21KHZ正玄波频率 VIVO手机不能设置成21050否则驱动不了电池
//默认发送配置
mAudioPlayerBuilder = new RxAudioPlayer.Builder().streamType(AudioManager.STREAM_MUSIC)//设置类型
        .rate(8000)//设置采样率
        .channelConfig(AudioFormat.CHANNEL_OUT_MONO)//设置单声道
        .audioFormat(AudioFormat.ENCODING_PCM_16BIT)
        .channel(Channel.LEFT)//左声道通信
        .mode(AudioTrack.MODE_STATIC);
```
注：这些参数不需要关系，RxAudio默认已经设置了，如果需要自定义可以设置Builder.

## Demo效果预览
![](http://img.blog.csdn.net/20170407170949970)
## 项目应用效果预览
![](http://img.blog.csdn.net/20170407172312631)

## 重点说明
- 本工程的Demo编解码都是空实现，因为编解码在其它app上已经商用，涉及到具体的业务通信安全，不对外开放，望理解！！！采用FSK，傅里叶FFT解码，能够兼容市场上主流手机80%以上，另外告知大家曼彻斯特的解码是行不通的（只有ios可以），勿走弯路，FFT才是正道（andoid和ios同时兼容），FFT同样也需要做兼容处理。
- 供电说明，供电并不是真正的供电，音频设备如果想真正的供电最少需要在2.5V以上才能够稳定，ios可以输出3.5V的电压，anroid输出最好的也才在1.9V左右，很多手机只有1v左右，根本达不到供电的标注。很多希望供电是因为，如果耳机孔输出能够供电，那么耳机孔设备就不用加电池会减少很多成本。但是实际测试在android上不行的，本库中供电是当耳机孔插入时开始发送人耳听不到的21KHZ正玄波用最大振幅输出，以此来打开耳机孔设备电池的使能，耳机孔开始工作，拔出时断开使能不工作，增加耳机孔电池使用寿命。


## 其它
如果想深入了解基于FFT的解码以及兼容、注意事项、供电、发送等原理，后期会在CSDN上发表博客，敬请期待！！

## 联系方式
邮箱地址： 478319399@qq.com
QQ群： 581235049（建议使用QQ群，邮箱使用较少，可能看的不及时）
本群旨在为使用我的[github](https://github.com/zhou-you)项目的人提供方便，如果遇到问题欢迎在群里提问。一个人的能力也有限，希望一起学习一起进步。
关注我的[github](https://github.com/zhou-you)，了解我的最新项目。关注我的[博客](http://blog.csdn.net/zhouy478319399)，阅读我的最新文章。

#####欢迎加入QQ交流群

![](http://img.blog.csdn.net/20170601165330238?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvemhvdXk0NzgzMTkzOTk=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)



