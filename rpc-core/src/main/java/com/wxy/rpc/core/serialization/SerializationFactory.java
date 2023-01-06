package com.wxy.rpc.core.serialization;

import com.wxy.rpc.core.enums.SerializationType;
import com.wxy.rpc.core.serialization.hessian.HessianSerialization;
import com.wxy.rpc.core.serialization.jdk.JdkSerialization;
import com.wxy.rpc.core.serialization.json.JsonSerialization;

/**
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
            default:
                throw new IllegalArgumentException("serialization type is illegal.");
        }
    }

}
