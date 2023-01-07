package com.wxy.rpc.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcServerProperties
 * @Date 2023/1/6 23:33
 */
@Data
@Component
@ConfigurationProperties(prefix = "rpc.server")
public class RpcServerProperties {

    /**
     * 服务启动端口
     */
    private Integer port;

    /**
     * 服务名称
     */
    private String appName;

    /**
     * 注册中心地址
     */
    private String registryAddr = "192.168.247.130:2181";

}
