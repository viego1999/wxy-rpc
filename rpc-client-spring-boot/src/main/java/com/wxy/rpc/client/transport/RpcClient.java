package com.wxy.rpc.client.transport;

import com.wxy.rpc.client.common.RequestMetadata;
import com.wxy.rpc.core.protocol.RpcMessage;

/**
 * Rpc 客户端类，负责向服务端发起请求（远程过程调用）
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcClient
 * @Date 2023/1/6 17:28
 */
public interface RpcClient {

    /**
     * 发起远程过程调用
     *
     * @param requestMetadata rpc 请求元数据
     * @return 响应结果
     */
    RpcMessage sendRpcRequest(RequestMetadata requestMetadata);

}
