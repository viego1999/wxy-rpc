package com.wxy.rpc.core.serialization;

import com.wxy.rpc.core.extension.SPI;

@SPI
public interface Serialization {

    /**
     * 将传入对象进行序列化
     *
     * @param object 需要被序列化的对象
     * @param <T>    对象类型
     * @return 返回序列化后的字节数组
     */
    <T> byte[] serialize(T object);

    /**
     * 将对象进行反序列化
     *
     * @param clazz 对象的类型
     * @param bytes 对象字节数组
     * @param <T>   对象类型
     * @return 返回序列化后的对象
     */
    <T> T deserialize(Class<T> clazz, byte[] bytes);

}
