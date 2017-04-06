package com.het.audio;

import org.junit.Test;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    int all_len = 15;
    int freq = 2500;
    ShortBuffer mShortBuf = ShortBuffer.allocate(all_len * 2);

    @Test
    public void parseData() throws Exception {
        List<Integer> list = new ArrayList<Integer>();
        list.add(37);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);
        list.add(2500);

        /*list.add(2500);//1
        list.add(1250);//0
        list.add(2500);//1
        list.add(1250);//0
        list.add(1250);//0
        list.add(2500);//1
        list.add(1250);//0
        list.add(2500);//1
        list.add(2500);//1
        list.add(1250);//0
        list.add(2500);//1
        list.add(1250);//0
        list.add(1250);//0
        list.add(1250);//0
        list.add(2500);//1*/

        list.add(2500);//1
        list.add(1250);//0
        list.add(2500);//1
        list.add(1250);//0
        list.add(1250);//0
        list.add(2500);//0
        list.add(1250);//1
        list.add(2500);//0
        list.add(2500);//1
        list.add(2500);//1
        list.add(1250);//0
        list.add(1250);//1
        list.add(1250);//0
        list.add(1250);//0
        list.add(2500);//1

        list.add(232);
        list.add(2500);
        list.add(2345);
        list.add(67);
        list.add(12);
        list.add(900);

        long time = System.currentTimeMillis();
        System.out.println("123456".substring(1, 1 + 3));
        for (Integer integer : list) {
            decoderData(integer);
        }
        System.out.println("time:" + (System.currentTimeMillis() - time));
    }

    private static final int HIGH_FREQ = 2500;
    private static final int LOW_FREQ = 1250;
    private static final String START_CODE = "1010";//起始码
    private static final String END_CODE = "01";//结束码
    private static final int bit = 8;//数据长度8位
    private static final int validate = 1;//校验码1位

    private void decoderData(int freq) {
        short currentSampleBit = freqByBit(freq);//将频率转化为01
        if (currentSampleBit != -1) {//说明是有用数据
            if (mShortBuf.limit() != mShortBuf.capacity()) {//说明是读模式，从写切换到读。
                mShortBuf.flip();
            }
            if (mShortBuf.position() >= mShortBuf.capacity()) {//说明写满了
                mShortBuf.clear();//清掉数据
            }
            mShortBuf.put(currentSampleBit);
        } else {//无用数据
            mShortBuf.flip();
            int all_length = mShortBuf.limit();
            if (all_length >= all_len) {//说明本次一个数据接收完成
                final String strdata = Arrays.toString(mShortBuf.array()).replaceAll("[\\[\\]\\s,]", "");
                int startindex = strdata.indexOf(START_CODE);//起始码位置索引
                System.out.println("startindex:" + startindex);
                if (startindex > -1 && (all_length - startindex) >= all_len) {//找到了起始位置
                    System.out.println("validate START_CODE success!!");
                    final int validateIndex = startindex + START_CODE.length() + bit;//查找校验位置
                    final short validateBit = mShortBuf.get(validateIndex);//获取校验值
                    System.out.println("validateBit：" + validateBit);
                    final short[] data = new short[START_CODE.length() + bit];//获取起始位+数据位
                    mShortBuf.position(startindex);//移动游标到起始位
                    mShortBuf.get(data, 0, data.length);
                    System.out.println("data Bit：" + Arrays.toString(data));
                    short parityBit = 0;//求出校验的数据位和
                    byte dataRxByte = 0;//数据位计算
                    for (int i = 0; i < data.length; i++) {
                        parityBit += i;
                        if (i > START_CODE.length()) {//数据位开始累加
                            dataRxByte |= (data[i] << (data.length - i - 1));
                        }
                    }
                    System.out.println("parityBit：" + (parityBit & 0x01));
                    if (validateBit == (parityBit & 0x01)) {//校验成功
                        System.out.println("validate DATA success!!");
                        int endIndex = startindex + START_CODE.length() + bit + validate;
                        String endStr = strdata.substring(endIndex, endIndex + END_CODE.length());
                        if (endStr.equals(END_CODE)) {//找到了结束位置，发送解码的数据
                            System.out.println("validate END_CODE success!!");
                            String str = " ";
                            if (dataRxByte < 16 && dataRxByte > -16) str += '0';
                            str += Integer.toHexString((int) (dataRxByte & 0xff));
                            System.out.println("@@@@@@@@@" + str);
                        } else {
                            System.out.println("validate END_CODE fail!!");
                        }
                    } else {
                        System.out.println("validate DATA fail!!");
                    }
                } else {
                    System.out.println("validate START_CODE fail!!" + startindex);
                }
            }
            mShortBuf.clear();
        }
    }

    private short freqByBit(int freq) {
        short sampleBit = -1;
        switch (freq) {
            case HIGH_FREQ:
                sampleBit = 1;
                break;
            case LOW_FREQ:
                sampleBit = 0;
                break;
        }
        return sampleBit;
    }
    @Test
    public void queue() throws Exception {
        final LinkedBlockingQueue<String> mQueueCache = new LinkedBlockingQueue(2);
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(">>>>>>>>>>>>>>>>>");
                while (true){
                    try {
                        System.out.println(">>>>>>>>1>>>>>>>>>");
                        String value = mQueueCache.take();
                        System.out.println(value);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("=============");
                int k = 100000;
                for (int i = 0; i < k; i++) {
                    try {
                        mQueueCache.put("测试"+i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                   /* try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                }
            }
        }).start();
    }
}