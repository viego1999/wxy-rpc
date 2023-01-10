package com.wxy.rpc.core.loadbalance.impl;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡策略实现类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RandomLoadBalance
 * @Date 2023/1/5 16:35
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    final Random random = new Random();

    @Override
    protected ServiceInfo doSelect(List<ServiceInfo> invokers, RpcRequest request) {
        return invokers.get(random.nextInt(invokers.size()));
    }
}
