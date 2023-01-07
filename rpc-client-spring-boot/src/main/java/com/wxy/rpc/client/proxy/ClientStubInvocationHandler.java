package com.wxy.rpc.client.proxy;

import com.wxy.rpc.client.common.RequestMetadata;
import com.wxy.rpc.client.config.RpcClientProperties;
import com.wxy.rpc.client.transport.RpcClient;
import com.wxy.rpc.client.transport.RpcClientFactory;
import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.RpcResponse;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.discovery.ServiceDiscovery;
import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.core.protocol.MessageHeader;
import com.wxy.rpc.core.protocol.RpcMessage;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 客户端方法调用处理器类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName ClientStubInvocationHandler
 * @Date 2023/1/7 14:03
 */
public class ClientStubInvocationHandler implements InvocationHandler {

    private final ServiceDiscovery serviceDiscovery;

    private final RpcClientProperties properties;

    private final String serviceName;

    public ClientStubInvocationHandler(ServiceDiscovery serviceDiscovery, RpcClientProperties properties, String serviceName) {
        this.serviceDiscovery = serviceDiscovery;
        this.properties = properties;
        this.serviceName = serviceName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 进行服务发现
        ServiceInfo serviceInfo = serviceDiscovery.discover(serviceName);
        if (serviceInfo == null) {
            throw new RpcException(String.format("The service [%s] was not found in the remote registry center.",
                    serviceName));
        }
        // 构建请求头
        MessageHeader header = MessageHeader.build(properties.getSerialization());
        // 构建请求体
        RpcRequest request = new RpcRequest();
        request.setServiceName(serviceName);
        request.setMethod(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameterValues(args);
        // 构建 通信协议
        RpcMessage protocol = new RpcMessage();
        protocol.setHeader(header);
        protocol.setBody(request);

        // 构建请求元数据
        RequestMetadata metadata = RequestMetadata.builder()
                .rpcMessage(protocol)
                .serverAddr(serviceInfo.getServiceName())
                .port(serviceInfo.getPort())
                .timeout(properties.getTimeout()).build();

        // 获得 RpcClient 实现类
        RpcClient rpcClient = RpcClientFactory.getRpcClient(properties.getTransport());
        // 发送网络请求，获取结果
        RpcMessage responseRpcMessage = rpcClient.sendRpcRequest(metadata);

        if (responseRpcMessage == null) {
            throw new RpcException("Remote procedure call timeout.");
        }

        // 获取响应结果
        RpcResponse response = (RpcResponse) responseRpcMessage.getBody();

        // 如果 远程调用 发生错误
        if (response.getExceptionValue() != null) {
            throw new RpcException(response.getExceptionValue());
        }
        // 返回响应结果
        return response.getReturnValue();
    }
}
