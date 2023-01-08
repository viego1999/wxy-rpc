package com.wxy.rpc.core.registry.nacos;

import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.loadbalance.LoadBalance;
import com.wxy.rpc.core.registry.ServiceRegistry;

import java.io.IOException;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName NacosServiceRegistry
 * @Date 2023/1/8 16:18
 */
public class NacosServiceRegistry implements ServiceRegistry {

    private final String registryAddr;


    public NacosServiceRegistry(String registryAddr) {
        this.registryAddr = registryAddr;
    }

    @Override
    public void register(ServiceInfo serviceInfo) throws Exception {

    }

    @Override
    public void unregister(ServiceInfo serviceInfo) throws Exception {

    }

    @Override
    public void destroy() throws IOException {

    }
}
