package com.wxy.rpc.client.transport.netty;

import com.wxy.rpc.client.common.RequestMetadata;
import com.wxy.rpc.client.handler.RpcResponseHandler;
import com.wxy.rpc.client.transport.RpcClient;
import com.wxy.rpc.core.codec.RpcFrameDecoder;
import com.wxy.rpc.core.codec.SharableRpcMessageCodec;
import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.core.factory.SingletonFactory;
import com.wxy.rpc.core.protocol.RpcMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于 Netty 实现的 Rpc Client 类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName NettyRpcClient
 * @Date 2023/1/6 17:28
 */
@Slf4j
public class NettyRpcClient implements RpcClient {

    /**
     * Netty 通信启动类
     */
    private final Bootstrap bootstrap;

    /**
     * 事件循环对象组，每一个事件循环对象对应一个线程（维护一个 Selector），用来处理 channel 上的 io 事件
     */
    private final EventLoopGroup eventLoopGroup;

    /**
     * Channel 对象缓存工具类
     */
    private final ChannelProvider channelProvider;

    public NettyRpcClient() {
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 超过 15s 内如果没有向服务器写数据，会触发一个 IdleState#WRITE_IDLE 事件
                        ch.pipeline().addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS));
                        // 添加 粘包拆包 解码器
                        ch.pipeline().addLast(new RpcFrameDecoder());
                        // 添加 协议编解码器
                        ch.pipeline().addLast(new SharableRpcMessageCodec());
                        // 添加 rpc 响应消息处理器
                        ch.pipeline().addLast(new RpcResponseHandler());
                    }
                });
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    @SneakyThrows
    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        // 构建接收返回结果的 promise
        Promise<RpcMessage> promise;
        // 获取 Channel 对象
        Channel channel = getChannel(new InetSocketAddress(requestMetadata.getServerAddr(), requestMetadata.getPort()));
        if (channel.isActive()) {
            // 创建 promise 来接受结果         指定执行完成通知的线程
            promise = new DefaultPromise<>(channel.eventLoop());
            // 获取请求的序列号 ID
            int sequenceId = requestMetadata.getRpcMessage().getHeader().getSequenceId();
            // 存入还未处理的请求
            RpcResponseHandler.UNPROCESSED_RPC_RESPONSES.put(sequenceId, promise);
            // 发送数据并监听发送状态
            channel.writeAndFlush(requestMetadata.getRpcMessage()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.debug("The client send the message successfully, msg: [{}].", requestMetadata);
                } else {
                    future.channel().close();
                    promise.setFailure(future.cause());
                    log.error("The client send the message failed.", future.cause());
                }
            });

            Integer timeout = requestMetadata.getTimeout();

            // 等待结果返回（让出cpu资源，同步阻塞调用线程main，其他线程去执行获取操作（eventLoop））
            // 如果没有指定超时时间，则 await 直到 promise 完成
            if (timeout == null || timeout <= 0) {
                promise.await();
            } else {
                // 在指定超时时间内等待结果返回
                boolean success = promise.await(requestMetadata.getTimeout(), TimeUnit.MILLISECONDS);
                if (!success) {
                    promise.setFailure(new TimeoutException(String.format("The Remote procedure call exceeded the " +
                            "specified timeout of %dms.", timeout)));
                }
            }
            if (promise.isSuccess()) {
                // 返回响应结果
                return promise.getNow();
            } else {
                throw new RpcException(promise.cause());
            }
        } else {
            throw new IllegalStateException("The channel is inactivate.");
        }
    }

    /**
     * 连接到服务器获取 channel 对象
     *
     * @param inetSocketAddress 服务端地址
     * @return channel 对象
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        // 方式一，不用打印提示信息可以使用的方案
        // sync() 同步等待异步connect连接成功
//        Channel channel = bootstrap.connect(inetSocketAddress).sync().channel();
        // 设置同步等待异步关闭完成
//        channel.closeFuture().sync();

        // 方式二，打印提示信息使用方案
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("The client has successfully connected to server [{}]!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new RpcException(String.format("The client failed to connect to [%s].", inetSocketAddress.toString()));
            }
        });
        // 等待 future 完成返回结果
        Channel channel = completableFuture.get();
        // 添加异步关闭之后的操作
        channel.closeFuture().addListener(future -> {
            log.info("The client has been disconnected from server [{}].", inetSocketAddress.toString());
        });
        return channel;
    }

    /**
     * 获取 Channel
     *
     * @param inetSocketAddress 通信地址
     * @return 返回对应的 channel 对象
     */
    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
