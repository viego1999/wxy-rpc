package com.wxy.rpc.core.enums;

import lombok.Getter;

/**
 * 消息状态类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName MessageStatus
 * @Date 2023/1/10 0:14
 */
public enum MessageStatus {

    /**
     * 成功
     */
    SUCCESS((byte) 0),

    /**
     * 失败
     */
    FAIL((byte) 1);

    @Getter
    private final byte code;

    MessageStatus(byte code) {
        this.code = code;
    }

    public static boolean isSuccess(byte code) {
        return MessageStatus.SUCCESS.code == code;
    }

}
