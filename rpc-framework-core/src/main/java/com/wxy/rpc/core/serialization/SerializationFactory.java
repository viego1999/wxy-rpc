package com.wxy.rpc.core.serialization;

import com.wxy.rpc.core.enums.SerializationType;
import com.wxy.rpc.core.serialization.hessian.HessianSerialization;
import com.wxy.rpc.core.serialization.jdk.JdkSerialization;
import com.wxy.rpc.core.serialization.json.JsonSerialization;
import com.wxy.rpc.core.serialization.kryo.KryoSerialization;
import com.wxy.rpc.core.serialization.protostuff.ProtostuffSerialization;

/**
 * 序列化算法工厂，通过序列化枚举类型获取相应的序列化算法实例
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName SerializationFactory
 * @Date 2023/1/5 12:21
 */
public class SerializationFactory {

    public static Serialization getSerialization(SerializationType enumType) {
        switch (enumType) {
            case JDK:
                return new JdkSerialization();
            case JSON:
                return new JsonSerialization();
            case HESSIAN:
                return new HessianSerialization();
            case KRYO:
                return new KryoSerialization();
            case PROTOSTUFF:
                return new ProtostuffSerialization();
            default:
                throw new IllegalArgumentException(String.format("The serialization type %s is illegal.",
                        enumType.name()));
        }
    }

}
