package com.wxy.rpc.core.protocol;

import lombok.Data;

/**
 * 消息协议类
 *
 * @param <T> 类型参数，具体的消息类型
 * @author Wuxy
 * @version 1.0
 * @Date 2023/1/4
 */
@Data
public class MessageProtocol<T> {

    /**
     * 请求头
     */
    private MessageHeader header;

    /**
     * 消息体
     */
    private T body;

}
