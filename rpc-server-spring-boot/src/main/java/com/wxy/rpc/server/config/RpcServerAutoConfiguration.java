package com.wxy.rpc.server.config;

import com.wxy.rpc.core.registry.ServiceRegistry;
import com.wxy.rpc.core.registry.nacos.NacosServiceRegistry;
import com.wxy.rpc.core.registry.zk.ZookeeperServiceRegistry;
import com.wxy.rpc.server.spring.RpcServerBeanPostProcessor;
import com.wxy.rpc.server.transport.RpcServer;
import com.wxy.rpc.server.transport.http.HttpRpcServer;
import com.wxy.rpc.server.transport.netty.NettyRpcServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * RpcServer 端的自动配置类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcServerAutoConfiguration
 * @Date 2023/1/8 12:34
 */
@Configuration
@EnableConfigurationProperties(RpcServerProperties.class)
public class RpcServerAutoConfiguration {

    @Autowired
    RpcServerProperties properties;

    @Bean(name = "serviceRegistry")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "registry", havingValue = "nacos")
    public ServiceRegistry nacosServiceRegistry() {
        return new NacosServiceRegistry(properties.getRegistryAddr());
    }

    /**
     * 创建 ServiceRegistry 实例 bean，当没有配置时默认使用 zookeeper 作为配置中心
     */
    @Bean(name = "serviceRegistry")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "registry", havingValue = "zookeeper")
    public ServiceRegistry zookeeperServiceRegistry() {

        return new ZookeeperServiceRegistry(properties.getRegistryAddr());
    }

    @Bean(name = "rpcServer")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "transport", havingValue = "http")
    public RpcServer httpRpcServer() {
        return new HttpRpcServer();
    }

    // 当没有配置通信协议属性时，默认使用 netty 作为通讯协议
    @Bean(name = "rpcServer")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "transport", havingValue = "netty")
    public RpcServer nettyRpcServer() {
        return new NettyRpcServer();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ServiceRegistry.class, RpcServer.class})
    public RpcServerBeanPostProcessor rpcServerBeanPostProcessor(@Autowired ServiceRegistry serviceRegistry,
                                                                 @Autowired RpcServer rpcServer,
                                                                 @Autowired RpcServerProperties properties) {

        return new RpcServerBeanPostProcessor(serviceRegistry, rpcServer, properties);
    }

}
