package com.specflow.api.config;

/**
 * Token 验证接口 - 框架基础设施
 *
 * <p>业务模块需实现此接口以提供 Token → userId 的解析能力，
 * 供 AuthInterceptor 使用。
 */
public interface TokenProvider {

    /**
     * 根据 Token 获取用户 ID
     *
     * @param token Bearer Token
     * @return userId，若 Token 无效或已过期返回 null
     */
    String getUserIdByToken(String token);
}
