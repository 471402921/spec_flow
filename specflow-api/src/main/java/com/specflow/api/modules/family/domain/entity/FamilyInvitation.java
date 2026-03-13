package com.specflow.api.modules.family.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * FamilyInvitation 领域实体（家庭邀请码）
 *
 * <p>职责：
 * - 维护家庭邀请码信息
 * - 提供邀请码验证逻辑
 * - 支持撤销邀请码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInvitation {

    /**
     * 邀请码 ID（UUID）
     */
    private String id;

    /**
     * 家庭 ID
     */
    private String familyId;

    /**
     * 邀请码（8位大写字母数字，不含0/O/I/1）
     */
    private String code;

    /**
     * 创建者 ID（家庭主人）
     */
    private String createdBy;

    /**
     * 是否已撤销
     */
    private boolean revoked;

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

    // ==================== 领域行为 ====================

    /**
     * 撤销邀请码
     */
    public void revoke() {
        this.revoked = true;
        this.updatedAt = Instant.now();
    }

    /**
     * 检查邀请码是否有效（未撤销且未过期）
     *
     * @return true 表示有效
     */
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiredAt);
    }

    /**
     * 检查邀请码是否已过期
     *
     * @return true 表示已过期
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiredAt);
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新的邀请码
     *
     * @param familyId 家庭 ID
     * @param code 邀请码（8位字符）
     * @param createdBy 创建者 ID
     * @param validDays 有效期天数
     * @return FamilyInvitation 实体
     */
    public static FamilyInvitation create(String familyId, String code, String createdBy, int validDays) {
        Instant now = Instant.now();
        return new FamilyInvitation(
                UUID.randomUUID().toString(),
                familyId,
                code.toUpperCase(),
                createdBy,
                false,
                now.plusSeconds(validDays * 24L * 60L * 60L),
                now,
                now
        );
    }
}
