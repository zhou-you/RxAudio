package com.het.audio;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;
import android.util.Log;

import com.zhouyou.audio.exception.RetryWhenProcess;
import com.zhouyou.audio.util.AudioLog;
import com.zhouyou.audio.util.CRCUtil;
import com.zhouyou.audio.util.Tools;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import static android.content.Context.ACTIVITY_SERVICE;
import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.het.audio", appContext.getPackageName());
    }

    @Test
    public void testRx3() {
        String names[] = {"张三", "李四", "王五", "赵六", "麻七"};
        Observable.from(names)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String name) {
                        AudioLog.i(name);
                    }
                });
    }

    @Test
    public void testRx34() {
        byte[] bytes = {0x10, 0x20, 0x00};
        byte result = CRCUtil.calcCrc8(bytes);
        AudioLog.i("=============="+Tools.byte2Hex(result));
    }

    @Test
    public void testRx4() {
        Observable
                .timer(2, TimeUnit.MILLISECONDS)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        AudioLog.i("===" + aLong.intValue());
                    }
                });
    }

    @Test
    public void testRx32() {
        final LinkedBlockingQueue<String> blockingQueue = new LinkedBlockingQueue();
        try {
            blockingQueue.put(0 + "qqqqqqqq");
            blockingQueue.put(1 + "qqqqqqqq");
           /* blockingQueue.put(2 + "qqqqqqqq");
            blockingQueue.put(3 + "qqqqqqqq");
            blockingQueue.put(4 + "qqqqqqqq");
            blockingQueue.put(5 + "qqqqqqqq");
            blockingQueue.put(6 + "qqqqqqqq");
            blockingQueue.put(7 + "qqqqqqqq");*/

            String take1 = blockingQueue.take();
            AudioLog.i("take:" + take1);
            String take2 = blockingQueue.take();
            AudioLog.i("take:" + take2);
            String take3 = blockingQueue.take();
            AudioLog.i("take:" + take3);
            String take4 = blockingQueue.take();
            AudioLog.i("take:" + take4);
        }catch (Exception e){
            e.printStackTrace();
        }
        
        
        final LinkedBlockingQueue<String> blockingQueue2 = new LinkedBlockingQueue();
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 1000;
                for (int i1 = 0; i1 < i; i1++) {
                    try {
                        blockingQueue2.put(i1 + "qqqqqqqq");
                        //Thread.sleep(new Random().nextInt(4)*1000);
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String value = blockingQueue2.take();
                        AudioLog.i("value:" + value);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Test
    public void testRx20() {
        LinkedList<Short> mShorts = new LinkedList<Short>();
        mShorts.add((short) 1);
        mShorts.add((short) 0);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        AudioLog.i("===" + mShorts.toString());
        String value = "10101011010101";
        AudioLog.i("===" +value.substring(2,3));
    }
    @Test
    public void testRx21() {
        /*LinkedList<Short> mShorts = new LinkedList<>();
        mShorts.add((short) 1);
        mShorts.add((short) 0);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        mShorts.add((short) 1);
        AudioLog.i("===" + mShorts.toString());
        String value = "10101011010101";
        AudioLog.i("===" +value.substring(2,3));*/
        String value="101001011010001";
        long time = System.currentTimeMillis();
        decoderdCache(value);
        AudioLog.i("FFTDecoder-->"+(System.currentTimeMillis() - time));
       // print(">>>>>>>>>>>>>>" + (System.currentTimeMillis() - time));
        LinkedList<String> mQueueCache = new LinkedList<String>();
        mQueueCache.add("a");
        mQueueCache.add("b");
        mQueueCache.add("c");
        mQueueCache.add("d");
        mQueueCache.add("e");
        mQueueCache.add("f");
        mQueueCache.push("1");
        mQueueCache.push("2");
        AudioLog.i("-->"+mQueueCache.toString());
        String abc = mQueueCache.peek();
        AudioLog.i("-->"+abc);
        AudioLog.i("-->"+mQueueCache.toString());
        
    }
    private static final int HIGH_FREQ = 2000;
    private static final int LOW_FREQ = 1000;
    private static final String START_CODE = "1010";//起始码
    private static final String END_CODE = "01";//结束码
    private static final int bit = 8;//数据长度8位
    private static final int validate = 1;//校验码1位
    private int all_len = 15;//硬件发送数据最长是15

    private void print(String msg) {
        AudioLog.i("FFTDecoder-->" + msg);
    }

    @Test
    public void testRx5() {
        Observable.just(get())
                //.cast(Integer.class)
                .retryWhen(new RetryWhenProcess(2, 30))
                .onErrorResumeNext(funcException)
                .subscribe(new Subscriber() {
                    @Override
                    public void onCompleted() {
                        AudioLog.i("#######=onCompleted==");
                    }

                    @Override
                    public void onError(Throwable e) {
                        AudioLog.i("#######=onError==" + e.getMessage());
                    }

                    @Override
                    public void onNext(Object o) {
                        AudioLog.i("#######=onNext==" + o.toString());
                    }
                });
    }

    private boolean get() {
        Integer.parseInt("ewrer");
        return Boolean.valueOf("true2");
    }

    @Test
    public void testRx10() {
        Observable.just(true)
                .cast(Integer.class).onErrorResumeNext(funcException)
                .retryWhen(new RetryWhenProcess(2, 30)).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                AudioLog.i("#######==call=" + integer);
            }
        });
    }

    @Test
    public void testRx11() {
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                //Integer.parseInt("ewrer");
                throw new RuntimeException("123");
            }
        }).onErrorResumeNext(funcException).doOnSubscribe(new Action0() {
            @Override
            public void call() {
                AudioLog.i("#######==doOnSubscribe=");
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                AudioLog.i("#######==doOnCompleted=");
            }
        }).doAfterTerminate(new Action0() {
            @Override
            public void call() {
                AudioLog.i("#######==doAfterTerminate=");
            }
        })
                .retryWhen(new RetryWhenProcess(3, 30)).subscribe(new Subscriber() {
            @Override
            public void onCompleted() {
                AudioLog.i("#######==onCompleted=");
            }

            @Override
            public void onError(Throwable e) {
                AudioLog.i("#######==onError=" + e.getMessage());
            }

            @Override
            public void onNext(Object o) {
                AudioLog.i("#######==onError=" + o.toString());
            }
        });
    }

    /**
     * 异常处理
     */
    private Func1 funcException = new Func1<Throwable, Observable>() {
        @Override
        public Observable call(Throwable throwable) {
            AudioLog.i("#######==funcException=" + throwable.getMessage());
            return Observable.error(throwable);
        }
    };

    @Test
    public void testRx6() {
        Observable.range(1, 10).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                AudioLog.i("==integer=" + integer);
            }
        });
    }

    @Test
    public void testRx8() {
        Observable.range(1, 10).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                AudioLog.i("==integer=" + integer);
            }
        });
    }

    @Test
    public void testRx12() {
        ByteBuffer mAllByteBuff = ByteBuffer.allocate(100);
        if (mAllByteBuff.limit() != mAllByteBuff.capacity()) {//说明是读模式，从读切换到写。
            mAllByteBuff.flip();
        }
        if (mAllByteBuff.position() >= mAllByteBuff.capacity()) {//说明写满了，防止溢出
            mAllByteBuff.clear();//清掉数据
        }
        mAllByteBuff.put((byte) 1);
    }

    @Test
    public void testRx13() {
        //short[] data = {1,0,1,0,1,1,0,0,1,0,0,0};
        short[] data = {1,0,1,0,0,0,1,0,0,0,0,1};//21
        //short[] data = {1,0,1,0,0,1,0,1,1,0,1,0};//5a
        //short[] data = {1,0,1,0,1,1,0,0,1,0,0,0};//c8
        byte dataRxByte = 0;//数据位计算
        for (int i = 0; i < data.length; i++) {
            if (i >= 4) {//数据位开始累加
                dataRxByte |= (data[i] << (data.length - i - 1));
            }
        }
        //AudioLog.i("#######==CRC=" + StringUtil.toHexString(dataRxByte));
    }
    @Test
    public void testRx15() {
        //short[] data = {1,0,1,0,1,1,0,0,1,0,0,0};
        //填写数据位
        byte[] audioBit = new byte[8];
        byte audioData= 90;
        for (int counter_i = 0; counter_i < 8; counter_i++) {
            if (((audioData >> (8-counter_i -1) & 0x01) == 0x01))
                audioBit[counter_i] = 1;
            else
                audioBit[counter_i] = 0;
        }
        AudioLog.i("#######==CRC=" + Arrays.toString(audioBit));
    }

    @Test
    public void testRx14() {
        byte[] sourceData = {90, 16, 33, 9, 1, 0, 0, 0, 1, 0, 31, 4, 1};
        byte[] crcRes = Tools.shortToByte(Tools.encryptCRC16(sourceData, sourceData.length));
        //String socrc = StringUtil.toHexString(crcSource);
        String scrc = Tools.byte2Hex(crcRes);
        //print("make sourceData CRC:" + scrc);
        AudioLog.i("#######==CRC=" + scrc);
    }

    @Test
    public void testRx9() {
        FutureTask<Integer> f = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Thread.sleep(1000);
                return 21;
            }
        });
        new Thread(f).start();

        //Observable<Integer> values = Observable.from(f);
        Observable<Integer> values = Observable.from(f, 2000, TimeUnit.MILLISECONDS);
        values.doOnTerminate(new Action0() {
            @Override
            public void call() {
                AudioLog.i("#######==doOnTerminate=");
            }
        }).doOnEach(new Action1<Notification<? super Integer>>() {
            @Override
            public void call(Notification<? super Integer> notification) {
                AudioLog.i("#######==doOnEach="+notification.toString());
            }
        }).subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                AudioLog.i("#######==onCompleted=");
            }

            @Override
            public void onError(Throwable e) {
                AudioLog.i("#######==onError=" + e.getMessage());
            }

            @Override
            public void onNext(Integer integer) {
                AudioLog.i("#######==onNext=" + integer);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AudioLog.i("#######==onNext===2222======");
            }
        });
    }

    @Test
    public void testRx7() {
        Observable.just(1, 2, 3).repeatWhen(new Func1<Observable<? extends Void>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Void> observable) {
                //重复3次
                return observable.zipWith(Observable.range(1, 3), new Func2<Void, Integer, Integer>() {
                    @Override
                    public Integer call(Void aVoid, Integer integer) {
                        return integer;
                    }
                }).flatMap(new Func1<Integer, Observable<?>>() {
                    @Override
                    public Observable<?> call(Integer integer) {
                        System.out.println("#######delay repeat the " + integer + " count");
                        //1秒钟重复一次
                        return Observable.timer(30, TimeUnit.MILLISECONDS);
                    }
                });
            }
        }).subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                System.out.println("#######Sequence complete.");
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("#######Error: " + e.getMessage());
            }

            @Override
            public void onNext(Integer value) {
                System.out.println("#######Next:" + value);
            }
        });
    }

    @Test
    public void testRx23() {
        decoderdCache("1010000000100101");
    }

    int i = 4;

    @Test
    public void testRx25() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 1; i < 4; i++) {
                    try {
                        Thread.sleep(100 * i);
                    } catch (InterruptedException e) {
                        subscriber.onError(e);
                    }
                    subscriber.onNext(i);
                }
                subscriber.onCompleted();
            }
        })
                .timeout(250, TimeUnit.MILLISECONDS).doOnTerminate(new Action0() {
            @Override
            public void call() {
                AudioLog.i("doOnTerminate.");
            }
        })
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        AudioLog.i("onCompleted.");
                    }

                    @Override
                    public void onError(Throwable e) {
                        AudioLog.i("onError: " + e.getMessage());
                    }

                    @Override
                    public void onNext(Integer integer) {
                        AudioLog.i("onNext: " + integer);
                    }
                });
    }

    public void testRx24() {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                AudioLog.i("===11111111=======");
                try {
                    i--;
                    //Thread.sleep(new Random().nextInt(3)*1000*2);
                    /*while (true){
                        Thread.sleep(4*1000);
                    }*/
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onNext("123456");
                subscriber.onCompleted();
                AudioLog.i("===2222222=======");

            }
        }).timeout(2000, TimeUnit.MILLISECONDS).retryWhen(new RetryWhenProcess()).doOnTerminate(new Action0() {
            @Override
            public void call() {
                AudioLog.i("===>>>=======");
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                AudioLog.i("==doOnCompleted=======");
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                AudioLog.i("=====onCompleted======");
            }

            @Override
            public void onError(Throwable e) {
                AudioLog.i("===========" + e.getMessage());
            }

            @Override
            public void onNext(String s) {
                AudioLog.i("===========" + s);
            }
        });
    }

    private void decoderdCache(String vaule) {
        print("vaule:" + vaule);
        int startindex = vaule.indexOf(START_CODE);//起始码位置索引
        print("startindex:" + startindex);
        if (startindex > -1 && (vaule.length() - startindex) >= all_len) {//找到了起始位置
            print("validate START_CODE success!!");
            final int validateIndex = startindex + START_CODE.length() + bit;//查找校验位置
            final String validateBit = vaule.substring(validateIndex, validateIndex + validate);//获取校验值
            print("validateBit：" + validateBit);
            final String data = vaule.substring(startindex, startindex + START_CODE.length() + bit);//获取需要校验的数据
            print("data Bit：" + data);
            short parityBit = 0;//求出校验的数据位和
            byte dataRxByte = 0;//数据位计算
            for (int i = 0; i < data.length(); i++) {
                short shortStr = Short.valueOf(data.substring(i, i + 1));
                parityBit += shortStr;////数据位开始累加(12) 起始码（4）+数据码（8）
                if (i >= START_CODE.length()) {//数据位开始累加(8)
                    dataRxByte |= (shortStr << (data.length() - i - 1));
                }
            }
            print("old parityBit：" + parityBit);
            print("new parityBit：" + (parityBit & 0x01));
            if (Short.valueOf(validateBit) == (parityBit & 0x01)) {//校验成功
                print("validate DATA success!!");
                int endIndex = startindex + START_CODE.length() + bit + validate;
                String endStr = vaule.substring(endIndex, endIndex + END_CODE.length());
                if (endStr.equals(END_CODE)) {//找到了结束位置，发送解码的数据
                    print("validate END_CODE success!!");
                    print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@" + Tools.byte2Hex(dataRxByte));
                    // decoderResult(dataRxByte);
                } else {
                    print("validate END_CODE fail!!");
                }
            } else {
                print("validate DATA fail!!");
            }
        } else {
            print("validate START_CODE fail!!" + startindex);
        }
    }

    private boolean isRunningForeground (Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if(!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(context.getPackageName())) {
            return true ;
        }
        return false ;
    }

    /**
     * 获取activity 
     */
    protected static void getTastInfos(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.RunningTaskInfo info = manager.getRunningTasks(1).get(0);
        String shortClassName = info.topActivity.getShortClassName();    //类名  
        String className = info.topActivity.getClassName();              //完整类名  
        String packageName = info.topActivity.getPackageName();          //包名  
        Log.e("test","YYSDK className:"+className);
    }

   public void testdoOn(){
       Observable.create(new Observable.OnSubscribe<Integer>() {
           @Override
           public void call(Subscriber<? super Integer> subscriber) {
               subscriber.onNext(1234);
           }
       })/*.compose(RxSchedulers.<Integer>_io_main())*//*.doOnTerminate(new Action0() {
           @Override
           public void call() {
               print("++++++doOnTerminate+++++++");
           }
       })*/.subscribe(new Subscriber<Integer>() {
           @Override
           public void onCompleted() {
               print("++++++onCompleted+++++++");
           }

           @Override
           public void onError(Throwable e) {
               print("++++++onError+++++++");
           }

           @Override
           public void onNext(Integer integer) {
               print("++++++onNext+++++++"+integer);
           }
       });
   }
}
