package com.wxy.rpc.server.transport.socket;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.RpcResponse;
import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.server.store.LocalServiceCache;
import com.wxy.rpc.server.transport.RpcServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Socket 的 RpcServer 实现类
 * <p>
 * SocketServer 接受和发送的数据为：RpcRequest, RpcResponse
 * </p>
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName SocketRpcServer
 * @Date 2023/1/12 12:07
 */
@Slf4j
public class SocketRpcServer implements RpcServer {

    /**
     * 当前处理器数量
     */
    private final int cpuNum = Runtime.getRuntime().availableProcessors();

    // 线程大小：这一点要看我们执行的任务是cpu密集型，还是io密集型
    // 如果有关于计算机计算，比较消耗资源的是cpu密集型，线程大小应该设置为：cpu 核数 + 1
    // 如果有关网络传输，连接数据库等，是io密集型，线程大小应该设置为：cpu * 2
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(cpuNum * 2, cpuNum * 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));

    @Override
    public void start(Integer port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            serverSocket.bind(new InetSocketAddress(hostAddress, port));
            Socket socket;
            // 循环接受客户端 Socket 连接（accept为阻塞时等待连接）
            while ((socket = serverSocket.accept()) != null) {
                log.info("The client connected [{}].", socket.getInetAddress());
                threadPool.execute(new SocketRpcRequestRunnable(socket));
            }
            // 服务端连断开，关闭线程池
            threadPool.shutdown();
        } catch (IOException e) {
            throw new RpcException(String.format("The socket server failed to start on port %d.", port), e);
        }
    }

    /**
     * 处理 RpcRequest，基于 Socket 通信
     *
     * @author Wuxy
     * @version 1.0
     * @ClassName SocketRpcRequestHandler
     * @Date 2023/1/12 13:14
     */
    private static class SocketRpcRequestRunnable implements Runnable {
        private final Socket socket;

        public SocketRpcRequestRunnable(Socket socket) {
            this.socket = socket;
        }

        @SuppressWarnings("Duplicates")
        @Override
        public void run() {
            log.info("The server handle client message by thread {}.", Thread.currentThread().getName());
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                // 注意：SocketServer 接受和发送的数据为：RpcRequest, RpcResponse
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                // 直接读取客户端发送过来的 RpcRequest，此时不需要进行编解码，无需消息协议
                RpcRequest request = (RpcRequest) ois.readObject();
                log.info("The server received message is {}.", request);
                // 创建一个 RpcResponse 对象用来响应给客户端
                RpcResponse response = new RpcResponse();
                // 处理请求
                try {
                    // 获取请求的服务对应的实例对象
                    Object service = LocalServiceCache.getService(request.getServiceName());
                    // 如果请求服务不存在
                    if (service == null) {
                        log.error("The service [{}] is not exist!", request.getServiceName());
                        throw new RpcException(String.format("The service [%s] is not exist!", request.getServiceName()));
                    }
                    Method method = service.getClass().getMethod(request.getMethod(), request.getParameterTypes());
                    Object result = method.invoke(service, request.getParameterValues());
                    response.setReturnValue(result);
                } catch (Exception e) {
                    log.error("The service [{}], the method [{}] invoke failed!", request.getServiceName(), request.getMethod());
                    // 若不设置，堆栈信息过多，导致报错
                    response.setExceptionValue(new RpcException("Error in remote procedure call, " + e.getMessage()));
                }
                log.info("The response is {}.", response);
                oos.writeObject(response);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("The socket server failed to handle client rpc request.", e);
            }
        }
    }
}
