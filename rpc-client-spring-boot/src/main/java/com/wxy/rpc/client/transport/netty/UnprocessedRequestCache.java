package com.wxy.rpc.client.transport.netty;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 缓存未处理完成的 rpc 请求
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName UnprocessedRequestCache
 * @Date 2023/1/7 12:18
 */
public class UnprocessedRequestCache {

    private static Map<Integer, CompletableFuture<?>> m;

}
