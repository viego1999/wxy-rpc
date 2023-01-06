package com.wxy.rpc.core.loadbalance;

import java.util.List;

/**
 * 负载均衡 接口类
 *
 * @author Wuxy
 * @version 1.0
 */
public interface LoadBalance {

    /**
     * 负载均衡，从传入的服务列表中按照指定的策略返回一个
     *
     * @param objects 服务列表
     * @param <T>     服务类型
     * @return 按策略返回的服务
     */
    <T> T chooseOne(List<T> objects);

}
