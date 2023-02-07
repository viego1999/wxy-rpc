package com.wxy.rpc.provider.service.impl;

import com.wxy.rpc.api.service.AbstractService;
import com.wxy.rpc.server.annotation.RpcService;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName AbstractServiceImpl
 * @since 2023/2/7 9:57
 */
@RpcService(interfaceClass = AbstractService.class)
public class AbstractServiceImpl extends AbstractService {
    @Override
    public String abstractHello(String name) {
        return "abstract hello " + name;
    }
}
