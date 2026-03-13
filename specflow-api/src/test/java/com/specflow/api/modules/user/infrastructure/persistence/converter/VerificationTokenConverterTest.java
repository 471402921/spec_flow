package com.specflow.api.modules.user.infrastructure.persistence.converter;

import com.specflow.api.modules.user.domain.entity.VerificationToken;
import com.specflow.api.modules.user.infrastructure.persistence.VerificationTokenDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VerificationTokenConverter 单元测试
 *
 * <p>测试范围：
 * - DO ↔ Entity 双向转换
 * - LocalDateTime ↔ Instant 时区转换
 * - Type 枚举与 String 转换
 * - null 值处理
 */
@DisplayName("VerificationTokenConverter 单元测试")
class VerificationTokenConverterTest {

    private static final String TEST_ID = "token-123";
    private static final String TEST_TOKEN = "abc123def456";
    private static final String TEST_USER_ID = "user-456";
    private static final String TEST_EMAIL = "test@example.com";

    // ==================== toDomain() 测试 ====================

    @Test
    @DisplayName("toDomain() - 完整 DO 应正确转换为 Entity")
    void toDomain_withCompleteDO_shouldConvertToEntity() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        VerificationTokenDO tokenDO = new VerificationTokenDO(
                TEST_ID,
                TEST_TOKEN,
                TEST_USER_ID,
                "EMAIL_VERIFICATION",
                TEST_EMAIL,
                false,
                now.plusHours(1),
                now,
                now
        );

        // When
        VerificationToken token = VerificationTokenConverter.toDomain(tokenDO);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getId()).isEqualTo(TEST_ID);
        assertThat(token.getToken()).isEqualTo(TEST_TOKEN);
        assertThat(token.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(token.getType()).isEqualTo(VerificationToken.Type.EMAIL_VERIFICATION);
        assertThat(token.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toDomain() - null 输入应返回 null")
    void toDomain_withNull_shouldReturnNull() {
        // When
        VerificationToken token = VerificationTokenConverter.toDomain(null);

        // Then
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("toDomain() - 所有类型应正确转换")
    void toDomain_allTypes_shouldConvertCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

        // EMAIL_VERIFICATION
        VerificationTokenDO emailDO = createTokenDO("EMAIL_VERIFICATION");
        assertThat(VerificationTokenConverter.toDomain(emailDO).getType())
                .isEqualTo(VerificationToken.Type.EMAIL_VERIFICATION);

        // PASSWORD_RESET
        VerificationTokenDO resetDO = createTokenDO("PASSWORD_RESET");
        assertThat(VerificationTokenConverter.toDomain(resetDO).getType())
                .isEqualTo(VerificationToken.Type.PASSWORD_RESET);

        // EMAIL_CHANGE
        VerificationTokenDO changeDO = createTokenDO("EMAIL_CHANGE");
        assertThat(VerificationTokenConverter.toDomain(changeDO).getType())
                .isEqualTo(VerificationToken.Type.EMAIL_CHANGE);
    }

    @Test
    @DisplayName("toDomain() - 无效类型字符串应返回 null")
    void toDomain_withInvalidType_shouldReturnNullType() {
        // Given
        VerificationTokenDO tokenDO = createTokenDO("INVALID_TYPE");

        // When
        VerificationToken token = VerificationTokenConverter.toDomain(tokenDO);

        // Then
        assertThat(token.getType()).isNull();
    }

    @Test
    @DisplayName("toDomain() - null 类型应返回 null")
    void toDomain_withNullType_shouldReturnNullType() {
        // Given
        VerificationTokenDO tokenDO = createTokenDO(null);

        // When
        VerificationToken token = VerificationTokenConverter.toDomain(tokenDO);

        // Then
        assertThat(token.getType()).isNull();
    }

    @Test
    @DisplayName("toDomain() - used 为 null 时应默认为 false")
    void toDomain_withNullUsed_shouldDefaultToFalse() {
        // Given
        VerificationTokenDO tokenDO = createMinimalTokenDO();
        tokenDO.setUsed(null);

        // When
        VerificationToken token = VerificationTokenConverter.toDomain(tokenDO);

        // Then
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    @DisplayName("toDomain() - 时间转换应正确处理 UTC 时区")
    void toDomain_shouldConvertTimeInUtc() {
        // Given
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        VerificationTokenDO tokenDO = new VerificationTokenDO(
                TEST_ID, TEST_TOKEN, TEST_USER_ID, "EMAIL_VERIFICATION", null, false,
                localDateTime, localDateTime, localDateTime);

        // When
        VerificationToken token = VerificationTokenConverter.toDomain(tokenDO);

        // Then
        assertThat(token.getCreatedAt()).isEqualTo(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
        assertThat(token.getUpdatedAt()).isEqualTo(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
    }

    // ==================== toDataObject() 测试 ====================

    @Test
    @DisplayName("toDataObject() - 完整 Entity 应正确转换为 DO")
    void toDataObject_withCompleteEntity_shouldConvertToDO() {
        // Given
        Instant now = Instant.now();
        VerificationToken token = new VerificationToken(
                TEST_ID,
                TEST_TOKEN,
                TEST_USER_ID,
                VerificationToken.Type.PASSWORD_RESET,
                TEST_EMAIL,
                false,
                now.plusSeconds(3600),
                now,
                now
        );

        // When
        VerificationTokenDO tokenDO = VerificationTokenConverter.toDataObject(token);

        // Then
        assertThat(tokenDO).isNotNull();
        assertThat(tokenDO.getId()).isEqualTo(TEST_ID);
        assertThat(tokenDO.getToken()).isEqualTo(TEST_TOKEN);
        assertThat(tokenDO.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(tokenDO.getType()).isEqualTo("PASSWORD_RESET");
        assertThat(tokenDO.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(tokenDO.getUsed()).isFalse();
    }

    @Test
    @DisplayName("toDataObject() - null 输入应返回 null")
    void toDataObject_withNull_shouldReturnNull() {
        // When
        VerificationTokenDO tokenDO = VerificationTokenConverter.toDataObject(null);

        // Then
        assertThat(tokenDO).isNull();
    }

    @Test
    @DisplayName("toDataObject() - null 类型应返回 null 字符串")
    void toDataObject_withNullType_shouldReturnNullString() {
        // Given
        Instant now = Instant.now();
        VerificationToken token = new VerificationToken(
                TEST_ID, TEST_TOKEN, TEST_USER_ID, null, null, false,
                now.plusSeconds(3600), now, now);

        // When
        VerificationTokenDO tokenDO = VerificationTokenConverter.toDataObject(token);

        // Then
        assertThat(tokenDO.getType()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 时间转换应正确处理 UTC 时区")
    void toDataObject_shouldConvertTimeInUtc() {
        // Given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        VerificationToken token = new VerificationToken(
                TEST_ID, TEST_TOKEN, TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, false,
                instant.plusSeconds(3600), instant, instant);

        // When
        VerificationTokenDO tokenDO = VerificationTokenConverter.toDataObject(token);

        // Then
        assertThat(tokenDO.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));
        assertThat(tokenDO.getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));
    }

    // ==================== 双向转换一致性测试 ====================

    @Test
    @DisplayName("双向转换 - toDomain 后再 toDataObject 应保持数据一致")
    void roundTrip_shouldPreserveData() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        VerificationTokenDO originalDO = new VerificationTokenDO(
                TEST_ID,
                TEST_TOKEN,
                TEST_USER_ID,
                "EMAIL_VERIFICATION",
                TEST_EMAIL,
                false,
                now.plusHours(1),
                now,
                now
        );

        // When
        VerificationToken token = VerificationTokenConverter.toDomain(originalDO);
        VerificationTokenDO convertedDO = VerificationTokenConverter.toDataObject(token);

        // Then
        assertThat(convertedDO.getId()).isEqualTo(originalDO.getId());
        assertThat(convertedDO.getToken()).isEqualTo(originalDO.getToken());
        assertThat(convertedDO.getUserId()).isEqualTo(originalDO.getUserId());
        assertThat(convertedDO.getType()).isEqualTo(originalDO.getType());
        assertThat(convertedDO.getEmail()).isEqualTo(originalDO.getEmail());
        assertThat(convertedDO.getUsed()).isEqualTo(originalDO.getUsed());
    }

    @Test
    @DisplayName("双向转换 - toDataObject 后再 toDomain 应保持数据一致")
    void roundTripReverse_shouldPreserveData() {
        // Given
        Instant now = Instant.now();
        VerificationToken originalToken = new VerificationToken(
                TEST_ID,
                TEST_TOKEN,
                TEST_USER_ID,
                VerificationToken.Type.PASSWORD_RESET,
                TEST_EMAIL,
                false,
                now.plusSeconds(3600),
                now,
                now
        );

        // When
        VerificationTokenDO tokenDO = VerificationTokenConverter.toDataObject(originalToken);
        VerificationToken convertedToken = VerificationTokenConverter.toDomain(tokenDO);

        // Then
        assertThat(convertedToken.getId()).isEqualTo(originalToken.getId());
        assertThat(convertedToken.getToken()).isEqualTo(originalToken.getToken());
        assertThat(convertedToken.getUserId()).isEqualTo(originalToken.getUserId());
        assertThat(convertedToken.getType()).isEqualTo(originalToken.getType());
        assertThat(convertedToken.getEmail()).isEqualTo(originalToken.getEmail());
        assertThat(convertedToken.isUsed()).isEqualTo(originalToken.isUsed());
        assertThat(convertedToken.getCreatedAt()).isEqualTo(originalToken.getCreatedAt());
        assertThat(convertedToken.getUpdatedAt()).isEqualTo(originalToken.getUpdatedAt());
    }

    // ==================== 辅助方法 ====================

    private VerificationTokenDO createTokenDO(String type) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        return new VerificationTokenDO(
                TEST_ID, TEST_TOKEN, TEST_USER_ID, type, TEST_EMAIL, false,
                now.plusHours(1), now, now);
    }

    private VerificationTokenDO createMinimalTokenDO() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        return new VerificationTokenDO(
                TEST_ID, TEST_TOKEN, TEST_USER_ID, "EMAIL_VERIFICATION", null, false,
                now.plusHours(1), now, now);
    }
}
