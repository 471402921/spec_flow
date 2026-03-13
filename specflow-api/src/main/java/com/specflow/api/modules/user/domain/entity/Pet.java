package com.specflow.api.modules.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pet 领域实体（纯 POJO，无框架依赖）
 *
 * <p>职责：
 * - 维护宠物的业务规则
 * - 提供领域行为（软删除、恢复、编辑等）
 * - 工厂方法（create）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

    /**
     * 宠物 ID（UUID）
     */
    private String id;

    /**
     * 主人 ID
     */
    private String ownerId;

    /**
     * 名字
     */
    private String name;

    /**
     * 种类（DOG/CAT）
     */
    private Species species;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别（MALE/FEMALE）
     */
    private Gender gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 软删除标记
     */
    private boolean deleted;

    /**
     * 删除时间
     */
    private Instant deletedAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 宠物种类枚举
     */
    public enum Species {
        DOG, CAT
    }

    /**
     * 宠物性别枚举
     */
    public enum Gender {
        MALE, FEMALE
    }

    // ==================== 领域行为 ====================

    /**
     * 编辑宠物信息
     */
    public void update(String name, Species species, String breed,
                       Gender gender, LocalDate birthday, String avatarUrl) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.gender = gender;
        this.birthday = birthday;
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * 软删除宠物
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * 恢复已删除的宠物
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.updatedAt = Instant.now();
    }

    /**
     * 检查宠物是否属于指定用户
     */
    public boolean belongsTo(String userId) {
        return this.ownerId.equals(userId);
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新宠物
     *
     * @param ownerId 主人 ID
     * @param name 名字
     * @param species 种类
     * @param breed 品种
     * @param gender 性别
     * @param birthday 生日（可为 null）
     * @param avatarUrl 头像 URL（可为 null）
     * @return Pet 实体
     */
    public static Pet create(String ownerId, String name, Species species,
                             String breed, Gender gender, LocalDate birthday, String avatarUrl) {
        Instant now = Instant.now();
        return new Pet(
                UUID.randomUUID().toString(),
                ownerId,
                name,
                species,
                breed,
                gender,
                birthday,
                avatarUrl,
                false,
                null,
                now,
                now
        );
    }
}
