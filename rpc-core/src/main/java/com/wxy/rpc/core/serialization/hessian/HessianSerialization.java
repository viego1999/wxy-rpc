package com.wxy.rpc.core.serialization.hessian;

import com.wxy.rpc.core.serialization.Serialization;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName HessianSerialization
 * @Date 2023/1/5 12:24
 */
public class HessianSerialization implements Serialization {
    @Override
    public <T> byte[] serialize(T object) {
        return new byte[0];
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return null;
    }
}
