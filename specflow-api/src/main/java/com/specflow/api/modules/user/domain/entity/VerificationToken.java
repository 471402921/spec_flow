package com.specflow.api.modules.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * VerificationToken 领域实体（纯 POJO，无框架依赖）
 *
 * <p>职责：
 * - 维护验证令牌的业务规则
 * - 提供领域行为（验证有效性、标记已使用等）
 * - 工厂方法（create）
 *
 * <p>令牌类型：
 * - EMAIL_VERIFICATION: 邮箱验证
 * - PASSWORD_RESET: 密码重置
 * - EMAIL_CHANGE: 邮箱修改
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    /**
     * 令牌 ID（UUID）
     */
    private String id;

    /**
     * 令牌值（用于 URL 中的 token）
     */
    private String token;

    /**
     * 关联用户 ID
     */
    private String userId;

    /**
     * 令牌类型
     */
    private Type type;

    /**
     * 目标邮箱（用于邮箱修改时存储新邮箱）
     */
    private String email;

    /**
     * 是否已使用
     */
    private boolean used;

    /**
     * 过期时间
     */
    private Instant expiredAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 令牌类型枚举
     */
    public enum Type {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        EMAIL_CHANGE
    }

    // ==================== 领域行为 ====================

    /**
     * 检查令牌是否有效（未使用且未过期）
     *
     * @return true 如果令牌有效
     */
    public boolean isValid() {
        return !used && Instant.now().isBefore(expiredAt);
    }

    /**
     * 标记令牌为已使用
     */
    public void markAsUsed() {
        this.used = true;
        this.updatedAt = Instant.now();
    }

    /**
     * 检查是否在指定时间间隔内（用于频率限制）
     *
     * @param seconds 时间间隔（秒）
     * @return true 如果在指定间隔内
     */
    public boolean isWithinSeconds(int seconds) {
        return Instant.now().isBefore(this.createdAt.plusSeconds(seconds));
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新的验证令牌
     *
     * @param userId 用户 ID
     * @param type 令牌类型
     * @param email 目标邮箱（可为 null）
     * @param expireSeconds 过期时间（秒）
     * @return VerificationToken 实体
     */
    public static VerificationToken create(String userId, Type type, String email, long expireSeconds) {
        Instant now = Instant.now();
        String tokenValue = generateTokenValue();

        return new VerificationToken(
                UUID.randomUUID().toString(),
                tokenValue,
                userId,
                type,
                email,
                false,
                now.plusSeconds(expireSeconds),
                now,
                now
        );
    }

    /**
     * 生成 URL-safe 的随机令牌
     *
     * @return 随机令牌字符串
     */
    private static String generateTokenValue() {
        // 使用 32 字符集生成 32 位随机字符串
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(32);
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
