package com.specflow.api.modules.family.infrastructure.persistence.converter;

import com.specflow.api.modules.family.domain.entity.Family;
import com.specflow.api.modules.family.infrastructure.persistence.FamilyDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FamilyConverter 单元测试
 *
 * <p>测试策略：
 * - 测试 DO ↔ Entity 双向转换
 * - 验证时间字段的 UTC 时区处理
 * - 测试 null 值处理
 */
@DisplayName("FamilyConverter 单元测试")
class FamilyConverterTest {

    private static final String TEST_ID = "family-123";
    private static final String TEST_NAME = "Test Family";
    private static final String TEST_OWNER_ID = "user-456";

    // ==================== toDomain() 测试 ====================

    @Test
    @DisplayName("toDomain() - 完整 DO 应正确转换为 Entity")
    void toDomain_withCompleteDO_shouldConvertToEntity() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        FamilyDO familyDO = new FamilyDO();
        familyDO.setId(TEST_ID);
        familyDO.setName(TEST_NAME);
        familyDO.setOwnerId(TEST_OWNER_ID);
        familyDO.setCreatedAt(now);
        familyDO.setUpdatedAt(now);

        // When
        Family result = FamilyConverter.toDomain(familyDO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID);
        assertThat(result.getName()).isEqualTo(TEST_NAME);
        assertThat(result.getOwnerId()).isEqualTo(TEST_OWNER_ID);

        Instant expectedInstant = now.atZone(ZoneId.of("UTC")).toInstant();
        assertThat(result.getCreatedAt()).isEqualTo(expectedInstant);
        assertThat(result.getUpdatedAt()).isEqualTo(expectedInstant);
    }

    @Test
    @DisplayName("toDomain() - null 输入应返回 null")
    void toDomain_withNull_shouldReturnNull() {
        // When
        Family result = FamilyConverter.toDomain(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDomain() - 部分字段为 null 应正确处理")
    void toDomain_withNullFields_shouldHandleGracefully() {
        // Given
        FamilyDO familyDO = new FamilyDO();
        familyDO.setId(TEST_ID);
        familyDO.setName(null);
        familyDO.setOwnerId(null);
        familyDO.setCreatedAt(null);
        familyDO.setUpdatedAt(null);

        // When
        Family result = FamilyConverter.toDomain(familyDO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID);
        assertThat(result.getName()).isNull();
        assertThat(result.getOwnerId()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("toDomain() - 时间应使用 UTC 时区转换")
    void toDomain_timeConversion_shouldUseUtc() {
        // Given
        LocalDateTime localDateTime = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        FamilyDO familyDO = new FamilyDO();
        familyDO.setId(TEST_ID);
        familyDO.setName(TEST_NAME);
        familyDO.setOwnerId(TEST_OWNER_ID);
        familyDO.setCreatedAt(localDateTime);
        familyDO.setUpdatedAt(localDateTime);

        // When
        Family result = FamilyConverter.toDomain(familyDO);

        // Then
        Instant expected = localDateTime.atZone(ZoneId.of("UTC")).toInstant();
        assertThat(result.getCreatedAt()).isEqualTo(expected);
        assertThat(result.getUpdatedAt()).isEqualTo(expected);
    }

    // ==================== toDataObject() 测试 ====================

    @Test
    @DisplayName("toDataObject() - 完整 Entity 应正确转换为 DO")
    void toDataObject_withCompleteEntity_shouldConvertToDO() {
        // Given
        Instant now = Instant.parse("2024-01-15T10:30:00Z");
        Family family = new Family();
        family.setId(TEST_ID);
        family.setName(TEST_NAME);
        family.setOwnerId(TEST_OWNER_ID);
        family.setCreatedAt(now);
        family.setUpdatedAt(now);

        // When
        FamilyDO result = FamilyConverter.toDataObject(family);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID);
        assertThat(result.getName()).isEqualTo(TEST_NAME);
        assertThat(result.getOwnerId()).isEqualTo(TEST_OWNER_ID);

        LocalDateTime expectedDateTime = LocalDateTime.ofInstant(now, ZoneId.of("UTC"));
        assertThat(result.getCreatedAt()).isEqualTo(expectedDateTime);
        assertThat(result.getUpdatedAt()).isEqualTo(expectedDateTime);
    }

    @Test
    @DisplayName("toDataObject() - null 输入应返回 null")
    void toDataObject_withNull_shouldReturnNull() {
        // When
        FamilyDO result = FamilyConverter.toDataObject(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 部分字段为 null 应正确处理")
    void toDataObject_withNullFields_shouldHandleGracefully() {
        // Given
        Family family = new Family();
        family.setId(TEST_ID);
        family.setName(null);
        family.setOwnerId(null);
        family.setCreatedAt(null);
        family.setUpdatedAt(null);

        // When
        FamilyDO result = FamilyConverter.toDataObject(family);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID);
        assertThat(result.getName()).isNull();
        assertThat(result.getOwnerId()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 时间应使用 UTC 时区转换")
    void toDataObject_timeConversion_shouldUseUtc() {
        // Given
        Instant instant = Instant.parse("2024-06-15T12:00:00Z");
        Family family = new Family();
        family.setId(TEST_ID);
        family.setName(TEST_NAME);
        family.setOwnerId(TEST_OWNER_ID);
        family.setCreatedAt(instant);
        family.setUpdatedAt(instant);

        // When
        FamilyDO result = FamilyConverter.toDataObject(family);

        // Then
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        assertThat(result.getCreatedAt()).isEqualTo(expected);
        assertThat(result.getUpdatedAt()).isEqualTo(expected);
    }

    // ==================== 双向转换一致性测试 ====================

    @Test
    @DisplayName("双向转换 - toDomain 后再 toDataObject 应保持一致性")
    void roundTrip_shouldMaintainConsistency() {
        // Given
        LocalDateTime now = LocalDateTime.of(2024, 3, 20, 15, 45, 30);
        FamilyDO original = new FamilyDO();
        original.setId(TEST_ID);
        original.setName(TEST_NAME);
        original.setOwnerId(TEST_OWNER_ID);
        original.setCreatedAt(now);
        original.setUpdatedAt(now);

        // When
        Family entity = FamilyConverter.toDomain(original);
        FamilyDO result = FamilyConverter.toDataObject(entity);

        // Then
        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getOwnerId()).isEqualTo(original.getOwnerId());
        assertThat(result.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }

    @Test
    @DisplayName("双向转换 - toDataObject 后再 toDomain 应保持一致性")
    void roundTripReverse_shouldMaintainConsistency() {
        // Given
        Instant now = Instant.parse("2024-03-20T15:45:30Z");
        Family original = new Family();
        original.setId(TEST_ID);
        original.setName(TEST_NAME);
        original.setOwnerId(TEST_OWNER_ID);
        original.setCreatedAt(now);
        original.setUpdatedAt(now);

        // When
        FamilyDO dataObject = FamilyConverter.toDataObject(original);
        Family result = FamilyConverter.toDomain(dataObject);

        // Then
        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getOwnerId()).isEqualTo(original.getOwnerId());
        assertThat(result.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }
}
