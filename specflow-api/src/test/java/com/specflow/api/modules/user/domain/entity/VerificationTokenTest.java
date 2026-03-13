package com.specflow.api.modules.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VerificationToken 领域实体单元测试
 *
 * <p>测试范围：
 * - 工厂方法（create）
 * - 领域行为（isValid, markAsUsed, isWithinSeconds）
 * - 状态转换
 */
@DisplayName("VerificationToken 领域实体单元测试")
class VerificationTokenTest {

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_EMAIL = "test@example.com";

    // ==================== 工厂方法测试 ====================

    @Test
    @DisplayName("create() - 应创建有效的验证令牌")
    void create_shouldCreateValidToken() {
        // When
        VerificationToken token = VerificationToken.create(
                TEST_USER_ID,
                VerificationToken.Type.EMAIL_VERIFICATION,
                TEST_EMAIL,
                3600
        );

        // Then
        assertThat(token).isNotNull();
        assertThat(token.getId()).isNotNull();
        assertThat(token.getToken()).isNotNull().hasSize(32);
        assertThat(token.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(token.getType()).isEqualTo(VerificationToken.Type.EMAIL_VERIFICATION);
        assertThat(token.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getUpdatedAt()).isNotNull();
        assertThat(token.getExpiredAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("create() - 邮箱为 null 时应正常创建")
    void create_withNullEmail_shouldCreateToken() {
        // When
        VerificationToken token = VerificationToken.create(
                TEST_USER_ID,
                VerificationToken.Type.PASSWORD_RESET,
                null,
                1800
        );

        // Then
        assertThat(token.getEmail()).isNull();
        assertThat(token.getType()).isEqualTo(VerificationToken.Type.PASSWORD_RESET);
    }

    @Test
    @DisplayName("create() - 应生成不同的令牌值")
    void create_shouldGenerateDifferentTokens() {
        // When
        VerificationToken token1 = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, 3600);
        VerificationToken token2 = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, 3600);

        // Then
        assertThat(token1.getToken()).isNotEqualTo(token2.getToken());
        assertThat(token1.getId()).isNotEqualTo(token2.getId());
    }

    @Test
    @DisplayName("create() - 应支持所有令牌类型")
    void create_shouldSupportAllTypes() {
        // When & Then
        VerificationToken emailToken = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, TEST_EMAIL, 86400);
        assertThat(emailToken.getType()).isEqualTo(VerificationToken.Type.EMAIL_VERIFICATION);

        VerificationToken resetToken = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.PASSWORD_RESET, TEST_EMAIL, 1800);
        assertThat(resetToken.getType()).isEqualTo(VerificationToken.Type.PASSWORD_RESET);

        VerificationToken changeToken = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_CHANGE, "new@example.com", 86400);
        assertThat(changeToken.getType()).isEqualTo(VerificationToken.Type.EMAIL_CHANGE);
    }

    // ==================== isValid() 测试 ====================

    @Test
    @DisplayName("isValid() - 未使用且未过期的令牌应有效")
    void isValid_withUnusedAndUnexpiredToken_shouldReturnTrue() {
        // Given
        VerificationToken token = createToken(false, Instant.now().plusSeconds(3600));

        // When & Then
        assertThat(token.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid() - 已使用的令牌应无效")
    void isValid_withUsedToken_shouldReturnFalse() {
        // Given
        VerificationToken token = createToken(true, Instant.now().plusSeconds(3600));

        // When & Then
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid() - 已过期的令牌应无效")
    void isValid_withExpiredToken_shouldReturnFalse() {
        // Given
        VerificationToken token = createToken(false, Instant.now().minusSeconds(1));

        // When & Then
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid() - 已使用且已过期的令牌应无效")
    void isValid_withUsedAndExpiredToken_shouldReturnFalse() {
        // Given
        VerificationToken token = createToken(true, Instant.now().minusSeconds(3600));

        // When & Then
        assertThat(token.isValid()).isFalse();
    }

    // ==================== markAsUsed() 测试 ====================

    @Test
    @DisplayName("markAsUsed() - 应标记令牌为已使用")
    void markAsUsed_shouldMarkTokenAsUsed() {
        // Given
        VerificationToken token = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, 3600);
        assertThat(token.isUsed()).isFalse();

        // When
        token.markAsUsed();

        // Then
        assertThat(token.isUsed()).isTrue();
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("markAsUsed() - 应更新更新时间")
    void markAsUsed_shouldUpdateUpdatedAt() {
        // Given
        VerificationToken token = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, 3600);
        Instant originalUpdatedAt = token.getUpdatedAt();

        // When
        try {
            Thread.sleep(10); // 确保时间变化
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        token.markAsUsed();

        // Then
        assertThat(token.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    // ==================== isWithinSeconds() 测试 ====================

    @Test
    @DisplayName("isWithinSeconds() - 在指定时间内应返回 true")
    void isWithinSeconds_withinTimeWindow_shouldReturnTrue() {
        // Given
        VerificationToken token = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, 3600);

        // When & Then
        assertThat(token.isWithinSeconds(60)).isTrue();
    }

    @Test
    @DisplayName("isWithinSeconds() - 超过指定时间应返回 false")
    void isWithinSeconds_outsideTimeWindow_shouldReturnFalse() {
        // Given - 创建一个刚创建的令牌，但检查一个极短的时间窗口
        VerificationToken token = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, 3600);

        // When & Then - 已经过了一段时间，0 秒的时间窗口应该返回 false
        try {
            Thread.sleep(100); // 等待 100ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(token.isWithinSeconds(0)).isFalse();
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("边界 - 恰好过期的令牌应无效")
    void boundary_tokenExactlyExpired_shouldBeInvalid() {
        // Given - 创建一个恰好过期的令牌（过去 0 秒）
        VerificationToken token = new VerificationToken();
        token.setUsed(false);
        token.setExpiredAt(Instant.now());

        // When & Then
        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("边界 - 长过期时间应正确设置")
    void boundary_longExpireTime_shouldSetCorrectly() {
        // Given
        long sevenDaysInSeconds = 7 * 24 * 60 * 60;

        // When
        VerificationToken token = VerificationToken.create(
                TEST_USER_ID, VerificationToken.Type.EMAIL_VERIFICATION, null, sevenDaysInSeconds);

        // Then
        Instant expectedExpiry = token.getCreatedAt().plusSeconds(sevenDaysInSeconds);
        assertThat(token.getExpiredAt()).isAfterOrEqualTo(expectedExpiry.minusSeconds(1));
    }

    // ==================== 辅助方法 ====================

    private VerificationToken createToken(boolean used, Instant expiredAt) {
        VerificationToken token = new VerificationToken();
        token.setId("token-id");
        token.setToken("test-token-value-12345678901234567890");
        token.setUserId(TEST_USER_ID);
        token.setType(VerificationToken.Type.EMAIL_VERIFICATION);
        token.setUsed(used);
        token.setExpiredAt(expiredAt);
        token.setCreatedAt(Instant.now());
        token.setUpdatedAt(Instant.now());
        return token;
    }
}
