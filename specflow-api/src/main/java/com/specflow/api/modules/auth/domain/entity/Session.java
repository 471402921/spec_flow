package com.specflow.api.modules.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Session 领域实体（纯 POJO，无框架依赖）
 *
 * 职责：
 * - 维护会话的业务规则
 * - 提供领域行为（isExpired、isValid、revoke）
 * - 工厂方法（create）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    /**
     * 会话 ID（UUID）
     */
    private String id;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 会话令牌
     */
    private String token;

    /**
     * 过期时间
     */
    private Instant expiredAt;

    /**
     * 是否已撤销
     */
    private boolean revoked;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    // ==================== 领域行为 ====================

    /**
     * 检查会话是否已过期
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiredAt);
    }

    /**
     * 检查会话是否有效（未过期且未撤销）
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * 撤销会话
     */
    public void revoke() {
        this.revoked = true;
        this.updatedAt = Instant.now();
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新会话
     *
     * @param userId         用户 ID
     * @param token          会话令牌
     * @param expirationDays 有效天数
     * @return Session 实体
     */
    public static Session create(String userId, String token, int expirationDays) {
        Instant now = Instant.now();
        return new Session(
                UUID.randomUUID().toString(),
                userId,
                token,
                now.plusSeconds(expirationDays * 24L * 60 * 60),
                false,
                now,
                now
        );
    }
}
