package com.wxy.rpc.core.loadbalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * 测试一致性哈希算法
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName TestConsistentHashLoadBalance
 * @Date 2023/1/11 10:35
 */
public class TestConsistentHashLoadBalance {

    public static void main(String[] args) {
        String key = "com.wxy.rpc.api.service.HelloService";
        byte[] digest = md5(key);
        System.out.println(digest.length);
        System.out.println(Arrays.toString(digest));
        TreeSet<Long> hashs = new TreeSet<>();
        String address = "192.168.0.5";
        for (int i = 0; i < 160 / 4; i++) {
            digest = md5(address + i);
            for (int h = 0; h < 4; h++) {
                long hash = hash(digest, h);
                hashs.add(hash);
            }
        }
        Long first = hashs.first(), last = hashs.last();
        System.out.println("first: " + first + ", last: " + last);
    }

    public static byte[] md5(String key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
            md.update(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md.digest();
    }

    private static long hash(byte[] digest, int number) {
        return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                | (digest[number * 4] & 0xFF))
                & 0xFFFFFFFFL;
    }

}
