package com.wxy.rpc.core.loadbalance;

import java.util.List;

/**
 * 一致性哈希负载均衡算法，参考：
 * <a href="https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java">dubbo一致性哈希算法</a>
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName ConsistentHashLoadBalance
 * @Date 2023/1/8 12:10
 */
public class ConsistentHashLoadBalance implements LoadBalance {
    @Override
    public <T> T chooseOne(List<T> objects) {
        return null;
    }
}
