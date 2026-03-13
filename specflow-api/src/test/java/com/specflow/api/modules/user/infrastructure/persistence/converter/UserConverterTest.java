package com.specflow.api.modules.user.infrastructure.persistence.converter;

import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.infrastructure.persistence.UserDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserConverter 单元测试
 *
 * <p>测试范围：
 * - DO ↔ Entity 双向转换
 * - LocalDateTime ↔ Instant 时区转换
 * - null 值处理
 */
@DisplayName("UserConverter 单元测试")
class UserConverterTest {

    private static final String TEST_ID = "user-123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD_HASH = "hashedPassword123";
    private static final String TEST_NICKNAME = "TestUser";
    private static final String TEST_AVATAR_URL = "https://example.com/avatar.jpg";

    // ==================== toDomain() 测试 ====================

    @Test
    @DisplayName("toDomain() - 完整 DO 应正确转换为 Entity")
    void toDomain_withCompleteDO_shouldConvertToEntity() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        UserDO userDO = new UserDO(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now,
                false,
                0,
                null
        );

        // When
        User user = UserConverter.toDomain(userDO);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(TEST_ID);
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(user.getPasswordHash()).isEqualTo(TEST_PASSWORD_HASH);
        assertThat(user.getNickname()).isEqualTo(TEST_NICKNAME);
        assertThat(user.getAvatarUrl()).isEqualTo(TEST_AVATAR_URL);
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("toDomain() - null 输入应返回 null")
    void toDomain_withNull_shouldReturnNull() {
        // When
        User user = UserConverter.toDomain(null);

        // Then
        assertThat(user).isNull();
    }

    @Test
    @DisplayName("toDomain() - 已删除用户应正确转换")
    void toDomain_withDeletedUser_shouldConvertCorrectly() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDateTime deletedAt = now.minusDays(1);
        UserDO userDO = new UserDO(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                null,
                true,
                deletedAt,
                now.minusDays(7),
                now,
                true,
                3,
                null
        );

        // When
        User user = UserConverter.toDomain(userDO);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("toDomain() - 时间转换应正确处理 UTC 时区")
    void toDomain_shouldConvertTimeInUtc() {
        // Given
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        UserDO userDO = new UserDO(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                null,
                false,
                null,
                localDateTime,
                localDateTime,
                false,
                0,
                null
        );

        // When
        User user = UserConverter.toDomain(userDO);

        // Then
        assertThat(user.getCreatedAt()).isEqualTo(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
        assertThat(user.getUpdatedAt()).isEqualTo(localDateTime.atZone(ZoneId.of("UTC")).toInstant());
    }

    @Test
    @DisplayName("toDomain() - deleted 字段为 null 时应默认为 false")
    void toDomain_withNullDeleted_shouldDefaultToFalse() {
        // Given
        UserDO userDO = createMinimalUserDO();
        userDO.setDeleted(null);

        // When
        User user = UserConverter.toDomain(userDO);

        // Then
        assertThat(user.isDeleted()).isFalse();
    }

    // ==================== toDataObject() 测试 ====================

    @Test
    @DisplayName("toDataObject() - 完整 Entity 应正确转换为 DO")
    void toDataObject_withCompleteEntity_shouldConvertToDO() {
        // Given
        Instant now = Instant.now();
        User user = new User(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now,
                false,
                0,
                null
        );

        // When
        UserDO userDO = UserConverter.toDataObject(user);

        // Then
        assertThat(userDO).isNotNull();
        assertThat(userDO.getId()).isEqualTo(TEST_ID);
        assertThat(userDO.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(userDO.getPasswordHash()).isEqualTo(TEST_PASSWORD_HASH);
        assertThat(userDO.getNickname()).isEqualTo(TEST_NICKNAME);
        assertThat(userDO.getAvatarUrl()).isEqualTo(TEST_AVATAR_URL);
        assertThat(userDO.getDeleted()).isFalse();
        assertThat(userDO.getDeletedAt()).isNull();
        assertThat(userDO.getCreatedAt()).isNotNull();
        assertThat(userDO.getUpdatedAt()).isNotNull();
        assertThat(userDO.getEmailVerified()).isFalse();
        assertThat(userDO.getFailedLoginAttempts()).isZero();
        assertThat(userDO.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - null 输入应返回 null")
    void toDataObject_withNull_shouldReturnNull() {
        // When
        UserDO userDO = UserConverter.toDataObject(null);

        // Then
        assertThat(userDO).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 已删除用户应正确转换")
    void toDataObject_withDeletedUser_shouldConvertCorrectly() {
        // Given
        Instant now = Instant.now();
        Instant deletedAt = now.minusSeconds(86400);
        User user = new User(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                null,
                true,
                deletedAt,
                now.minusSeconds(604800),
                now,
                true,
                5,
                now
        );

        // When
        UserDO userDO = UserConverter.toDataObject(user);

        // Then
        assertThat(userDO).isNotNull();
        assertThat(userDO.getDeleted()).isTrue();
        assertThat(userDO.getDeletedAt()).isNotNull();
        assertThat(userDO.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("toDataObject() - 时间转换应正确处理 UTC 时区")
    void toDataObject_shouldConvertTimeInUtc() {
        // Given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");
        User user = new User(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                null,
                false,
                null,
                instant,
                instant,
                false,
                0,
                null
        );

        // When
        UserDO userDO = UserConverter.toDataObject(user);

        // Then
        assertThat(userDO.getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));
        assertThat(userDO.getUpdatedAt()).isEqualTo(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")));
    }

    // ==================== 双向转换一致性测试 ====================

    @Test
    @DisplayName("双向转换 - toDomain 后再 toDataObject 应保持数据一致")
    void roundTrip_shouldPreserveData() {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        UserDO originalDO = new UserDO(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now,
                false,
                0,
                null
        );

        // When
        User user = UserConverter.toDomain(originalDO);
        UserDO convertedDO = UserConverter.toDataObject(user);

        // Then
        assertThat(convertedDO.getId()).isEqualTo(originalDO.getId());
        assertThat(convertedDO.getEmail()).isEqualTo(originalDO.getEmail());
        assertThat(convertedDO.getPasswordHash()).isEqualTo(originalDO.getPasswordHash());
        assertThat(convertedDO.getNickname()).isEqualTo(originalDO.getNickname());
        assertThat(convertedDO.getAvatarUrl()).isEqualTo(originalDO.getAvatarUrl());
        assertThat(convertedDO.getDeleted()).isEqualTo(originalDO.getDeleted());
    }

    @Test
    @DisplayName("双向转换 - toDataObject 后再 toDomain 应保持数据一致")
    void roundTripReverse_shouldPreserveData() {
        // Given
        Instant now = Instant.now();
        User originalUser = new User(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                TEST_AVATAR_URL,
                false,
                null,
                now,
                now,
                false,
                0,
                null
        );

        // When
        UserDO userDO = UserConverter.toDataObject(originalUser);
        User convertedUser = UserConverter.toDomain(userDO);

        // Then
        assertThat(convertedUser.getId()).isEqualTo(originalUser.getId());
        assertThat(convertedUser.getEmail()).isEqualTo(originalUser.getEmail());
        assertThat(convertedUser.getPasswordHash()).isEqualTo(originalUser.getPasswordHash());
        assertThat(convertedUser.getNickname()).isEqualTo(originalUser.getNickname());
        assertThat(convertedUser.getAvatarUrl()).isEqualTo(originalUser.getAvatarUrl());
        assertThat(convertedUser.isDeleted()).isEqualTo(originalUser.isDeleted());
        assertThat(convertedUser.getCreatedAt()).isEqualTo(originalUser.getCreatedAt());
        assertThat(convertedUser.getUpdatedAt()).isEqualTo(originalUser.getUpdatedAt());
    }

    // ==================== 辅助方法 ====================

    private UserDO createMinimalUserDO() {
        return new UserDO(
                TEST_ID,
                TEST_EMAIL,
                TEST_PASSWORD_HASH,
                TEST_NICKNAME,
                null,
                false,
                null,
                LocalDateTime.now(ZoneId.of("UTC")),
                LocalDateTime.now(ZoneId.of("UTC")),
                false,
                0,
                null
        );
    }
}
