package com.wxy.rpc.client.spring;

import com.wxy.rpc.client.annotation.RpcReference;
import com.wxy.rpc.client.proxy.ClientStubProxyFactory;
import com.wxy.rpc.core.exception.RpcException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * 客户端 Bean 后置处理器，主要用于扫描创建的 bean 中有被 @RpcReference 标注的域属性，获取对应的代理对象并进行替换
 *
 * @author Wuxy
 * @version 1.0
 * @see com.wxy.rpc.client.annotation.RpcReference
 */
public class RpcClientBeanPostProcessor implements BeanPostProcessor {

    private final ClientStubProxyFactory proxyFactory;

    public RpcClientBeanPostProcessor(ClientStubProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    /**
     * 在 bean 实例化完后，扫描 bean 中需要进行 rpc 注入的属性，将对应的属性使用 代理对象 进行替换
     *
     * @param bean     bean 对象
     * @param beanName bean 名称
     * @return 后置增强后的 bean 对象
     * @throws BeansException bean 异常
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取该 bean 的类的所有属性（getFields - 获取所有的public属性，getDeclaredFields - 获取所有声明的属性，不区分访问修饰符）
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历所有属性
        for (Field field : fields) {
            // 判断是否被 RpcReference 注解标注
            if (field.isAnnotationPresent(RpcReference.class)) {
                // 获得 RpcReference 注解
                RpcReference rpcReference = field.getAnnotation(RpcReference.class);
                // 默认类为属性当前类型
                // filed.class = java.lang.reflect.Field
                // filed.type = com.wxy.xxx.service.XxxService
                Class<?> clazz = field.getType();
                try {
                    // 如果指定了全限定类型接口名
                    if (!"".equals(rpcReference.interfaceName())) {
                        clazz = Class.forName(rpcReference.interfaceName());
                    }
                    // 如果指定了接口类型
                    if (rpcReference.interfaceClass() != void.class) {
                        clazz = rpcReference.interfaceClass();
                    }
                    // 获取指定类型的代理对象
                    Object proxy = proxyFactory.getProxy(clazz, rpcReference.version());
                    // 关闭安全检查
                    field.setAccessible(true);
                    // 设置域的值为代理对象
                    field.set(bean, proxy);
                } catch (ClassNotFoundException | IllegalAccessException e) {
                    throw new RpcException(String.format("Failed to obtain proxy object, the type of field %s is %s, " +
                            "and the specified loaded proxy type is %s.", field.getName(), field.getClass(), clazz), e);
                }
            }
        }
        return bean;
    }

}
