package com.wxy.rpc.client.annotaition;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.*;

/**
 * RPC 引用注解，自动注入对应的实现类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcReference
 * @Date 2023/1/6 17:22
 * @see Autowired
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Autowired
public @interface RpcReference {

    /**
     * 对外暴露服务的接口类型，默认为 void.class
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 对外暴露服务的接口名（全限定名），默认为 ""
     */
    String interfaceName() default "";

    /**
     * 版本号，默认 1.0
     */
    String version() default "1.0";

}
