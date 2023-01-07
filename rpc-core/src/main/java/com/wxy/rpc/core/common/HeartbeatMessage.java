package com.wxy.rpc.core.common;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 心跳检查消息类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName HeartbeatMessage
 * @Date 2023/1/7 18:31
 */
@Data
@Builder
public class HeartbeatMessage implements Serializable {

    /**
     * 消息
     */
    private String msg;

}
