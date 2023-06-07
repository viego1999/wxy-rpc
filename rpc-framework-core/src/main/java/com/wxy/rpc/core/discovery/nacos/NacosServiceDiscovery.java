package com.wxy.rpc.core.discovery.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.discovery.ServiceDiscovery;
import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.core.loadbalance.LoadBalance;
import com.wxy.rpc.core.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Nacos 实现服务发现实现类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName NacosServiceDiscovery
 * @Date 2023/1/8 16:19
 * @see NamingService
 * @see com.alibaba.nacos.api.naming.pojo.Instance
 */
@Slf4j
public class NacosServiceDiscovery implements ServiceDiscovery {
    /**
     * Nacos 命名服务
     */
    private NamingService namingService;

    /**
     * 负载均衡算法
     */
    private LoadBalance loadBalance;

    /**
     * 用来将服务列表缓存到本地内存，当服务发生变化时，由 serviceCache 进行服务列表更新操作，当 nacos 挂掉时，将保存当前服务列表以便继续提供服务
     */
    private final Map<String, List<ServiceInfo>> serviceMap = new ConcurrentHashMap<>();


    /**
     * 构造方法，传入 nacos 连接地址和指定的负载均衡算法
     *
     * @param registryAddr nacos服务地址，例如 localhost:8848
     * @param loadBalance  负载均衡算法
     */
    public NacosServiceDiscovery(String registryAddr, LoadBalance loadBalance) {
        try {
            this.loadBalance = loadBalance;
            this.namingService = NamingFactory.createNamingService(registryAddr);
        } catch (NacosException e) {
            log.error("An error occurred while starting the nacos discovery: ", e);
        }
    }

    @Override
    public ServiceInfo discover(RpcRequest request) {
        try {
            return loadBalance.select(getServices(request.getServiceName()), request);
        } catch (Exception e) {
            throw new RpcException(String.format("Remote service discovery did not find service %s.",
                    request.getServiceName()), e);
        }
    }

    @Override
    public List<ServiceInfo> getServices(String serverName) {
        try {
            // 如果当前服务列表没有被缓存
            if (!serviceMap.containsKey(serverName)) {
                // 将 instances 列表进行映射为 serviceInfo 列表
                List<ServiceInfo> instances = namingService.getAllInstances(serverName).stream()
                        .map(instance -> ServiceUtil.toServiceInfo(instance.getMetadata()))
                        .collect(Collectors.toList());
                // 加入缓存 map 中
                serviceMap.put(serverName, instances);

                // 注册指定服务名称下的监听事件，用来实时更新本地服务缓存列表
                namingService.subscribe(serverName, event -> {
                    NamingEvent namingEvent = (NamingEvent) event;
                    log.info("The service [{}] cache has changed. The current number of service samples is {}."
                            , serverName, namingEvent.getInstances().size());

                    // 更新本地服务列表缓存
                    serviceMap.put(namingEvent.getServiceName(), namingEvent.getInstances().stream()
                            .map(instance -> ServiceUtil.toServiceInfo(instance.getMetadata()))
                            .collect(Collectors.toList()));
                });
            }
            return serviceMap.get(serverName);
        } catch (NacosException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        namingService.shutDown();
        log.info("Destroy nacos discovery completed.");
    }
}
