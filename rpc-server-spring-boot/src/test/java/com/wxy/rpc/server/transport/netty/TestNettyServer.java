package com.wxy.rpc.server.transport.netty;

import com.wxy.rpc.server.transport.RpcServer;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestNettyServer
 * @Date 2023/1/7 19:59
 */
public class TestNettyServer {

    public static void main(String[] args) {
        RpcServer rpcServer = new NettyRpcServer();
        rpcServer.start(8880);
    }

}
