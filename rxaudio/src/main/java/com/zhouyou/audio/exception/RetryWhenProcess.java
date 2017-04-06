package com.zhouyou.audio.exception;

import com.zhouyou.audio.util.AudioLog;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * <p>描述：错误重试</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/14 18:09<br>
 * 版本： v2.0<br>
 */
public class RetryWhenProcess implements Func1<Observable<? extends Throwable>, Observable<?>> {
    /* retry次数*/
    private int count = 3;
    /*延迟*/
    private long delay = 500;

    public RetryWhenProcess() {
    }

    public RetryWhenProcess(int count, long delay) {
        this.count = count;
        this.delay = delay;
    }

    @Override
    public Observable<?> call(Observable<? extends Throwable> observable) {
        return observable.zipWith(Observable.range(1, count), new Func2<Throwable, Integer, Wrapper>() {
            @Override
            public Wrapper call(Throwable throwable, Integer integer) {
                return new Wrapper(throwable, integer);
            }
        }).flatMap(new Func1<Wrapper, Observable<Long>>() {
            @Override
            public Observable<Long> call(Wrapper wrapper) {
                AudioLog.i("#######重试次数：" + wrapper.index+" err:"+wrapper.throwable.getMessage());
                if ((wrapper.throwable instanceof PlayException || wrapper.throwable instanceof ReceiveException || wrapper.throwable instanceof TimeoutException)
                        && wrapper.index < count) { //如果超出重试次数也抛出错误，否则默认是会进入onCompleted
                    return Observable.timer(delay, TimeUnit.MILLISECONDS);
                }
                return Observable.error(wrapper.throwable);
            }
        });
    }

    private class Wrapper {
        private int index;
        private Throwable throwable;

        public Wrapper(Throwable throwable, int index) {
            this.index = index;
            this.throwable = throwable;
        }
    }

}
