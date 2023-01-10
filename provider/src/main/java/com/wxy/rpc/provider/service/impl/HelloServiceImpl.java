package com.wxy.rpc.provider.service.impl;

import com.wxy.rpc.api.service.HelloService;
import com.wxy.rpc.server.annotation.RpcService;

@RpcService(interfaceClass = HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
