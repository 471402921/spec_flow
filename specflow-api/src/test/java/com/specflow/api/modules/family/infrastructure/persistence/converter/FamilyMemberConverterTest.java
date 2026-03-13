package com.specflow.api.modules.family.infrastructure.persistence.converter;

import com.specflow.api.modules.family.domain.entity.FamilyMember;
import com.specflow.api.modules.family.infrastructure.persistence.FamilyMemberDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FamilyMemberConverter 单元测试
 *
 * <p>测试策略：
 * - 测试 DO ↔ Entity 双向转换
 * - 验证 FamilyRole 枚举与 String 的转换
 * - 验证时间字段的 UTC 时区处理
 * - 测试 null 值处理
 */
@DisplayName("FamilyMemberConverter 单元测试")
class FamilyMemberConverterTest {

    private static final String TEST_MEMBER_ID = "member-123";
    private static final String TEST_FAMILY_ID = "family-456";
    private static final String TEST_USER_ID = "user-789";

    // ==================== toDomain() 测试 ====================

    @Test
    @DisplayName("toDomain() - OWNER 角色的 DO 应正确转换")
    void toDomain_withOwnerRole_shouldConvertCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        FamilyMemberDO memberDO = new FamilyMemberDO();
        memberDO.setId(TEST_MEMBER_ID);
        memberDO.setFamilyId(TEST_FAMILY_ID);
        memberDO.setUserId(TEST_USER_ID);
        memberDO.setRole("OWNER");
        memberDO.setJoinedAt(now);
        memberDO.setCreatedAt(now);
        memberDO.setUpdatedAt(now);

        // When
        FamilyMember result = FamilyMemberConverter.toDomain(memberDO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_MEMBER_ID);
        assertThat(result.getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getRole()).isEqualTo(FamilyMember.FamilyRole.OWNER);
        assertThat(result.isOwner()).isTrue();

        Instant expectedInstant = now.atZone(ZoneId.of("UTC")).toInstant();
        assertThat(result.getJoinedAt()).isEqualTo(expectedInstant);
    }

    @Test
    @DisplayName("toDomain() - MEMBER 角色的 DO 应正确转换")
    void toDomain_withMemberRole_shouldConvertCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        FamilyMemberDO memberDO = new FamilyMemberDO();
        memberDO.setId(TEST_MEMBER_ID);
        memberDO.setFamilyId(TEST_FAMILY_ID);
        memberDO.setUserId(TEST_USER_ID);
        memberDO.setRole("MEMBER");
        memberDO.setJoinedAt(now);
        memberDO.setCreatedAt(now);
        memberDO.setUpdatedAt(now);

        // When
        FamilyMember result = FamilyMemberConverter.toDomain(memberDO);

        // Then
        assertThat(result.getRole()).isEqualTo(FamilyMember.FamilyRole.MEMBER);
        assertThat(result.isOwner()).isFalse();
    }

    @Test
    @DisplayName("toDomain() - null 输入应返回 null")
    void toDomain_withNull_shouldReturnNull() {
        // When
        FamilyMember result = FamilyMemberConverter.toDomain(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDomain() - role 为 null 时应返回 null 角色")
    void toDomain_withNullRole_shouldReturnNullRole() {
        // Given
        FamilyMemberDO memberDO = new FamilyMemberDO();
        memberDO.setId(TEST_MEMBER_ID);
        memberDO.setRole(null);

        // When
        FamilyMember result = FamilyMemberConverter.toDomain(memberDO);

        // Then
        assertThat(result.getRole()).isNull();
        assertThat(result.isOwner()).isFalse();
    }

    @Test
    @DisplayName("toDomain() - 无效 role 字符串应返回 null")
    void toDomain_withInvalidRole_shouldReturnNull() {
        // Given
        FamilyMemberDO memberDO = new FamilyMemberDO();
        memberDO.setId(TEST_MEMBER_ID);
        memberDO.setRole("INVALID_ROLE");

        // When
        FamilyMember result = FamilyMemberConverter.toDomain(memberDO);

        // Then
        assertThat(result.getRole()).isNull();
    }

    // ==================== toDataObject() 测试 ====================

    @Test
    @DisplayName("toDataObject() - OWNER 角色的 Entity 应正确转换")
    void toDataObject_withOwnerRole_shouldConvertCorrectly() {
        // Given
        Instant now = Instant.now();
        FamilyMember member = new FamilyMember();
        member.setId(TEST_MEMBER_ID);
        member.setFamilyId(TEST_FAMILY_ID);
        member.setUserId(TEST_USER_ID);
        member.setRole(FamilyMember.FamilyRole.OWNER);
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);

        // When
        FamilyMemberDO result = FamilyMemberConverter.toDataObject(member);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_MEMBER_ID);
        assertThat(result.getFamilyId()).isEqualTo(TEST_FAMILY_ID);
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getRole()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("toDataObject() - MEMBER 角色的 Entity 应正确转换")
    void toDataObject_withMemberRole_shouldConvertCorrectly() {
        // Given
        Instant now = Instant.now();
        FamilyMember member = new FamilyMember();
        member.setId(TEST_MEMBER_ID);
        member.setFamilyId(TEST_FAMILY_ID);
        member.setUserId(TEST_USER_ID);
        member.setRole(FamilyMember.FamilyRole.MEMBER);
        member.setJoinedAt(now);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);

        // When
        FamilyMemberDO result = FamilyMemberConverter.toDataObject(member);

        // Then
        assertThat(result.getRole()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("toDataObject() - null 输入应返回 null")
    void toDataObject_withNull_shouldReturnNull() {
        // When
        FamilyMemberDO result = FamilyMemberConverter.toDataObject(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDataObject() - role 为 null 时应返回 null")
    void toDataObject_withNullRole_shouldReturnNullRole() {
        // Given
        FamilyMember member = new FamilyMember();
        member.setId(TEST_MEMBER_ID);
        member.setRole(null);

        // When
        FamilyMemberDO result = FamilyMemberConverter.toDataObject(member);

        // Then
        assertThat(result.getRole()).isNull();
    }

    // ==================== 双向转换一致性测试 ====================

    @Test
    @DisplayName("双向转换 - OWNER 角色应保持一致性")
    void roundTrip_withOwnerRole_shouldMaintainConsistency() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 3, 20, 15, 45, 30);
        FamilyMemberDO original = new FamilyMemberDO();
        original.setId(TEST_MEMBER_ID);
        original.setFamilyId(TEST_FAMILY_ID);
        original.setUserId(TEST_USER_ID);
        original.setRole("OWNER");
        original.setJoinedAt(now);
        original.setCreatedAt(now);
        original.setUpdatedAt(now);

        // When
        FamilyMember entity = FamilyMemberConverter.toDomain(original);
        FamilyMemberDO result = FamilyMemberConverter.toDataObject(entity);

        // Then
        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getFamilyId()).isEqualTo(original.getFamilyId());
        assertThat(result.getUserId()).isEqualTo(original.getUserId());
        assertThat(result.getRole()).isEqualTo(original.getRole());
        assertThat(result.getJoinedAt()).isEqualTo(original.getJoinedAt());
    }

    @Test
    @DisplayName("双向转换 - MEMBER 角色应保持一致性")
    void roundTrip_withMemberRole_shouldMaintainConsistency() {
        // Given
        Instant now = Instant.parse("2024-03-20T15:45:30Z");
        FamilyMember original = new FamilyMember();
        original.setId(TEST_MEMBER_ID);
        original.setFamilyId(TEST_FAMILY_ID);
        original.setUserId(TEST_USER_ID);
        original.setRole(FamilyMember.FamilyRole.MEMBER);
        original.setJoinedAt(now);
        original.setCreatedAt(now);
        original.setUpdatedAt(now);

        // When
        FamilyMemberDO dataObject = FamilyMemberConverter.toDataObject(original);
        FamilyMember result = FamilyMemberConverter.toDomain(dataObject);

        // Then
        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getRole()).isEqualTo(original.getRole());
        assertThat(result.getJoinedAt()).isEqualTo(original.getJoinedAt());
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("toDomain() - 空字符串 role 应返回 null")
    void toDomain_withEmptyRole_shouldReturnNull() {
        // Given
        FamilyMemberDO memberDO = new FamilyMemberDO();
        memberDO.setId(TEST_MEMBER_ID);
        memberDO.setRole("");

        // When
        FamilyMember result = FamilyMemberConverter.toDomain(memberDO);

        // Then
        assertThat(result.getRole()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 所有字段为 null 时应正常处理")
    void toDataObject_withAllNullFields_shouldHandleGracefully() {
        // Given
        FamilyMember member = new FamilyMember();

        // When
        FamilyMemberDO result = FamilyMemberConverter.toDataObject(member);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getFamilyId()).isNull();
        assertThat(result.getUserId()).isNull();
        assertThat(result.getRole()).isNull();
        assertThat(result.getJoinedAt()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }
}
