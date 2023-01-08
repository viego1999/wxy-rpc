package com.wxy.rpc.consumer.controller;

import com.wxy.rpc.api.service.HelloService;
import com.wxy.rpc.client.annotaition.RpcReference;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName HelloController
 * @Date 2023/1/8 10:12
 */
@RestController
@RequestMapping
public class HelloController {

    @RpcReference
    private HelloService helloService;

    @RequestMapping("/hello/{name}")
    public String hello(@PathVariable("name") String name) {

        return helloService.sayHello(name);
    }
}
