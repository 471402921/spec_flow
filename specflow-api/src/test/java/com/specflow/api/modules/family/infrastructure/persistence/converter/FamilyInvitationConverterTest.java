package com.specflow.api.modules.family.infrastructure.persistence.converter;

import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import com.specflow.api.modules.family.infrastructure.persistence.FamilyInvitationDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FamilyInvitationConverter 单元测试
 *
 * <p>测试策略：
 * - 测试 DO ↔ Entity 双向转换
 * - 验证 revoked 布尔值与 Boolean 的转换
 * - 验证时间字段的 UTC 时区处理
 * - 测试 null 值处理
 */
@DisplayName("FamilyInvitationConverter 单元测试")
class FamilyInvitationConverterTest {

    private static final String TEST_INVITATION_ID = "invitation-123";
    private static final String TEST_FAMILY_ID = "family-456";
    private static final String TEST_CODE = "ABC12345";
    private static final String TEST_CREATED_BY = "user-789";

    // ==================== toDomain() 测试 ====================

    @Test
    @DisplayName("toDomain() - 完整 DO 应正确转换为 Entity")
    void toDomain_withCompleteDO_shouldConvertToEntity() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LocalDateTime expired = now.plusDays(7);

        FamilyInvitationDO invitationDO = new FamilyInvitationDO();
        invitationDO.setId(TEST_INVITATION_ID);
        invitationDO.setFamilyId(TEST_FAMILY_ID);
        invitationDO.setCode(TEST_CODE);
        invitationDO.setCreatedBy(TEST_CREATED_BY);
        invitationDO.setRevoked(false);
        invitationDO.setExpiredAt(expired);
        invitationDO.setCreatedAt(now);
        invitationDO.setUpdatedAt(now);

        // When
        FamilyInvitation result = FamilyInvitationConverter.toDomain(invitationDO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_INVITATION_ID);
        assertThat(result.getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.getCode()).isEqualTo(TEST_CODE);
        assertThat(result.getCreatedBy()).isEqualTo(TEST_CREATED_BY);
        assertThat(result.isRevoked()).isFalse();

        Instant expectedNow = now.atZone(ZoneId.of("UTC")).toInstant();
        Instant expectedExpired = expired.atZone(ZoneId.of("UTC")).toInstant();
        assertThat(result.getCreatedAt()).isEqualTo(expectedNow);
        assertThat(result.getExpiredAt()).isEqualTo(expectedExpired);
    }

    @Test
    @DisplayName("toDomain() - revoked = true 时应正确转换")
    void toDomain_withRevokedTrue_shouldConvertCorrectly() {
        // Given
        FamilyInvitationDO invitationDO = new FamilyInvitationDO();
        invitationDO.setId(TEST_INVITATION_ID);
        invitationDO.setRevoked(true);

        // When
        FamilyInvitation result = FamilyInvitationConverter.toDomain(invitationDO);

        // Then
        assertThat(result.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("toDomain() - revoked = null 时应视为 false")
    void toDomain_withRevokedNull_shouldTreatAsFalse() {
        // Given
        FamilyInvitationDO invitationDO = new FamilyInvitationDO();
        invitationDO.setId(TEST_INVITATION_ID);
        invitationDO.setRevoked(null);

        // When
        FamilyInvitation result = FamilyInvitationConverter.toDomain(invitationDO);

        // Then
        assertThat(result.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("toDomain() - null 输入应返回 null")
    void toDomain_withNull_shouldReturnNull() {
        // When
        FamilyInvitation result = FamilyInvitationConverter.toDomain(null);

        // Then
        assertThat(result).isNull();
    }

    // ==================== toDataObject() 测试 ====================

    @Test
    @DisplayName("toDataObject() - 完整 Entity 应正确转换为 DO")
    void toDataObject_withCompleteEntity_shouldConvertToDO() {
        // Given
        Instant now = Instant.parse("2024-01-15T10:30:00Z");
        Instant expired = now.plusSeconds(7 * 24 * 60 * 60); // 7 days later

        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setId(TEST_INVITATION_ID);
        invitation.setFamilyId(TEST_FAMILY_ID);
        invitation.setCode(TEST_CODE);
        invitation.setCreatedBy(TEST_CREATED_BY);
        invitation.setRevoked(false);
        invitation.setExpiredAt(expired);
        invitation.setCreatedAt(now);
        invitation.setUpdatedAt(now);

        // When
        FamilyInvitationDO result = FamilyInvitationConverter.toDataObject(invitation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_INVITATION_ID);
        assertThat(result.getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.getCode()).isEqualTo(TEST_CODE);
        assertThat(result.getCreatedBy()).isEqualTo(TEST_CREATED_BY);
        assertThat(result.getRevoked()).isFalse();

        LocalDateTime expectedNow = LocalDateTime.ofInstant(now, ZoneId.of("UTC"));
        LocalDateTime expectedExpired = LocalDateTime.ofInstant(expired, ZoneId.of("UTC"));
        assertThat(result.getCreatedAt()).isEqualTo(expectedNow);
        assertThat(result.getExpiredAt()).isEqualTo(expectedExpired);
    }

    @Test
    @DisplayName("toDataObject() - revoked = true 时应转换为 Boolean.TRUE")
    void toDataObject_withRevokedTrue_shouldConvertToBooleanTrue() {
        // Given
        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setId(TEST_INVITATION_ID);
        invitation.setRevoked(true);

        // When
        FamilyInvitationDO result = FamilyInvitationConverter.toDataObject(invitation);

        // Then
        assertThat(result.getRevoked()).isTrue();
    }

    @Test
    @DisplayName("toDataObject() - revoked = false 时应转换为 Boolean.FALSE")
    void toDataObject_withRevokedFalse_shouldConvertToBooleanFalse() {
        // Given
        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setId(TEST_INVITATION_ID);
        invitation.setRevoked(false);

        // When
        FamilyInvitationDO result = FamilyInvitationConverter.toDataObject(invitation);

        // Then
        assertThat(result.getRevoked()).isFalse();
    }

    @Test
    @DisplayName("toDataObject() - null 输入应返回 null")
    void toDataObject_withNull_shouldReturnNull() {
        // When
        FamilyInvitationDO result = FamilyInvitationConverter.toDataObject(null);

        // Then
        assertThat(result).isNull();
    }

    // ==================== 双向转换一致性测试 ====================

    @Test
    @DisplayName("双向转换 - 完整数据应保持一致性")
    void roundTrip_shouldMaintainConsistency() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 3, 20, 15, 45, 30);
        LocalDateTime expired = now.plusDays(7);

        FamilyInvitationDO original = new FamilyInvitationDO();
        original.setId(TEST_INVITATION_ID);
        original.setFamilyId(TEST_FAMILY_ID);
        original.setCode(TEST_CODE);
        original.setCreatedBy(TEST_CREATED_BY);
        original.setRevoked(true);
        original.setExpiredAt(expired);
        original.setCreatedAt(now);
        original.setUpdatedAt(now);

        // When
        FamilyInvitation entity = FamilyInvitationConverter.toDomain(original);
        FamilyInvitationDO result = FamilyInvitationConverter.toDataObject(entity);

        // Then
        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getFamilyId()).isEqualTo(original.getFamilyId());
        assertThat(result.getCode()).isEqualTo(original.getCode());
        assertThat(result.getCreatedBy()).isEqualTo(original.getCreatedBy());
        assertThat(result.getRevoked()).isEqualTo(original.getRevoked());
        assertThat(result.getExpiredAt()).isEqualTo(original.getExpiredAt());
        assertThat(result.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }

    @Test
    @DisplayName("双向转换 - revoked 为 null 时应保持一致性")
    void roundTrip_withRevokedNull_shouldMaintainConsistency() {
        // Given
        FamilyInvitationDO original = new FamilyInvitationDO();
        original.setId(TEST_INVITATION_ID);
        original.setRevoked(null);
        original.setCreatedAt(LocalDateTime.now());
        original.setUpdatedAt(LocalDateTime.now());
        original.setExpiredAt(LocalDateTime.now().plusDays(7));

        // When
        FamilyInvitation entity = FamilyInvitationConverter.toDomain(original);
        FamilyInvitationDO result = FamilyInvitationConverter.toDataObject(entity);

        // Then - toDomain 会将 null 视为 false，toDataObject 会转换为 Boolean.FALSE
        assertThat(entity.isRevoked()).isFalse();
        assertThat(result.getRevoked()).isFalse();
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("toDomain() - 所有时间字段为 null 时应正常处理")
    void toDomain_withNullTimeFields_shouldHandleGracefully() {
        // Given
        FamilyInvitationDO invitationDO = new FamilyInvitationDO();
        invitationDO.setId(TEST_INVITATION_ID);
        invitationDO.setExpiredAt(null);
        invitationDO.setCreatedAt(null);
        invitationDO.setUpdatedAt(null);

        // When
        FamilyInvitation result = FamilyInvitationConverter.toDomain(invitationDO);

        // Then
        assertThat(result.getExpiredAt()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 所有字段为 null 时应正常处理")
    void toDataObject_withAllNullFields_shouldHandleGracefully() {
        // Given
        FamilyInvitation invitation = new FamilyInvitation();

        // When
        FamilyInvitationDO result = FamilyInvitationConverter.toDataObject(invitation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getFamilyId()).isNull();
        assertThat(result.getCode()).isNull();
        assertThat(result.getCreatedBy()).isNull();
        assertThat(result.getRevoked()).isFalse(); // boolean 默认 false
        assertThat(result.getExpiredAt()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("转换验证 - 过期时间应正确转换")
    void conversion_expiredAt_shouldConvertCorrectly() {
        // Given
        LocalDateTime expired = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        FamilyInvitationDO invitationDO = new FamilyInvitationDO();
        invitationDO.setId(TEST_INVITATION_ID);
        invitationDO.setExpiredAt(expired);

        // When
        FamilyInvitation result = FamilyInvitationConverter.toDomain(invitationDO);

        // Then
        Instant expected = expired.atZone(ZoneId.of("UTC")).toInstant();
        assertThat(result.getExpiredAt()).isEqualTo(expected);
    }
}
