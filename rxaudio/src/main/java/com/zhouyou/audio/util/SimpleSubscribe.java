package com.zhouyou.audio.util;

/**
 * <p>描述：封装的订阅者</p>
 * 作者： ~若相惜<br>
 * 日期： 2017/2/25 12:09<br>
 * 版本： v2.0<br>
 */

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;

public abstract class SimpleSubscribe<T> implements Observable.OnSubscribe<T> {
    @Override
    public final void call(Subscriber<? super T> subscriber) {
        try {
            T data = execute();
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(data);
            }
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            if (!subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
            return;
        }

        if (!subscriber.isUnsubscribed()) {
            subscriber.onCompleted();
        }
    }

    public abstract T execute() throws Throwable;
}
