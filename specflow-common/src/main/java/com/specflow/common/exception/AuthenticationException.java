package com.specflow.common.exception;

/**
 * 认证失败异常
 *
 * 当用户身份验证失败（例如 token 无效、过期、已撤销）时抛出此异常
 * HTTP 状态码：401 UNAUTHORIZED
 */
public class AuthenticationException extends BusinessException {

    private static final String DEFAULT_CODE = "AUTHENTICATION_FAILED";

    public AuthenticationException(String message) {
        super(DEFAULT_CODE, message);
    }

    public AuthenticationException(String code, String message) {
        super(code, message);
    }

    public AuthenticationException(String message, Object data) {
        super(DEFAULT_CODE, message, data);
    }

    public AuthenticationException(String code, String message, Object data) {
        super(code, message, data);
    }
}
