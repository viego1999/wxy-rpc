package com.wxy.rpc.client.transport.http;

import com.wxy.rpc.client.common.RequestMetadata;
import com.wxy.rpc.client.transport.RpcClient;
import com.wxy.rpc.core.protocol.RpcMessage;

/**
 * 基于 HTTP 通信协议实现的 Rpc Client 类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName HttpRpcClient
 * @Date 2023/1/7 11:12
 */
public class HttpRpcClient implements RpcClient {

    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        return null;
    }

}
