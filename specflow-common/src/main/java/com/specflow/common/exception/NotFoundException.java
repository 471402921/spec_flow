package com.specflow.common.exception;

/**
 * 资源未找到异常
 *
 * 当查询或操作的资源不存在时抛出此异常
 * HTTP 状态码：404 NOT FOUND
 */
public class NotFoundException extends BusinessException {

    private static final String DEFAULT_CODE = "NOT_FOUND";

    public NotFoundException(String message) {
        super(DEFAULT_CODE, message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }

    public NotFoundException(String message, Object data) {
        super(DEFAULT_CODE, message, data);
    }

    public NotFoundException(String code, String message, Object data) {
        super(code, message, data);
    }
}
