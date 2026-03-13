package com.specflow.api.modules.family.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FamilyInvitation 领域实体单元测试
 *
 * <p>测试范围：
 * - 工厂方法 create()
 * - 领域行为：revoke, isValid, isExpired
 * - 状态验证
 */
@DisplayName("FamilyInvitation 领域实体测试")
class FamilyInvitationTest {

    private static final String TEST_FAMILY_ID = "family-123";
    private static final String TEST_CODE = "ABC12345";
    private static final String TEST_CREATED_BY = "user-456";
    private static final int VALID_DAYS = 7;

    @Test
    @DisplayName("create() - 使用参数创建邀请码")
    void create_withParams_shouldCreateInvitation() {
        // When
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);

        // Then
        assertThat(invitation).isNotNull();
        assertThat(invitation.getId()).isNotNull();
        assertThat(invitation.getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(invitation.getCode()).isEqualTo(TEST_CODE.toUpperCase());
        assertThat(invitation.getCreatedBy()).isEqualTo(TEST_CREATED_BY);
        assertThat(invitation.isRevoked()).isFalse();
        assertThat(invitation.getCreatedAt()).isNotNull();
        assertThat(invitation.getUpdatedAt()).isNotNull();
        assertThat(invitation.getExpiredAt()).isNotNull();
    }

    @Test
    @DisplayName("create() - 邀请码应转为大写")
    void create_withLowerCaseCode_shouldConvertToUpperCase() {
        // Given
        String lowerCaseCode = "abc12345";

        // When
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, lowerCaseCode, TEST_CREATED_BY, VALID_DAYS);

        // Then
        assertThat(invitation.getCode()).isEqualTo("ABC12345");
    }

    @Test
    @DisplayName("create() - 过期时间应为当前时间加上有效期天数")
    void create_shouldSetCorrectExpirationTime() {
        // Given
        long beforeCreate = Instant.now().toEpochMilli();

        // When
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);

        // Then
        long afterCreate = Instant.now().toEpochMilli();
        long expectedMinExpiredAt = beforeCreate + (VALID_DAYS * 24L * 60 * 60 * 1000) - 1000; // 允许1秒误差
        long expectedMaxExpiredAt = afterCreate + (VALID_DAYS * 24L * 60 * 60 * 1000) + 1000;
        long actualExpiredAt = invitation.getExpiredAt().toEpochMilli();

        assertThat(actualExpiredAt).isBetween(expectedMinExpiredAt, expectedMaxExpiredAt);
    }

    @Test
    @DisplayName("create() - 有效期为 0 天时应立即过期")
    void create_withZeroValidDays_shouldExpireImmediately() {
        // When
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, 0);

        // Then
        assertThat(invitation.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isValid() - 未撤销且未过期时返回 true")
    void isValid_whenNotRevokedAndNotExpired_shouldReturnTrue() {
        // Given
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);

        // Then
        assertThat(invitation.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid() - 已撤销时返回 false")
    void isValid_whenRevoked_shouldReturnFalse() {
        // Given
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);

        // When
        invitation.revoke();

        // Then
        assertThat(invitation.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid() - 已过期时返回 false")
    void isValid_whenExpired_shouldReturnFalse() {
        // Given - 创建一个已经过期的邀请码
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, -1); // 昨天过期

        // Then
        assertThat(invitation.isValid()).isFalse();
    }

    @Test
    @DisplayName("isExpired() - 未过期时返回 false")
    void isExpired_whenNotExpired_shouldReturnFalse() {
        // Given
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);

        // Then
        assertThat(invitation.isExpired()).isFalse();
    }

    @Test
    @DisplayName("isExpired() - 已过期时返回 true")
    void isExpired_whenExpired_shouldReturnTrue() {
        // Given - 创建一个已经过期的邀请码
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, -1); // 昨天过期

        // Then
        assertThat(invitation.isExpired()).isTrue();
    }

    @Test
    @DisplayName("revoke() - 撤销邀请码")
    void revoke_shouldSetRevokedToTrue() {
        // Given
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);
        assertThat(invitation.isRevoked()).isFalse();
        Instant originalUpdatedAt = invitation.getUpdatedAt();

        // 确保时间有变化
        sleep(10);

        // When
        invitation.revoke();

        // Then
        assertThat(invitation.isRevoked()).isTrue();
        assertThat(invitation.isValid()).isFalse();
        assertThat(invitation.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("revoke() - 重复撤销应保持撤销状态")
    void revoke_twice_shouldRemainRevoked() {
        // Given
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);
        invitation.revoke();

        // When - 再次撤销
        invitation.revoke();

        // Then
        assertThat(invitation.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("isRevoked() - 已撤销返回 true")
    void isRevoked_whenRevoked_shouldReturnTrue() {
        // Given
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);
        invitation.revoke();

        // Then
        assertThat(invitation.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("isRevoked() - 未撤销返回 false")
    void isRevoked_whenNotRevoked_shouldReturnFalse() {
        // Given
        FamilyInvitation invitation = FamilyInvitation.create(
                TEST_FAMILY_ID, TEST_CODE, TEST_CREATED_BY, VALID_DAYS);

        // Then
        assertThat(invitation.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("create() - 不同调用应生成不同的 UUID")
    void create_multipleCalls_shouldGenerateDifferentIds() {
        // When
        FamilyInvitation invitation1 = FamilyInvitation.create(
                TEST_FAMILY_ID, "CODE0001", TEST_CREATED_BY, VALID_DAYS);
        FamilyInvitation invitation2 = FamilyInvitation.create(
                TEST_FAMILY_ID, "CODE0002", TEST_CREATED_BY, VALID_DAYS);

        // Then
        assertThat(invitation1.getId()).isNotEqualTo(invitation2.getId());
    }

    @Test
    @DisplayName("getters/setters - Lombok 生成的 getter 和 setter 应正常工作")
    void gettersAndSetters_shouldWork() {
        // Given
        FamilyInvitation invitation = new FamilyInvitation();
        String id = "test-id";
        String familyId = "family-123";
        String code = "TESTCODE";
        String createdBy = "user-456";
        boolean revoked = true;
        Instant now = Instant.now();

        // When
        invitation.setId(id);
        invitation.setFamilyId(familyId);
        invitation.setCode(code);
        invitation.setCreatedBy(createdBy);
        invitation.setRevoked(revoked);
        invitation.setExpiredAt(now);
        invitation.setCreatedAt(now);
        invitation.setUpdatedAt(now);

        // Then
        assertThat(invitation.getId()).isEqualTo(id);
        assertThat(invitation.getFamilyId()).isEqualTo(familyId);
        assertThat(invitation.getCode()).isEqualTo(code);
        assertThat(invitation.getCreatedBy()).isEqualTo(createdBy);
        assertThat(invitation.isRevoked()).isTrue();
        assertThat(invitation.getExpiredAt()).isEqualTo(now);
        assertThat(invitation.getCreatedAt()).isEqualTo(now);
        assertThat(invitation.getUpdatedAt()).isEqualTo(now);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
