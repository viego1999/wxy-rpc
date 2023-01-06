package com.wxy.rpc.core.loadbalance;

import com.wxy.rpc.core.exception.LoadBalanceException;

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
public class RandomLoadBalance implements LoadBalance {

    final Random random = new Random();

    @Override
    public <T> T chooseOne(List<T> objects) {
        if (objects == null || objects.isEmpty()) {
            throw new LoadBalanceException("The service list is empty.");
        }
        return objects.get(random.nextInt(objects.size()));
    }
}
