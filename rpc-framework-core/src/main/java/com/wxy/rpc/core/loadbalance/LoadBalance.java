package com.wxy.rpc.core.loadbalance;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.extension.SPI;

import java.util.List;

/**
 * 负载均衡 接口类
 * <p>
 * <a href="https://cn.dubbo.apache.org/zh/docsv2.7/dev/source/loadbalance/">dubbo负载均衡参考资料</a>
 * </p>
 *
 * @author Wuxy
 * @version 1.0
 */
@SPI
public interface LoadBalance {

    /**
     * 负载均衡，从传入的服务列表中按照指定的策略返回一个
     *
     * @param invokers 服务列表
     * @param request rpc请求
     * @return 按策略返回的服务信息对象
     */
    ServiceInfo select(List<ServiceInfo> invokers, RpcRequest request);

}
