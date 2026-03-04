package com.yuxuan.utils;

public interface ILock {
    /**
     *尝试获取锁，并设置过期时间
     * 非阻塞式，放回true就执行业务，返回false就执行其他的不用等待
     */
    boolean tryLock(long timeoutSec);

    void unlock();
}
