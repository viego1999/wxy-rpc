package com.wxy.rpc.core.extension;

import com.wxy.rpc.core.common.RpcRequest;
import com.wxy.rpc.core.common.ServiceInfo;
import com.wxy.rpc.core.loadbalance.LoadBalance;

import java.util.Arrays;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestExtensionLoader
 * @Date 2023/1/11 20:21
 */
public class TestExtensionLoader {

    public static void main(String[] args) {
        ExtensionLoader<LoadBalance> extensionLoader = ExtensionLoader.getExtensionLoader(LoadBalance.class);
        LoadBalance random = extensionLoader.getExtension("random");
        System.out.println(random.select(Arrays.asList(
                ServiceInfo.builder().port(1).build(),
                ServiceInfo.builder().port(2).build(),
                ServiceInfo.builder().port(3).build()), new RpcRequest()));
        System.out.println(random);
    }

}
