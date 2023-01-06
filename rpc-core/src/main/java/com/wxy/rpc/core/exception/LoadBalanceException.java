package com.wxy.rpc.core.exception;

/**
 * 负载均衡异常类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName LoadBalanceException
 * @Date 2023/1/5 16:39
 */
public class LoadBalanceException extends RuntimeException {

    private static final long serialVersionUID = 3365624081242234230L;

    public LoadBalanceException() {
        super();
    }

    public LoadBalanceException(String msg) {
        super(msg);
    }

    public LoadBalanceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public LoadBalanceException(Throwable cause) {
        super(cause);
    }

}
