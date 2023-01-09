package com.wxy.rpc.core.util;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName ServiceUtil
 * @Date 2023/1/5 20:57
 */
public class ServiceUtil {

    /**
     * 根据 服务名称 + 版本号 生成注册服务的 key
     *
     * @param serverName 服务名
     * @param version    版本号
     * @return 生成最终的服务名称: serverName-version
     */
    public static String serviceKey(String serverName, String version) {

        return String.join("-", serverName, version);
    }
}
