package com.specflow.common.exception;

import lombok.Getter;

/**
 * 业务异常基类
 *
 * 用于表示可预期的业务逻辑错误，这些错误应该被全局异常处理器捕获并转换为用户友好的错误响应
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码（例如：USER_NOT_FOUND、INVALID_TOKEN 等）
     */
    private final String code;

    /**
     * 附加数据（可选，用于提供更多上下文信息）
     */
    private final Object data;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    public BusinessException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = null;
    }

    public BusinessException(String code, String message, Object data, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = data;
    }
}
