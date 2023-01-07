package com.wxy.rpc.core.constant;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 协议常量类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName ProtocolConstant
 * @Date 2023/1/5 17:32
 */
public class ProtocolConstants {

    private static final AtomicInteger ai = new AtomicInteger();

    public static final short MAGIC_NUM = 0x00;

    public static final byte VERSION = 0x01;

    public static final String PING = "ping";

    public static final String PONG = "pong";

    public static int getSequenceId() {
        // todo: 实现原子操作
        return ai.getAndIncrement();
    }

}
