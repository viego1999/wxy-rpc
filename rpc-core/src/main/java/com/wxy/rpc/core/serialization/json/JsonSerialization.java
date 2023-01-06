package com.wxy.rpc.core.serialization.json;

import com.wxy.rpc.core.serialization.Serialization;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName JsonSerialization
 * @Date 2023/1/5 12:23
 */
public class JsonSerialization implements Serialization {
    @Override
    public <T> byte[] serialize(T object) {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return null;
    }
}
