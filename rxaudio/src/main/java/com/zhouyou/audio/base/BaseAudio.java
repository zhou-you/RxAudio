package com.zhouyou.audio.base;

import com.zhouyou.audio.util.AudioLog;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>描述：音频线程基类</p>
 * 1.开启线程<br>
 * 2.停止线程<br>
 * 3.是否在运行中<br>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/5 17:38<br>
 * 版本： v2.0<br>
 */
public abstract class BaseAudio implements Runnable, IAudio {
    private boolean isRun = false;
    private boolean isLog = true;
    private final ReadWriteLock mLock = new ReentrantReadWriteLock();
    private Thread mThread;
    @Override
    public final void start() {
        mLock.readLock().lock();
        try {
            if (isRun) {
                print("Already running!!!!!!!!!!!");
                return;
            }
            doStart();
            mThread = new Thread(this);
            mThread.start();
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    public final void stop() {
        mLock.readLock().lock();
        try {
            if (isRun == false) {
                print("Already stop!!!!!!!!!!!");
                return;
            }
            isRun = false;
            doStop();
            //mThread.sleep(50);
            //mThread.interrupt();
            mThread.join(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    public final boolean isRuning() {
        return isRun;
    }

    public void print(String msg) {
        if (isLog) AudioLog.i(msg);
    }

    @Override
    public final void run() {
        isRun = true;
        doRun();
    }

    protected abstract void doStart();

    protected abstract void doStop();

    protected abstract void doRun();
}
