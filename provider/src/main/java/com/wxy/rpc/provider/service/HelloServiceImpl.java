package com.wxy.rpc.provider.service;

import com.wxy.rpc.api.service.HelloService;

public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
