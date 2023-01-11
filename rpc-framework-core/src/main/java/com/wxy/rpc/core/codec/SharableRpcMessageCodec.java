package com.wxy.rpc.core.codec;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.RpcResponse;
import com.wxy.rpc.core.constant.ProtocolConstants;
import com.wxy.rpc.core.enums.MessageType;
import com.wxy.rpc.core.enums.SerializationType;
import com.wxy.rpc.core.protocol.MessageHeader;
import com.wxy.rpc.core.protocol.RpcMessage;
import com.wxy.rpc.core.serialization.Serialization;
import com.wxy.rpc.core.serialization.SerializationFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.Arrays;
import java.util.List;

/**
 * 可共享的 Rpc 消息编码解码器，使用此编解码器必须配合 {@link com.wxy.rpc.core.codec.RpcFrameDecoder} 进行使用，
 * 以保证得到完整的数据包。不同于 {@link io.netty.handler.codec.ByteToMessageCodec} 的编解码器，共享编解码器无需
 * 保存 ByteBuf 的状态信息。
 * <p>
 * 消息协议：
 * <pre>
 *   --------------------------------------------------------------------
 *  | 魔数 (4byte) | 版本号 (1byte)  | 序列化算法 (1byte) | 消息类型 (1byte) |
 *  -------------------------------------------------------------------
 *  |    状态类型 (1byte)  |    消息序列号 (4byte)   |    消息长度 (4byte)   |
 *  --------------------------------------------------------------------
 *  |                        消息内容 (不固定长度)                         |
 *  -------------------------------------------------------------------
 * </pre>
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName SharableRpcMessageCodec
 * @Date 2023/1/4 23:51
 * @see io.netty.handler.codec.MessageToMessageCodec
 * @see io.netty.channel.ChannelInboundHandlerAdapter
 * @see io.netty.channel.ChannelOutboundHandlerAdapter
 */
@Sharable
public class SharableRpcMessageCodec extends MessageToMessageCodec<ByteBuf, RpcMessage> {

    // 编码器为出站处理，将 RpcMessage 编码为 ByteBuf 对象
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.alloc().buffer();
        MessageHeader header = msg.getHeader();
        // 4字节 魔数
        buf.writeBytes(header.getMagicNum());
        // 1字节 版本号
        buf.writeByte(header.getVersion());
        // 1字节 序列化算法
        buf.writeByte(header.getSerializerType());
        // 1字节 消息类型
        buf.writeByte(header.getMessageType());
        // 1字节 消息状态
        buf.writeByte(header.getMessageStatus());
        // 4字节 消息序列号
        buf.writeInt(header.getSequenceId());

        // 取出消息体
        Object body = msg.getBody();
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

    // 解码器为入站处理，将 ByteBuf 对象解码成 RpcMessage 对象
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // 4字节 魔数
        int len = ProtocolConstants.MAGIC_NUM.length;
        byte[] magicNum = new byte[len];
        msg.readBytes(magicNum, 0, len);
        // 判断魔数是否正确，不正确表示非协议请求，不进行处理
        for (int i = 0; i < len; i++) {
            if (magicNum[i] != ProtocolConstants.MAGIC_NUM[i]) {
                throw new IllegalArgumentException("Unknown magic code: " + Arrays.toString(magicNum));
            }
        }

        // 1字节 版本号
        byte version = msg.readByte();
        // 检查版本号是否一致
        if (version != ProtocolConstants.VERSION) {
            throw new IllegalArgumentException("The version isn't compatible " + version);
        }

        // 1字节 序列化算法
        byte serializeType = msg.readByte();
        // 1字节 消息类型
        byte messageType = msg.readByte();
        // 1字节 消息状态
        byte messageStatus = msg.readByte();
        // 4字节 消息序列号
        int sequenceId = msg.readInt();
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
                .messageStatus(messageStatus)
                .length(length).build();

        // 获取反序列化算法
        Serialization serialization = SerializationFactory
                .getSerialization(SerializationType.parseByType(serializeType));
        // 获取消息枚举类型
        MessageType type = MessageType.parseByType(messageType);
        RpcMessage protocol = new RpcMessage();
        protocol.setHeader(header);
        if (type == MessageType.REQUEST) {
            // 进行反序列化
            RpcRequest request = serialization.deserialize(RpcRequest.class, bytes);
            protocol.setBody(request);
        } else if (type == MessageType.RESPONSE) {
            // 进行反序列化
            RpcResponse response = serialization.deserialize(RpcResponse.class, bytes);
            protocol.setBody(response);
        } else if (type == MessageType.HEARTBEAT_REQUEST || type == MessageType.HEARTBEAT_RESPONSE) {
            String message = serialization.deserialize(String.class, bytes);
            protocol.setBody(message);
        }
        // 传递到下一个处理器
        out.add(protocol);
    }
}
