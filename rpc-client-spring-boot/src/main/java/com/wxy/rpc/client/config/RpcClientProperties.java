package com.wxy.rpc.client.config;

import lombok.Data;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcClientProperties
 * @Date 2023/1/7 15:12
 */
@Data
public class RpcClientProperties {

    /**
     * 负载均衡算法
     */
    private String loadBalance;

    /**
     * 序列化算法
     */
    private String serialization;

    /**
     * 通信协议
     */
    private String transport = "netty";

    /**
     * 服务发现（注册中心）地址
     */
    private String registerAddr = "192.168.247.130:2181";

    /**
     * 连接超时时间
     */
    private Integer timeout;

}
