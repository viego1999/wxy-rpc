package com.wxy.rpc.core.loadbalance.impl;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.exception.LoadBalanceException;
import com.wxy.rpc.core.loadbalance.LoadBalance;

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
    public ServiceInfo select(List<ServiceInfo> invokers, RpcRequest request) {
        if (invokers == null || invokers.isEmpty()) {
            throw new LoadBalanceException("The service list is empty.");
        }
        return invokers.get(current.getAndIncrement() % invokers.size());
    }
}
