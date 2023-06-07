package com.wxy.rpc.server.spring;

import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.registry.ServiceRegistry;
import com.wxy.rpc.core.util.ServiceUtil;
import com.wxy.rpc.server.annotation.RpcService;
import com.wxy.rpc.server.config.RpcServerProperties;
import com.wxy.rpc.server.store.LocalServiceCache;
import com.wxy.rpc.server.transport.RpcServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;

/**
 * Rpc Server Bean Processor class
 * <p>
 * 该类主要用于 spring 容器启动时，将被 {@link com.wxy.rpc.server.annotation.RpcService} 标注的服务进行注册并暴露
 * </p>
 */
@Slf4j
public class RpcServerBeanPostProcessor implements BeanPostProcessor, CommandLineRunner {

    private final ServiceRegistry serviceRegistry;

    private final RpcServer rpcServer;

    private final RpcServerProperties properties;

    public RpcServerBeanPostProcessor(ServiceRegistry serviceRegistry, RpcServer rpcServer, RpcServerProperties properties) {
        this.serviceRegistry = serviceRegistry;
        this.rpcServer = rpcServer;
        this.properties = properties;
    }

    /**
     * 在 bean 实例化后，初始化后，检测标注有 @RpcService 注解的类，将对应的服务类进行注册，对外暴露服务，同时进行本地服务注册
     *
     * @param bean     bean
     * @param beanName beanName
     * @return 返回增强后的 bean
     * @throws BeansException Bean 异常
     */
    @SneakyThrows
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 判断当前 bean 是否被 @RpcService 注解标注
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with [{}].", bean.getClass().getName(), RpcService.class.getCanonicalName());
            // 获取到该类的 @RpcService 注解
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            String interfaceName;
            if ("".equals(rpcService.interfaceName())) {
                interfaceName = rpcService.interfaceClass().getName();
            } else {
                interfaceName = rpcService.interfaceName();
            }
            String version = rpcService.version();
            String serviceName = ServiceUtil.serviceKey(interfaceName, version);
            // 构建 ServiceInfo 对象
            ServiceInfo serviceInfo = ServiceInfo.builder()
                    .appName(properties.getAppName())
                    .serviceName(serviceName)
                    .version(version)
                    .address(properties.getAddress())
                    .port(properties.getPort())
                    .build();
            // 进行远程服务注册
            serviceRegistry.register(serviceInfo);
            // 进行本地服务缓存注册
            LocalServiceCache.addService(serviceName, bean);
        }
        return bean;
    }

    /**
     * 开机自启动 - 此方法实现于 {@link CommandLineRunner} 接口，基于 springboot
     *
     * @param args incoming main method arguments 命令行参数
     * @throws Exception 启动异常
     */
    @Override
    public void run(String... args) throws Exception {
        new Thread(() -> rpcServer.start(properties.getPort())).start();
        log.info("Rpc server [{}] start, the appName is {}, the port is {}",
                rpcServer, properties.getAppName(), properties.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 当服务关闭之后，将服务从 注册中心 上清除（关闭连接）
                serviceRegistry.destroy();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
