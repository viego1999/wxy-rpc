package com.wxy.rpc.core.extension;

/**
 * Extension 工厂类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName ExtensionFactory
 * @Date 2023/1/11 22:30
 */
@SPI
public interface ExtensionFactory {

    /**
     * 得到扩展对象实例
     *
     * @param type 对象类型
     * @param name 对象名称
     * @param <T>  实例类型
     * @return 返回对象实例
     */
    <T> T getExtension(Class<?> type, String name);

}
