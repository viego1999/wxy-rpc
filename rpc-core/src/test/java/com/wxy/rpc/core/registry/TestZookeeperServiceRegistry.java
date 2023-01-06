package com.wxy.rpc.core.registry;

import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.registry.zk.ZookeeperServiceRegistry;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestZookeeperServiceRegistry
 * @Date 2023/1/5 22:50
 */
public class TestZookeeperServiceRegistry {

    public static void main(String[] args) throws Exception {
        ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry("192.168.247.130:2181");

        ServiceInfo serviceInfo = ServiceInfo.builder()
                .appName("rpc")
                .serviceName("com.wxy.rpc.api.service.HelloService")
                .version("1.0")
                .address(InetAddress.getLocalHost().getHostAddress())
                .port(8081)
                .build();

        serviceRegistry.register(serviceInfo);

        TimeUnit.SECONDS.sleep(3);

        serviceRegistry.unregister(serviceInfo);

        TimeUnit.SECONDS.sleep(3);

        serviceRegistry.destroy();
    }

}
