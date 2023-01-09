package com.wxy.rpc.core.common;

import lombok.Data;

import java.io.Serializable;

/**
 * Rpc 响应消息实体类
 */
@Data
public class RpcResponse implements Serializable {

    /**
     * 请求返回值
     */
    private Object returnValue;

    /**
     * 发生异常时的异常信息
     */
    private Exception exceptionValue;

}
