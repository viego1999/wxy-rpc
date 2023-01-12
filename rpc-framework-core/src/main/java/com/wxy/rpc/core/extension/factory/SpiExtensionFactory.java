package com.wxy.rpc.core.extension.factory;

import com.wxy.rpc.core.extension.ExtensionFactory;
import com.wxy.rpc.core.extension.ExtensionLoader;
import com.wxy.rpc.core.extension.SPI;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName SpiExtensionFactory
 * @Date 2023/1/11 22:33
 */
public class SpiExtensionFactory implements ExtensionFactory {
    @Override
    public <T> T getExtension(Class<?> type, String name) {
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(type);
            // todo: implement this method
        }
        return null;
    }
}
