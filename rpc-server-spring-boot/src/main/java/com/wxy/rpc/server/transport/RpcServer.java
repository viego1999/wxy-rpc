package com.wxy.rpc.server.transport;

/**
 * Rpc 服务类，接受客户端消息，调用客户端请求的方法并将结果返回给客户端
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcServer
 * @Date 2023/1/6 17:28
 */
public interface RpcServer {

    /**
     * 开启 RpcServer 服务
     *
     * @param port 启动端口
     */
    void start(Integer port);

}
