package com.wxy.rpc.core.serialization;

import com.wxy.rpc.core.codec.RpcFrameDecoder;
import com.wxy.rpc.core.codec.SharableRpcMessageCodec;
import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.enums.SerializationType;
import com.wxy.rpc.core.protocol.MessageHeader;
import com.wxy.rpc.core.protocol.RpcMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.UnknownHostException;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestJdkSerialization
 * @Date 2023/1/5 17:22
 */
public class TestSerialization {

    public static void main(String[] args) throws UnknownHostException {
        SharableRpcMessageCodec REQUEST_CODE = new SharableRpcMessageCodec();
        LoggingHandler LOGGING = new LoggingHandler(LogLevel.DEBUG);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(LOGGING, new RpcFrameDecoder(), REQUEST_CODE, LOGGING);

        RpcRequest request = new RpcRequest();
        request.setServiceName("com.wxy.rpc.api.service.HelloService");
        request.setMethod("sayHello");
        request.setParameterTypes(new Class[]{String.class});
        request.setParameterValues(new Object[]{"zhangsan"});

        RpcMessage protocol = new RpcMessage();
        MessageHeader header = MessageHeader.build("PROTOSTUFF");
        protocol.setHeader(header);
        protocol.setBody(request);

//        embeddedChannel.writeOutbound(protocol); // encode

        ByteBuf buf = embeddedChannel.alloc().buffer();
        buf.writeBytes(header.getMagicNum());
        buf.writeByte(header.getVersion());
        buf.writeByte(header.getSerializerType());
        buf.writeByte(header.getMessageType());
        buf.writeByte(header.getMessageStatus());
        buf.writeInt(header.getSequenceId());
        Serialization serialization = SerializationFactory
                .getSerialization(SerializationType.parseByType(header.getSerializerType()));
        byte[] bytes = serialization.serialize(request);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
        /*
         * JDK          369B
         * JSON         156B
         * HESSIAN      226B
         * KRYO         99B
         * PROTOSTUFF   144B
         */
        embeddedChannel.writeInbound(buf); // decode
    }

}
