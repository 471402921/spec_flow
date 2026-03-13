package com.specflow.api.util;

/**
 * 日志脱敏工具类
 *
 * <p>职责：
 * - 敏感信息脱敏处理（如 token、密码等）
 * - 统一日志脱敏规则
 */
public final class LogMasker {

    private LogMasker() {
        // 工具类禁止实例化
    }

    /**
     * 脱敏 Token（仅保留前16位）
     *
     * @param token 原始 token
     * @return 脱敏后的 token 字符串，如 "token_abc123..."
     */
    public static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "***";
        }
        if (token.length() <= 16) {
            return token + "***";
        }
        return token.substring(0, 16) + "***";
    }
}
