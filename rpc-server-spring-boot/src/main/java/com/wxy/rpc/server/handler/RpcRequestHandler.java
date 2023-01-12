package com.wxy.rpc.server.handler;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.RpcResponse;
import com.wxy.rpc.core.constant.ProtocolConstants;
import com.wxy.rpc.core.enums.MessageStatus;
import com.wxy.rpc.core.enums.MessageType;
import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.core.protocol.MessageHeader;
import com.wxy.rpc.core.protocol.RpcMessage;
import com.wxy.rpc.server.store.LocalServiceCache;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Rpc 请求消息处理器
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcRequestHandler
 * @Date 2023/1/6 19:42
 */
@Slf4j
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));

    @SuppressWarnings("Duplicates")
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        threadPool.submit(() -> {
            try {
                RpcMessage responseRpcMessage = new RpcMessage();
                MessageHeader header = msg.getHeader();
                MessageType type = MessageType.parseByType(header.getMessageType());
                log.info("The message received by the server is: {}", msg.getBody());
                // 如果是心跳检测请求信息
                if (type == MessageType.HEARTBEAT_REQUEST) {
                    header.setMessageType(MessageType.HEARTBEAT_RESPONSE.getType());
                    header.setMessageStatus(MessageStatus.SUCCESS.getCode());
                    // 设置响应头部信息
                    responseRpcMessage.setHeader(header);
                    responseRpcMessage.setBody(ProtocolConstants.PONG);
                } else { // 处理 Rpc 请求信息
                    RpcRequest request = (RpcRequest) msg.getBody();
                    RpcResponse response = new RpcResponse();
                    // 设置头部消息类型
                    header.setMessageType(MessageType.RESPONSE.getType());
                    // 反射调用
                    try {
                        // 获取请求服务实例
                        Object service = LocalServiceCache.getService(request.getServiceName());
                        if (service == null) {
                            log.error("The service [{}] is not exist!", request.getServiceName());
                            throw new RpcException(String.format("The service [%s] is not exist!", request.getServiceName()));
                        }
                        Method method = service.getClass().getMethod(request.getMethod(), request.getParameterTypes());
                        Object result = method.invoke(service, request.getParameterValues());
                        response.setReturnValue(result);
                        header.setMessageStatus(MessageStatus.SUCCESS.getCode());
                    } catch (Exception e) {
                        log.error("The service [{}], the method [{}] invoke failed!", request.getServiceName(), request.getMethod());
                        // 若不设置，堆栈信息过多，导致报错
                        response.setExceptionValue(new RpcException("Error in remote procedure call, " + e.getMessage()));
                        header.setMessageStatus(MessageStatus.FAIL.getCode());
                    }
                    // 设置响应头部信息
                    responseRpcMessage.setHeader(header);
                    responseRpcMessage.setBody(response);
                }
                log.debug("responseRpcMessage: {}.", responseRpcMessage);
                // 将结果写入，传递到下一个处理器
                ctx.writeAndFlush(responseRpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } finally {
                // 确保 ByteBuf 被释放，防止发生内存泄露
                ReferenceCountUtil.release(msg);
            }
        });
    }

    /**
     * 用户自定义事件，当触发读空闲时，自动关闭【客户端channel】连接
     *
     * @param ctx ctx
     * @param evt evt
     * @throws Exception exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection.");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }

}
