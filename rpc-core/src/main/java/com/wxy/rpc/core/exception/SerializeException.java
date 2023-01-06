package com.wxy.rpc.core.exception;

/**
 * @author Wuxy
 * @version 1.0
 * @ClassName SerializeException
 * @Date 2023/1/5 16:03
 */
public class SerializeException extends RuntimeException {

    private static final long serialVersionUID = 3365624081242234232L;

    public SerializeException() {
        super();
    }

    public SerializeException(String msg) {
        super(msg);
    }

    public SerializeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }

}
