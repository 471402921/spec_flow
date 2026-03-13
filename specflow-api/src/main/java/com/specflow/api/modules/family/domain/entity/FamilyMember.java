package com.specflow.api.modules.family.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * FamilyMember 领域实体（家庭成员关系）
 *
 * <p>职责：
 * - 维护用户与家庭的多对多关系
 * - 记录成员在家庭中的角色
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMember {

    /**
     * 成员关系 ID（UUID）
     */
    private String id;

    /**
     * 家庭 ID
     */
    private String familyId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 角色：OWNER（主人）或 MEMBER（成员）
     */
    private FamilyRole role;

    /**
     * 加入时间
     */
    private Instant joinedAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 家庭成员角色枚举
     */
    public enum FamilyRole {
        OWNER,
        MEMBER
    }

    // ==================== 领域行为 ====================

    /**
     * 转让主人身份给当前成员
     */
    public void promoteToOwner() {
        this.role = FamilyRole.OWNER;
        this.updatedAt = Instant.now();
    }

    /**
     * 降级为普通成员
     */
    public void demoteToMember() {
        this.role = FamilyRole.MEMBER;
        this.updatedAt = Instant.now();
    }

    /**
     * 检查是否是主人
     */
    public boolean isOwner() {
        return this.role == FamilyRole.OWNER;
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建家庭主人记录
     *
     * @param familyId 家庭 ID
     * @param userId 用户 ID
     * @return FamilyMember 实体（角色为 OWNER）
     */
    public static FamilyMember createOwner(String familyId, String userId) {
        Instant now = Instant.now();
        return new FamilyMember(
                UUID.randomUUID().toString(),
                familyId,
                userId,
                FamilyRole.OWNER,
                now,
                now,
                now
        );
    }

    /**
     * 创建普通成员记录
     *
     * @param familyId 家庭 ID
     * @param userId 用户 ID
     * @return FamilyMember 实体（角色为 MEMBER）
     */
    public static FamilyMember createMember(String familyId, String userId) {
        Instant now = Instant.now();
        return new FamilyMember(
                UUID.randomUUID().toString(),
                familyId,
                userId,
                FamilyRole.MEMBER,
                now,
                now,
                now
        );
    }
}
