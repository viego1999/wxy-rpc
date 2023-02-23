package com.wxy.rpc.server.transport.socket;

import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.server.transport.RpcServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
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
                log.debug("The client connected [{}].", socket.getInetAddress());
                threadPool.execute(new SocketRpcRequestHandler(socket));
            }
            // 服务端连断开，关闭线程池
            threadPool.shutdown();
        } catch (IOException e) {
            throw new RpcException(String.format("The socket server failed to start on port %d.", port), e);
        }
    }
}
