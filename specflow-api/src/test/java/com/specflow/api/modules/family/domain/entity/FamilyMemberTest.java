package com.specflow.api.modules.family.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FamilyMember 领域实体单元测试
 *
 * <p>测试范围：
 * - 工厂方法 createOwner(), createMember()
 * - 领域行为：promoteToOwner, demoteToMember, isOwner
 * - 状态验证
 */
@DisplayName("FamilyMember 领域实体测试")
class FamilyMemberTest {

    private static final String TEST_FAMILY_ID = "family-123";
    private static final String TEST_USER_ID = "user-456";

    @Test
    @DisplayName("createOwner() - 创建家庭主人")
    void createOwner_shouldCreateOwnerWithCorrectRole() {
        // When
        FamilyMember owner = FamilyMember.createOwner(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(owner).isNotNull();
        assertThat(owner.getId()).isNotNull();
        assertThat(owner.getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(owner.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(owner.getRole()).isEqualTo(FamilyMember.FamilyRole.OWNER);
        assertThat(owner.isOwner()).isTrue();
        assertThat(owner.getJoinedAt()).isNotNull();
        assertThat(owner.getCreatedAt()).isNotNull();
        assertThat(owner.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("createMember() - 创建普通成员")
    void createMember_shouldCreateMemberWithCorrectRole() {
        // When
        FamilyMember member = FamilyMember.createMember(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(member).isNotNull();
        assertThat(member.getId()).isNotNull();
        assertThat(member.getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(member.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(member.getRole()).isEqualTo(FamilyMember.FamilyRole.MEMBER);
        assertThat(member.isOwner()).isFalse();
        assertThat(member.getJoinedAt()).isNotNull();
        assertThat(member.getCreatedAt()).isNotNull();
        assertThat(member.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("createOwner() - 不同调用应生成不同的 UUID")
    void createOwner_multipleCalls_shouldGenerateDifferentIds() {
        // When
        FamilyMember owner1 = FamilyMember.createOwner(TEST_FAMILY_ID, "user-1");
        FamilyMember owner2 = FamilyMember.createOwner(TEST_FAMILY_ID, "user-2");

        // Then
        assertThat(owner1.getId()).isNotEqualTo(owner2.getId());
    }

    @Test
    @DisplayName("promoteToOwner() - 成员升级为主人")
    void promoteToOwner_shouldChangeRoleToOwner() {
        // Given
        FamilyMember member = FamilyMember.createMember(TEST_FAMILY_ID, TEST_USER_ID);
        assertThat(member.isOwner()).isFalse();
        java.time.Instant originalUpdatedAt = member.getUpdatedAt();

        // 确保时间有变化
        sleep(10);

        // When
        member.promoteToOwner();

        // Then
        assertThat(member.getRole()).isEqualTo(FamilyMember.FamilyRole.OWNER);
        assertThat(member.isOwner()).isTrue();
        assertThat(member.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("demoteToMember() - 主人降级为成员")
    void demoteToMember_shouldChangeRoleToMember() {
        // Given
        FamilyMember owner = FamilyMember.createOwner(TEST_FAMILY_ID, TEST_USER_ID);
        assertThat(owner.isOwner()).isTrue();
        java.time.Instant originalUpdatedAt = owner.getUpdatedAt();

        // 确保时间有变化
        sleep(10);

        // When
        owner.demoteToMember();

        // Then
        assertThat(owner.getRole()).isEqualTo(FamilyMember.FamilyRole.MEMBER);
        assertThat(owner.isOwner()).isFalse();
        assertThat(owner.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("isOwner() - 主人返回 true")
    void isOwner_withOwnerRole_shouldReturnTrue() {
        // Given
        FamilyMember owner = FamilyMember.createOwner(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(owner.isOwner()).isTrue();
    }

    @Test
    @DisplayName("isOwner() - 成员返回 false")
    void isOwner_withMemberRole_shouldReturnFalse() {
        // Given
        FamilyMember member = FamilyMember.createMember(TEST_FAMILY_ID, TEST_USER_ID);

        // Then
        assertThat(member.isOwner()).isFalse();
    }

    @Test
    @DisplayName("promoteToOwner() - 已经是主人再次升级也应更新时间")
    void promoteToOwner_alreadyOwner_shouldStillUpdateTimestamp() {
        // Given
        FamilyMember owner = FamilyMember.createOwner(TEST_FAMILY_ID, TEST_USER_ID);
        java.time.Instant originalUpdatedAt = owner.getUpdatedAt();

        // 确保时间有变化
        sleep(10);

        // When
        owner.promoteToOwner();

        // Then
        assertThat(owner.isOwner()).isTrue();
        assertThat(owner.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("demoteToMember() - 已经是成员再次降级也应更新时间")
    void demoteToMember_alreadyMember_shouldStillUpdateTimestamp() {
        // Given
        FamilyMember member = FamilyMember.createMember(TEST_FAMILY_ID, TEST_USER_ID);
        java.time.Instant originalUpdatedAt = member.getUpdatedAt();

        // 确保时间有变化
        sleep(10);

        // When
        member.demoteToMember();

        // Then
        assertThat(member.isOwner()).isFalse();
        assertThat(member.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("getters/setters - Lombok 生成的 getter 和 setter 应正常工作")
    void gettersAndSetters_shouldWork() {
        // Given
        FamilyMember member = new FamilyMember();
        String id = "test-id";
        String familyId = "family-123";
        String userId = "user-456";
        FamilyMember.FamilyRole role = FamilyMember.FamilyRole.OWNER;
        java.time.Instant now = java.time.Instant.now();

        // When
        member.setId(id);
        member.setFamilyId(familyId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);

        // Then
        assertThat(member.getId()).isEqualTo(id);
        assertThat(member.getFamilyId()).isEqualTo(familyId);
        assertThat(member.getUserId()).isEqualTo(userId);
        assertThat(member.getRole()).isEqualTo(role);
        assertThat(member.getJoinedAt()).isEqualTo(now);
        assertThat(member.getCreatedAt()).isEqualTo(now);
        assertThat(member.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("FamilyRole 枚举 - OWNER 和 MEMBER 应存在")
    void familyRoleEnum_shouldHaveOwnerAndMember() {
        // Then
        assertThat(FamilyMember.FamilyRole.values()).containsExactly(
                FamilyMember.FamilyRole.OWNER,
                FamilyMember.FamilyRole.MEMBER
        );
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
