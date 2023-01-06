package com.wxy.rpc.core.codec;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.RpcResponse;
import com.wxy.rpc.core.enums.MessageType;
import com.wxy.rpc.core.protocol.MessageHeader;
import com.wxy.rpc.core.protocol.MessageProtocol;
import com.wxy.rpc.core.serialization.Serialization;
import com.wxy.rpc.core.serialization.SerializationFactory;
import com.wxy.rpc.core.enums.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

/**
 * 可共享的 Rpc 消息编码解码器，使用此编解码器必须配合 {@link com.wxy.rpc.core.codec.RpcFrameDecoder} 进行使用，
 * 以保证得到完整的数据包。不同于 {@link io.netty.handler.codec.ByteToMessageCodec} 的编解码器，共享编解码器无需
 * 保存 ByteBuf 的状态信息。
 * <p>
 * 消息协议：
 * <pre>
 *   ------------------------------------------------------------------
 *  | 魔数 (2byte) | 版本号 (1byte) | 序列化算法 (1byte) | 消息类型 (1byte) |
 *  -------------------------------------------------------------------
 *  |   消息序列号 (4byte)   |   对齐填充符 (1byte)   |   消息长度 (4byte)  |
 *  -------------------------------------------------------------------
 *  |                        消息内容 (不固定长度)                        |
 *  -------------------------------------------------------------------
 * </pre>
 *
 * @param <T> 类型参数，具体的消息体类型
 * @author Wuxy
 * @version 1.0
 * @ClassName SharableRpcMessageCodec
 * @Date 2023/1/4 23:51
 * @see io.netty.handler.codec.MessageToMessageCodec
 * @see io.netty.channel.ChannelInboundHandlerAdapter
 * @see io.netty.channel.ChannelOutboundHandlerAdapter
 */
@Sharable
public class SharableRpcMessageCodec<T> extends MessageToMessageCodec<ByteBuf, MessageProtocol<T>> {

    // 编码器为出站处理
    @Override
    protected void encode(ChannelHandlerContext ctx, MessageProtocol<T> msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.alloc().buffer();
        MessageHeader header = msg.getHeader();
        // 2字节 魔数
        buf.writeShort(header.getMagicNum());
        // 1字节 版本号
        buf.writeByte(header.getVersion());
        // 1字节 序列化算法
        buf.writeByte(header.getSerializerType());
        // 1字节 消息类型
        buf.writeByte(header.getMessageType());
        // 4字节 消息序列号
        buf.writeInt(header.getSequenceId());
        // 1字节 填充符号
        buf.writeByte(header.getPadding());

        // 取出消息体
        T body = msg.getBody();
        // 获取序列化算法
        Serialization serialization = SerializationFactory
                .getSerialization(SerializationType.parseByType(header.getSerializerType()));
        // 进行序列化
        byte[] bytes = serialization.serialize(body);
        // 设置消息体长度
        header.setLength(bytes.length);

        // 4字节 消息内容长度
        buf.writeInt(header.getLength());

        // 不固定字节 消息内容字节数组
        buf.writeBytes(bytes);

        // 传递到下一个出站处理器
        out.add(buf);
    }

    // 解码器为入站处理
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // 2字节 魔数
        short magicNum = msg.readShort();
        // 1字节 版本号
        byte version = msg.readByte();
        // 1字节 序列化算法
        byte serializeType = msg.readByte();
        // 1字节 消息类型
        byte messageType = msg.readByte();
        // 4字节 消息序列号
        int sequenceId = msg.readInt();
        // 1字节 填充符号
        byte padding = msg.readByte();
        // 4字节 长度
        int length = msg.readInt();

        byte[] bytes = new byte[length];
        msg.readBytes(bytes, 0, length);

        // 构建协议头部信息
        MessageHeader header = MessageHeader.builder()
                .magicNum(magicNum)
                .version(version)
                .serializerType(serializeType)
                .messageType(messageType)
                .sequenceId(sequenceId)
                .padding(padding)
                .length(length).build();

        // 获取反序列化算法
        Serialization serialization = SerializationFactory
                .getSerialization(SerializationType.parseByType(serializeType));
        // 获取消息枚举类型
        MessageType type = MessageType.parseByType(messageType);

        switch (type) {
            case REQUEST:
                // 进行反序列化
                RpcRequest request = serialization.deserialize(RpcRequest.class, bytes);
                if (request != null) {
                    MessageProtocol<RpcRequest> protocol = new MessageProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(request);
                    // 传递到下一个入站处理器
                    out.add(protocol);
                }
                break;
            case RESPONSE:
                // 进行反序列化
                RpcResponse response = serialization.deserialize(RpcResponse.class, bytes);
                if (response != null) {
                    MessageProtocol<RpcResponse> protocol = new MessageProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(response);
                    // 传递到下一个入站处理器
                    out.add(protocol);
                }
                break;
        }
    }
}
