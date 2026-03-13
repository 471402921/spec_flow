package com.specflow.api.modules.user.domain.service;

/**
 * Token 提供者接口
 *
 * <p>职责：
 * - 定义用户认证令牌的创建、验证和撤销操作
 * - 由 Auth 模块提供具体实现（SessionTokenProvider）
 *
 * <p>设计意图：
 * - User 模块（核心域）定义接口
 * - Auth 模块（支撑域）提供实现
 * - 避免 User 模块直接依赖 Auth 模块的具体类
 */
public interface TokenProvider {

    /**
     * 创建新的认证令牌
     *
     * @param userId 用户 ID
     * @return 生成的令牌字符串
     */
    String createToken(String userId);

    /**
     * 撤销指定的令牌
     *
     * @param token 令牌字符串
     */
    void revokeToken(String token);

    /**
     * 验证令牌是否有效
     *
     * @param token 令牌字符串
     * @return 有效返回 true，无效返回 false
     */
    boolean validateToken(String token);

    /**
     * 根据令牌获取用户 ID
     *
     * @param token 令牌字符串
     * @return 用户 ID，令牌无效时返回 null
     */
    String getUserIdByToken(String token);

    /**
     * 根据令牌获取过期时间
     *
     * @param token 令牌字符串
     * @return 过期时间（Instant），令牌无效时返回 null
     */
    java.time.Instant getExpiredAtByToken(String token);
}
