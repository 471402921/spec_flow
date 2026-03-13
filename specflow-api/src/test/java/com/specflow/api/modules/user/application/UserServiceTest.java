package com.specflow.api.modules.user.application;

import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.domain.repository.UserRepository;
import com.specflow.api.modules.user.domain.service.TokenProvider;
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService 单元测试
 *
 * <p>测试策略：
 * - Mock UserRepository 和 SessionService
 * - 验证业务规则和异常处理
 * - 验证密码加密
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // ==================== register() 测试 ====================

    @Test
    @DisplayName("register() - 成功注册")
    void register_withValidData_shouldCreateUser() {
        // Given
        String email = "test@example.com";
        String password = "Password123";
        String nickname = "TestUser";

        when(userRepository.existsByEmail(email)).thenReturn(false);

        // When
        User result = userService.register(email, password, nickname);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getNickname()).isEqualTo(nickname);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo(password); // 已加密
    }

    @Test
    @DisplayName("register() - 邮箱格式无效应抛出异常")
    void register_withInvalidEmail_shouldThrowException() {
        // Given
        String invalidEmail = "invalid-email";

        // When & Then
        assertThatThrownBy(() -> userService.register(invalidEmail, "Password123", "Test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - 密码少于8位应抛出异常")
    void register_withShortPassword_shouldThrowException() {
        // Given
        String shortPassword = "Pass1"; // 少于8位

        // When & Then
        assertThatThrownBy(() -> userService.register("test@example.com", shortPassword, "Test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - 纯字母密码应抛出异常")
    void register_withLettersOnlyPassword_shouldThrowException() {
        // Given
        String lettersOnlyPassword = "passwordonly"; // 没有数字

        // When & Then
        assertThatThrownBy(() -> userService.register("test@example.com", lettersOnlyPassword, "Test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码");
    }

    @Test
    @DisplayName("register() - 纯数字密码应抛出异常")
    void register_withNumbersOnlyPassword_shouldThrowException() {
        // Given
        String numbersOnlyPassword = "12345678"; // 没有字母

        // When & Then
        assertThatThrownBy(() -> userService.register("test@example.com", numbersOnlyPassword, "Test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码");
    }

    @Test
    @DisplayName("register() - 邮箱已存在应抛出异常")
    void register_withExistingEmail_shouldThrowException() {
        // Given
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.register(email, "Password123", "Test"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("EMAIL_ALREADY_EXISTS");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - nickname 为 null 时默认使用邮箱前缀")
    void register_withNullNickname_shouldUseEmailPrefix() {
        // Given
        String email = "john.doe@example.com";
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // When
        User result = userService.register(email, "Password123", null);

        // Then
        assertThat(result.getNickname()).isEqualTo("john.doe");
    }

    // ==================== login() 测试 ====================

    @Test
    @DisplayName("login() - 成功登录应返回 token")
    void login_withValidCredentials_shouldReturnToken() {
        // Given
        String email = "test@example.com";
        String password = "Password123";
        String encodedPassword = "encoded_password_hash";
        String userId = "user-123";
        String expectedToken = "token_abc123";

        User user = User.create(email, encodedPassword, "TestUser");
        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(tokenProvider.createToken(userId)).thenReturn(expectedToken);

        // When
        String result = userService.login(email, password);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(tokenProvider).createToken(userId);
    }

    @Test
    @DisplayName("login() - 邮箱大小写不敏感")
    void login_withDifferentCaseEmail_shouldWork() {
        // Given
        String email = "Test.User@Example.COM";
        String password = "Password123";
        String encodedPassword = "encoded_password_hash";

        User user = User.create(email.toLowerCase(), encodedPassword, "TestUser");
        ReflectionTestUtils.setField(user, "id", "user-123");

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(tokenProvider.createToken(anyString())).thenReturn("token");

        // When
        userService.login(email, password);

        // Then - 不抛出异常
        verify(tokenProvider).createToken(anyString());
    }

    @Test
    @DisplayName("login() - 用户不存在应抛出异常")
    void login_withNonExistentUser_shouldThrowException() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.login(email, "Password123"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("邮箱或密码错误");

        verify(tokenProvider, never()).createToken(anyString());
    }

    @Test
    @DisplayName("login() - 密码错误应抛出异常")
    void login_withWrongPassword_shouldThrowException() {
        // Given
        String email = "test@example.com";
        String correctPassword = "Password123";
        String wrongPassword = "WrongPassword456";
        String encodedPassword = "encoded_password_hash";

        User user = User.create(email, encodedPassword, "TestUser");
        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongPassword, encodedPassword)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.login(email, wrongPassword))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("邮箱或密码错误");

        verify(tokenProvider, never()).createToken(anyString());
    }

    // ==================== logout() 测试 ====================

    @Test
    @DisplayName("logout() - 应调用 tokenProvider.revokeToken()")
    void logout_shouldRevokeToken() {
        // Given
        String token = "token_abc123";

        // When
        userService.logout(token);

        // Then
        verify(tokenProvider).revokeToken(token);
    }

    // ==================== getUserById() 测试 ====================

    @Test
    @DisplayName("getUserById() - 用户存在应返回用户")
    void getUserById_withExistingUser_shouldReturnUser() {
        // Given
        String userId = "user-123";
        User user = createTestUser(userId, "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        User result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("getUserById() - 用户不存在应抛出异常")
    void getUserById_withNonExistentUser_shouldThrowException() {
        // Given
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    // ==================== updateProfile() 测试 ====================

    @Test
    @DisplayName("updateProfile() - 成功更新昵称和头像")
    void updateProfile_withValidData_shouldUpdate() {
        // Given
        String userId = "user-123";
        User user = createTestUser(userId, "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        User result = userService.updateProfile(userId, "NewNickname", "https://new-avatar.jpg");

        // Then
        assertThat(result.getNickname()).isEqualTo("NewNickname");
        assertThat(result.getAvatarUrl()).isEqualTo("https://new-avatar.jpg");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateProfile() - 用户不存在应抛出异常")
    void updateProfile_withNonExistentUser_shouldThrowException() {
        // Given
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(userId, "NewName", null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile() - 昵称少于2字符应抛出异常")
    void updateProfile_withTooShortNickname_shouldThrowException() {
        // Given
        String userId = "user-123";
        User user = createTestUser(userId, "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(userId, "A", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("NICKNAME_LENGTH_INVALID");
    }

    @Test
    @DisplayName("updateProfile() - 昵称超过20字符应抛出异常")
    void updateProfile_withTooLongNickname_shouldThrowException() {
        // Given
        String userId = "user-123";
        User user = createTestUser(userId, "test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(userId, "A".repeat(21), null))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("NICKNAME_LENGTH_INVALID");
    }

    // ==================== changePassword() 测试 ====================

    @Test
    @DisplayName("changePassword() - 成功修改密码")
    void changePassword_withCorrectOldPassword_shouldUpdate() {
        // Given
        String userId = "user-123";
        String oldPassword = "OldPass123";
        String newPassword = "NewPass456";
        String oldHash = "encoded_old_password";
        String newHash = "encoded_new_password";

        User user = createTestUser(userId, "test@example.com");
        ReflectionTestUtils.setField(user, "passwordHash", oldHash);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, oldHash)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newHash);

        // When
        userService.changePassword(userId, oldPassword, newPassword);

        // Then
        assertThat(user.getPasswordHash()).isEqualTo(newHash);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword() - 旧密码错误应抛出异常")
    void changePassword_withWrongOldPassword_shouldThrowException() {
        // Given
        String userId = "user-123";
        String wrongOldPassword = "WrongPass456";
        String oldHash = "encoded_old_password";

        User user = createTestUser(userId, "test@example.com");
        ReflectionTestUtils.setField(user, "passwordHash", oldHash);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongOldPassword, oldHash)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(userId, wrongOldPassword, "NewPass789"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INCORRECT_PASSWORD");
    }

    @Test
    @DisplayName("changePassword() - 新密码格式无效应抛出异常")
    void changePassword_withInvalidNewPassword_shouldThrowException() {
        // Given
        String userId = "user-123";
        String oldPassword = "OldPass123";
        String invalidNewPassword = "short";
        String oldHash = "encoded_old_password";

        User user = createTestUser(userId, "test@example.com");
        ReflectionTestUtils.setField(user, "passwordHash", oldHash);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, oldHash)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(userId, oldPassword, invalidNewPassword))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("INVALID_PASSWORD_FORMAT");
    }

    @Test
    @DisplayName("changePassword() - 用户不存在应抛出异常")
    void changePassword_withNonExistentUser_shouldThrowException() {
        // Given
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(userId, "old", "new"))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== 辅助方法 ====================

    private User createTestUser(String id, String email) {
        User user = User.create(email, "hashedPassword", "TestUser");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
