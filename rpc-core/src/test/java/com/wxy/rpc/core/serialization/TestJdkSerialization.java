package com.wxy.rpc.core.serialization;

import com.wxy.rpc.core.codec.RpcFrameDecoder;
import com.wxy.rpc.core.codec.SharableRpcMessageCodec;
import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.enums.SerializationType;
import com.wxy.rpc.core.protocol.MessageHeader;
import com.wxy.rpc.core.protocol.MessageProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestJdkSerialization
 * @Date 2023/1/5 17:22
 */
public class TestJdkSerialization {

    public static void main(String[] args) {
        SharableRpcMessageCodec<RpcRequest> REQUEST_CODE = new SharableRpcMessageCodec<>();
        LoggingHandler LOGGING = new LoggingHandler(LogLevel.DEBUG);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(LOGGING, new RpcFrameDecoder(), REQUEST_CODE, LOGGING);

        RpcRequest request = new RpcRequest();
        request.setServerName("com.wxy.rpc.api.service.HelloService");
        request.setMethod("sayHello");
        request.setParameterTypes(new Class[]{String.class});
        request.setParameterValues(new Object[]{"zhangsan"});

        MessageProtocol<RpcRequest> protocol = new MessageProtocol<>();
        MessageHeader header = MessageHeader.build("JDK");
        protocol.setHeader(header);
        protocol.setBody(request);

//        embeddedChannel.writeOutbound(protocol); // encode

        ByteBuf buf = embeddedChannel.alloc().buffer();
        buf.writeShort(header.getMagicNum());
        buf.writeByte(header.getVersion());
        buf.writeByte(header.getSerializerType());
        buf.writeByte(header.getMessageType());
        buf.writeInt(header.getSequenceId());
        buf.writeByte(header.getPadding());
        Serialization serialization = SerializationFactory
                .getSerialization(SerializationType.parseByType(header.getSerializerType()));
        byte[] bytes = serialization.serialize(request);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
        embeddedChannel.writeInbound(buf); // decode
    }

}
