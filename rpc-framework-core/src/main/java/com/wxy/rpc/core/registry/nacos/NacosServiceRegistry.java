package com.wxy.rpc.core.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.core.registry.ServiceRegistry;
import com.wxy.rpc.core.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Nacos 实现服务注册中心类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName NacosServiceRegistry
 * @see com.alibaba.nacos.api.naming.NamingService
 * @see com.alibaba.nacos.api.naming.pojo.Instance
 * @see com.alibaba.nacos.api.naming.NamingFactory
 * @Date 2023/1/8 16:18
 */
@Slf4j
public class NacosServiceRegistry implements ServiceRegistry {

    /**
     * Nacos 命名服务
     */
    private NamingService namingService;


    /**
     * 构造方法，传入 nacos 的连接地址，例如：localhost:8848
     *
     * @param registryAddr nacos 连接地址
     */
    public NacosServiceRegistry(String registryAddr) {
        try {
            // 创建Nacos命名服务
            namingService = NamingFactory.createNamingService(registryAddr);

        } catch (Exception e) {
            log.error("An error occurred while starting the nacos registry: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void register(ServiceInfo serviceInfo) {
        try {
            // 创建服务实例
            Instance instance = new Instance();
            instance.setServiceName(serviceInfo.getServiceName());
            instance.setIp(serviceInfo.getAddress());
            instance.setPort(serviceInfo.getPort());
            instance.setHealthy(true); // 服务是否健康，和服务发现有关，默认为 true
            instance.setMetadata(ServiceUtil.toMap(serviceInfo));

            // 注册实例
            namingService.registerInstance(instance.getServiceName(), instance);

            log.info("Successfully registered [{}] service.", instance.getServiceName());
        } catch (Exception e) {
            throw new RpcException(String.format("An error occurred when rpc server registering [%s] service.",
                    serviceInfo.getServiceName()), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unregister(ServiceInfo serviceInfo) {
        try {
            // 创建服务实例
            Instance instance = new Instance();
            instance.setServiceName(serviceInfo.getServiceName());
            instance.setIp(serviceInfo.getAddress());
            instance.setPort(serviceInfo.getPort());
            instance.setHealthy(true); // 服务是否健康，和服务发现有关，默认为 true
            instance.setMetadata(ServiceUtil.toMap(serviceInfo));

            namingService.deregisterInstance(instance.getServiceName(), instance);
            log.warn("Successfully unregistered {} service.", instance.getServiceName());
        } catch (NacosException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        namingService.shutDown();
        log.info("Destroy nacos registry completed.");
    }
}
