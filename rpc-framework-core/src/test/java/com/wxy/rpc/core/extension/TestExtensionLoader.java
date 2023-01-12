package com.wxy.rpc.core.extension;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.discovery.ServiceDiscovery;
import com.wxy.rpc.core.loadbalance.LoadBalance;
import com.wxy.rpc.core.registry.ServiceRegistry;
import com.wxy.rpc.core.serialization.Serialization;

import java.util.Arrays;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestExtensionLoader
 * @Date 2023/1/11 20:21
 */
public class TestExtensionLoader {

    public static void main(String[] args) {
        ExtensionLoader<LoadBalance> loadBalanceExtensionLoader = ExtensionLoader.getExtensionLoader(LoadBalance.class);
        LoadBalance random = loadBalanceExtensionLoader.getExtension("random");
        System.out.println(random.select(Arrays.asList(
                ServiceInfo.builder().port(1).build(),
                ServiceInfo.builder().port(2).build(),
                ServiceInfo.builder().port(3).build()), new RpcRequest()));
        System.out.println(random);

        ExtensionLoader<ExtensionFactory> factoryExtensionLoader = ExtensionLoader.getExtensionLoader(ExtensionFactory.class);
        System.out.println(factoryExtensionLoader.getExtension("spi"));

        ExtensionLoader<Serialization> serializationExtensionLoader = ExtensionLoader.getExtensionLoader(Serialization.class);
        System.out.println(serializationExtensionLoader.getExtension("protostuff"));

        // 下方需要依赖注入，解决（实现 IOC 或提供 空参构造方法）

        ExtensionLoader<ServiceDiscovery> discoveryExtensionLoader = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class);
        System.out.println(discoveryExtensionLoader.getExtension("zk"));

        ExtensionLoader<ServiceRegistry> registryExtensionLoader = ExtensionLoader.getExtensionLoader(ServiceRegistry.class);
        System.out.println(registryExtensionLoader.getExtension("zk"));
    }

}
