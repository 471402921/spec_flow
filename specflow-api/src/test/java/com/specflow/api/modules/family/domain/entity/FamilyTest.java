package com.specflow.api.modules.family.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Family 领域实体单元测试
 *
 * <p>测试范围：
 * - 工厂方法 create()
 * - 领域行为：updateName, transferOwnership
 * - 状态验证
 */
@DisplayName("Family 领域实体测试")
class FamilyTest {

    @Test
    @DisplayName("create() - 使用名称和主人 ID 创建家庭")
    void create_withNameAndOwner_shouldCreateFamily() {
        // Given
        String name = "My Family";
        String ownerId = "user-123";

        // When
        Family family = Family.create(name, ownerId);

        // Then
        assertThat(family).isNotNull();
        assertThat(family.getId()).isNotNull();
        assertThat(family.getName()).isEqualTo(name);
        assertThat(family.getOwnerId()).isEqualTo(ownerId);
        assertThat(family.getCreatedAt()).isNotNull();
        assertThat(family.getUpdatedAt()).isNotNull();
        assertThat(family.getCreatedAt()).isEqualTo(family.getUpdatedAt());
    }

    @Test
    @DisplayName("create() - 创建时应自动设置创建时间为当前时间")
    void create_shouldSetCurrentTimestamp() {
        // Given
        long beforeCreate = java.time.Instant.now().toEpochMilli();

        // When
        Family family = Family.create("Test Family", "user-123");

        // Then
        long afterCreate = java.time.Instant.now().toEpochMilli();
        long createdAtMillis = family.getCreatedAt().toEpochMilli();
        assertThat(createdAtMillis).isBetween(beforeCreate, afterCreate);
    }

    @Test
    @DisplayName("updateName() - 更新家庭名称")
    void updateName_withNewName_shouldUpdateNameAndTimestamp() {
        // Given
        Family family = Family.create("Old Name", "user-123");
        String newName = "New Family Name";

        // When
        family.updateName(newName);

        // Then
        assertThat(family.getName()).isEqualTo(newName);
        assertThat(family.getUpdatedAt()).isAfter(family.getCreatedAt());
    }

    @Test
    @DisplayName("updateName() - 更新名称后 updatedAt 应改变")
    void updateName_shouldChangeUpdatedAt() {
        // Given
        Family family = Family.create("Original", "user-123");
        java.time.Instant originalUpdatedAt = family.getUpdatedAt();

        // 确保时间有变化
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        family.updateName("Updated Name");

        // Then
        assertThat(family.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("transferOwnership() - 转让主人身份给新用户")
    void transferOwnership_withNewOwner_shouldUpdateOwnerId() {
        // Given
        Family family = Family.create("Test Family", "original-owner");
        String newOwnerId = "new-owner-456";

        // When
        family.transferOwnership(newOwnerId);

        // Then
        assertThat(family.getOwnerId()).isEqualTo(newOwnerId);
        assertThat(family.getUpdatedAt()).isAfter(family.getCreatedAt());
    }

    @Test
    @DisplayName("transferOwnership() - 转让后 updatedAt 应更新")
    void transferOwnership_shouldUpdateTimestamp() {
        // Given
        Family family = Family.create("Test Family", "owner-1");
        java.time.Instant originalUpdatedAt = family.getUpdatedAt();

        // 确保时间有变化
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        family.transferOwnership("owner-2");

        // Then
        assertThat(family.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("transferOwnership() - 转让给同一个主人 ID 也应更新时间")
    void transferOwnership_toSameOwner_shouldStillUpdateTimestamp() {
        // Given
        String sameOwnerId = "same-owner";
        Family family = Family.create("Test Family", sameOwnerId);
        java.time.Instant originalUpdatedAt = family.getUpdatedAt();

        // 确保时间有变化
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        family.transferOwnership(sameOwnerId);

        // Then
        assertThat(family.getOwnerId()).isEqualTo(sameOwnerId);
        assertThat(family.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("create() - 不同调用应生成不同的 UUID")
    void create_multipleCalls_shouldGenerateDifferentIds() {
        // When
        Family family1 = Family.create("Family 1", "user-1");
        Family family2 = Family.create("Family 2", "user-2");

        // Then
        assertThat(family1.getId()).isNotEqualTo(family2.getId());
    }

    @Test
    @DisplayName("getters/setters - Lombok 生成的 getter 和 setter 应正常工作")
    void gettersAndSetters_shouldWork() {
        // Given
        Family family = new Family();
        String id = "test-id";
        String name = "Test Family";
        String ownerId = "owner-123";
        java.time.Instant now = java.time.Instant.now();

        // When
        family.setId(id);
        family.setName(name);
        family.setOwnerId(ownerId);
        family.setCreatedAt(now);
        family.setUpdatedAt(now);

        // Then
        assertThat(family.getId()).isEqualTo(id);
        assertThat(family.getName()).isEqualTo(name);
        assertThat(family.getOwnerId()).isEqualTo(ownerId);
        assertThat(family.getCreatedAt()).isEqualTo(now);
        assertThat(family.getUpdatedAt()).isEqualTo(now);
    }
}
