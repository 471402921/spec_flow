package com.specflow.api.modules.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User 领域实体单元测试
 *
 * <p>测试范围：
 * - 工厂方法 create()
 * - 领域行为：updateProfile, changePassword, softDelete
 * - 状态验证
 */
@DisplayName("User 领域实体测试")
class UserTest {

    @Test
    @DisplayName("create() - 使用所有参数创建用户")
    void create_withAllParams_shouldCreateUser() {
        // Given
        String email = "test@example.com";
        String passwordHash = "hashedPassword123";
        String nickname = "TestUser";

        // When
        User user = User.create(email, passwordHash, nickname);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("test@example.com"); // 小写
        assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("create() - 邮箱应转为小写存储")
    void create_withUpperCaseEmail_shouldConvertToLowerCase() {
        // Given
        String email = "Test.User@EXAMPLE.COM";
        String passwordHash = "hashedPassword123";

        // When
        User user = User.create(email, passwordHash, null);

        // Then
        assertThat(user.getEmail()).isEqualTo("test.user@example.com");
    }

    @Test
    @DisplayName("create() - nickname 为 null 时使用邮箱前缀")
    void create_withNullNickname_shouldUseEmailPrefix() {
        // Given
        String email = "john.doe@example.com";
        String passwordHash = "hashedPassword123";

        // When
        User user = User.create(email, passwordHash, null);

        // Then
        assertThat(user.getNickname()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("create() - 无 @ 符号的邮箱使用整个邮箱作为 nickname")
    void create_withEmailNoAt_shouldUseFullEmailAsNickname() {
        // Given
        String email = "invalidemail";
        String passwordHash = "hashedPassword123";

        // When
        User user = User.create(email, passwordHash, null);

        // Then
        assertThat(user.getNickname()).isEqualTo("invalidemail");
    }

    @Test
    @DisplayName("updateProfile() - 更新昵称和头像")
    void updateProfile_withNicknameAndAvatar_shouldUpdateBoth() {
        // Given
        User user = User.create("test@example.com", "hash", "OldName");
        String newNickname = "NewName";
        String newAvatar = "https://example.com/avatar.jpg";

        // When
        user.updateProfile(newNickname, newAvatar);

        // Then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getAvatarUrl()).isEqualTo(newAvatar);
        assertThat(user.getUpdatedAt()).isAfter(user.getCreatedAt());
    }

    @Test
    @DisplayName("updateProfile() - 只更新昵称时头像保持不变")
    void updateProfile_withOnlyNickname_shouldKeepAvatar() {
        // Given
        User user = User.create("test@example.com", "hash", "OldName");
        user.updateProfile(null, "https://example.com/avatar.jpg");
        String originalAvatar = user.getAvatarUrl();

        // When
        user.updateProfile("NewName", null);

        // Then
        assertThat(user.getNickname()).isEqualTo("NewName");
        assertThat(user.getAvatarUrl()).isEqualTo(originalAvatar);
    }

    @Test
    @DisplayName("updateProfile() - 参数都为 null 时不改变任何值")
    void updateProfile_withNullParams_shouldNotChange() {
        // Given
        User user = User.create("test@example.com", "hash", "OriginalName");
        String originalNickname = user.getNickname();

        // When
        user.updateProfile(null, null);

        // Then
        assertThat(user.getNickname()).isEqualTo(originalNickname);
    }

    @Test
    @DisplayName("changePassword() - 修改密码哈希")
    void changePassword_shouldUpdatePasswordHash() {
        // Given
        User user = User.create("test@example.com", "oldHash", "TestUser");
        String newHash = "newHashedPassword456";

        // When
        user.changePassword(newHash);

        // Then
        assertThat(user.getPasswordHash()).isEqualTo(newHash);
        assertThat(user.getUpdatedAt()).isAfter(user.getCreatedAt());
    }

    @Test
    @DisplayName("softDelete() - 软删除应设置标记和时间")
    void softDelete_shouldSetDeletedFlagAndTimestamp() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");

        // When
        user.softDelete();

        // Then
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isAfter(user.getCreatedAt());
    }

    @Test
    @DisplayName("softDelete() - 多次调用应更新删除时间")
    void softDelete_calledTwice_shouldUpdateDeletedAt() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");
        user.softDelete();

        // When - 再次调用
        user.softDelete();

        // Then - 仍然是删除状态
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
    }

    // ==================== P2 新增测试 ====================

    @Test
    @DisplayName("create() - 新用户邮箱未验证且未锁定")
    void create_newUser_shouldNotBeVerifiedOrLocked() {
        // When
        User user = User.create("test@example.com", "hash", "TestUser");

        // Then
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    @DisplayName("verifyEmail() - 应标记邮箱为已验证")
    void verifyEmail_shouldMarkEmailAsVerified() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");
        assertThat(user.isEmailVerified()).isFalse();

        // When
        user.verifyEmail();

        // Then
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("isLocked() - 未锁定用户应返回 false")
    void isLocked_withUnlockedUser_shouldReturnFalse() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");

        // Then
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    @DisplayName("isLocked() - 锁定中的用户应返回 true")
    void isLocked_withLockedUser_shouldReturnTrue() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");
        user.lockAccount(15);

        // Then
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    @DisplayName("isLocked() - 锁定过期后应返回 false")
    void isLocked_afterLockExpires_shouldReturnFalse() {
        // Given - 锁定 0 分钟（已过期）
        User user = User.create("test@example.com", "hash", "TestUser");
        user.lockAccount(0);

        // Then
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    @DisplayName("getRemainingLockMinutes() - 未锁定应返回 0")
    void getRemainingLockMinutes_withUnlockedUser_shouldReturnZero() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");

        // Then
        assertThat(user.getRemainingLockMinutes()).isZero();
    }

    @Test
    @DisplayName("getRemainingLockMinutes() - 锁定中应返回剩余分钟数")
    void getRemainingLockMinutes_withLockedUser_shouldReturnMinutes() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");
        user.lockAccount(15);

        // Then
        long minutes = user.getRemainingLockMinutes();
        assertThat(minutes).isGreaterThanOrEqualTo(14).isLessThanOrEqualTo(15);
    }

    @Test
    @DisplayName("recordFailedLogin() - 应增加失败计数")
    void recordFailedLogin_shouldIncrementFailedAttempts() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");

        // When
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);

        // Then
        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
    }

    @Test
    @DisplayName("recordFailedLogin() - 达到最大次数应锁定账号")
    void recordFailedLogin_reachingMaxAttempts_shouldLockAccount() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");

        // When - 第 5 次失败
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        assertThat(user.isLocked()).isFalse();

        user.recordFailedLogin(5, 15);

        // Then
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getLockedUntil()).isNotNull();
    }

    @Test
    @DisplayName("recordSuccessfulLogin() - 应重置失败计数和锁定")
    void recordSuccessfulLogin_shouldResetFailedAttemptsAndLock() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        assertThat(user.isLocked()).isTrue();

        // When
        user.recordSuccessfulLogin();

        // Then
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("lockAccount() - 应锁定账号指定时间")
    void lockAccount_shouldLockAccountForSpecifiedMinutes() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");

        // When
        user.lockAccount(15);

        // Then
        assertThat(user.isLocked()).isTrue();
        assertThat(user.getLockedUntil()).isNotNull();
    }

    @Test
    @DisplayName("unlockAccount() - 应解锁账号并重置计数")
    void unlockAccount_shouldUnlockAndResetAttempts() {
        // Given
        User user = User.create("test@example.com", "hash", "TestUser");
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);
        user.recordFailedLogin(5, 15);

        // When
        user.unlockAccount();

        // Then
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }
}
