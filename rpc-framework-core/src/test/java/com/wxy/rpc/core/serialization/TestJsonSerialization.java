package com.wxy.rpc.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName TestJsonSerialization
 * @Date 2023/1/6 16:03
 */
public class TestJsonSerialization {

    public static void main(String[] args) {
        Gson gson = new Gson();
        String hello = "hello";

        // 序列化对象
        String toJson = gson.toJson(hello);
        System.out.println(toJson);

        // 反序列化对象
        String json = gson.fromJson(toJson, String.class);
        System.out.println(json);

        gson = new GsonBuilder().disableInnerClassSerialization().create();
        // 序列化 Java Class
        String json1 = gson.toJson(String.class);
        System.out.println(json1);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class User {
        private String username;

        private Date date;
    }
}
