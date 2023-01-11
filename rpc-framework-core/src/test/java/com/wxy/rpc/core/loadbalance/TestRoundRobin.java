package com.wxy.rpc.core.loadbalance;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestRoundRobin
 * @Date 2023/1/11 12:16
 */
public class TestRoundRobin {

    static final AtomicInteger ai = new AtomicInteger(Integer.MAX_VALUE - 2);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch count = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    getAndIncrement();
                }
                count.countDown();
            }).start();
        }
        count.await();
        System.out.println(getAndIncrement());
    }

    public static int getAndIncrement() {
        int prev, next;
        do {
            prev = ai.get();
            next = prev == Integer.MAX_VALUE ? 0 : prev + 1;
        } while (!ai.compareAndSet(prev, next));
        return prev;
    }
}
