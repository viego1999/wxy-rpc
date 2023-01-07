package com.wxy.rpc.client.transport;

import com.wxy.rpc.client.transport.http.HttpRpcClient;
import com.wxy.rpc.client.transport.netty.NettyRpcClient;

/**
 * 获取指定传输通信协议的 Rpc 客户端实现类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcClientFactory
 * @Date 2023/1/7 15:33
 */
public class RpcClientFactory {

    /**
     * 根据指定的通信协议去生成对应的 RpcClient 实现类
     *
     * @param transport 指定的通信协议
     * @return 返回指定通信协议的 Rpc 客户端实现类
     */
    // todo：实现 dubbo 的 ExtensionLoader 机制去动态加载
    public static RpcClient getRpcClient(String transport) {
        switch (transport) {
            case "netty":
                return new NettyRpcClient();
            case "http":
                return new HttpRpcClient();
            default:
                throw new IllegalArgumentException(String.format("Transport communication protocol type %s is illegal.",
                        transport));
        }
    }

}
