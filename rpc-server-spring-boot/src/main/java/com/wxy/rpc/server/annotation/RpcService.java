package com.wxy.rpc.server.annotation;

import java.lang.annotation.*;

/**
 * Rpc Service 注解，标注该类为服务实现类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName RpcService
 * @Date 2023/1/6 17:15
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RpcService {

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
