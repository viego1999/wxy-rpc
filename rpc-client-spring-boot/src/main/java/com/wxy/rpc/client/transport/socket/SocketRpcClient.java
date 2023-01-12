package com.wxy.rpc.client.transport.socket;

import com.wxy.rpc.client.common.RequestMetadata;
import com.wxy.rpc.client.transport.RpcClient;
import com.wxy.rpc.core.common.RpcResponse;
import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.core.protocol.RpcMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于 Socket 实现 RpcClient 类
 * <p>
 *     SocketClient 发送和接受的数据为：RpcRequest, RpcResponse
 * </p>
 * @author Wuxy
 * @version 1.0
 * @ClassName SocketRpcClient
 * @Date 2023/1/12 13:36
 */
public class SocketRpcClient implements RpcClient {

    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        // 获取服务器地址和端口，构建 socket address
        InetSocketAddress socketAddress = new InetSocketAddress(requestMetadata.getServerAddr(), requestMetadata.getPort());
        try (Socket socket = new Socket()) {
            // 与服务器建立连接
            socket.connect(socketAddress);
            // 注意：SocketClient 发送和接受的数据为：RpcRequest, RpcResponse
            // 发送数据给服务端
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(requestMetadata.getRpcMessage().getBody());
            oos.flush();
            // 阻塞等待服务端的响应
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            RpcResponse response = (RpcResponse) ois.readObject();
            // 封装成 RpcMessage 对象
            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setBody(response);
            return rpcMessage;
        } catch (IOException | ClassNotFoundException e) {
            throw new RpcException("The socket client failed to send or receive message.", e);
        }
    }
}
