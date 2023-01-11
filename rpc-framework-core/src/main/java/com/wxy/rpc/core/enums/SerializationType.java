package com.wxy.rpc.core.enums;

import lombok.Getter;

/**
 * 序列化算法枚举类
 */
public enum SerializationType {

    /**
     * JDK 序列化算法
     */
    JDK((byte) 0),

    /**
     * JSON 序列化算法
     */
    JSON((byte) 1),

    /**
     * HESSIAN 序列化算法
     */
    HESSIAN((byte) 2),

    /**
     * KRYO 序列化算法
     */
    KRYO((byte) 3),

    /**
     * PROTOSTUFF 序列化算法
     */
    PROTOSTUFF((byte) 4);

    /**
     * 类型
     */
    @Getter
    private final byte type;

    SerializationType(byte type) {
        this.type = type;
    }

    public static SerializationType parseByName(String serializeName) {
        for (SerializationType serializationType : SerializationType.values()) {
            if (serializationType.name().equalsIgnoreCase(serializeName)) {
                return serializationType;
            }
        }
        return HESSIAN;
    }

    /**
     * 通过序列化类型获取序列化算法枚举类
     *
     * @param type 类型
     * @return 枚举类型
     */
    public static SerializationType parseByType(byte type) {
        for (SerializationType serializationType : SerializationType.values()) {
            if (serializationType.getType() == type) {
                return serializationType;
            }
        }
        return HESSIAN;
    }
}
