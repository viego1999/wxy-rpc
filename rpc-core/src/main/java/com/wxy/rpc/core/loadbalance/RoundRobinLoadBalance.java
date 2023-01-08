package com.wxy.rpc.core.loadbalance;

import com.wxy.rpc.core.exception.LoadBalanceException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡算法
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RoundRobinLoadBalance
 * @Date 2023/1/8 12:14
 */
public class RoundRobinLoadBalance implements LoadBalance {

    private static final AtomicInteger current = new AtomicInteger(0);

    @Override
    public <T> T chooseOne(List<T> objects) {
        if (objects == null || objects.isEmpty()) {
            throw new LoadBalanceException("The service list is empty.");
        }
        return objects.get(current.getAndIncrement() % objects.size());
    }
}
