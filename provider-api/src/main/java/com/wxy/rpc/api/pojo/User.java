package com.wxy.rpc.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName User
 * @Date 2023/1/8 23:41
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private String username;

    private String password;

    private Integer age;
}
