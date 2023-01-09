package com.wxy.rpc.consumer.controller;

import com.wxy.rpc.api.pojo.User;
import com.wxy.rpc.api.service.UserService;
import com.wxy.rpc.client.annotation.RpcReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName UserController
 * @Date 2023/1/8 23:47
 */
@RestController
public class UserController {

    @RpcReference
    private UserService userService;


    @RequestMapping("/user/getUser")
    public User getUser() {

        return userService.queryUser();
    }

    @RequestMapping("/user/getAllUser")
    public List<User> getAllUser() {

        return userService.getAllUsers();
    }

}
