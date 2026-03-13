package com.specflow.api.modules.family.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Family 领域实体（家庭）
 *
 * <p>职责：
 * - 维护家庭的基本信息和业务规则
 * - 提供领域行为（修改名称、转让主人等）
 * - 工厂方法（create）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Family {

    /**
     * 家庭 ID（UUID）
     */
    private String id;

    /**
     * 家庭名称（2-20字符）
     */
    private String name;

    /**
     * 当前家庭主人 ID（冗余字段，便于快速查询）
     */
    private String ownerId;

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
     * 修改家庭名称
     *
     * @param newName 新名称
     */
    public void updateName(String newName) {
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    /**
     * 转让家庭主人身份
     *
     * @param newOwnerId 新主人 ID
     */
    public void transferOwnership(String newOwnerId) {
        this.ownerId = newOwnerId;
        this.updatedAt = Instant.now();
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新家庭
     *
     * @param name 家庭名称
     * @param ownerId 创建者 ID（自动成为主人）
     * @return Family 实体
     */
    public static Family create(String name, String ownerId) {
        Instant now = Instant.now();
        return new Family(
                UUID.randomUUID().toString(),
                name,
                ownerId,
                now,
                now
        );
    }
}
