package com.specflow.api.modules.user.infrastructure.persistence;

import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.infrastructure.persistence.converter.UserConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserRepositoryImpl 单元测试
 *
 * <p>测试策略：
 * - Mock UserMapper
 * - 测试 DO ↔ Entity 转换逻辑
 * - 验证查询条件构建
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryImpl 单元测试")
class UserRepositoryImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_EMAIL = "test@example.com";

    // ==================== save() 测试 ====================

    @Test
    @DisplayName("save() - 新用户应执行插入")
    void save_withNewUser_shouldInsert() {
        // Given
        User user = User.create(TEST_EMAIL, "hashedPassword", "TestUser");

        when(userMapper.updateById(any(UserDO.class))).thenReturn(0);

        // When
        userRepository.save(user);

        // Then
        verify(userMapper).updateById(any(UserDO.class));
        verify(userMapper).insert(any(UserDO.class));
    }

    @Test
    @DisplayName("save() - 已存在用户应执行更新")
    void save_withExistingUser_shouldUpdate() {
        // Given
        User user = User.create(TEST_EMAIL, "hashedPassword", "TestUser");

        when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

        // When
        userRepository.save(user);

        // Then
        verify(userMapper).updateById(any(UserDO.class));
    }

    // ==================== findById() 测试 ====================

    @Test
    @DisplayName("findById() - 用户存在应返回用户")
    void findById_withExistingUser_shouldReturnUser() {
        // Given
        UserDO userDO = createUserDO(TEST_USER_ID, TEST_EMAIL);
        when(userMapper.selectById(TEST_USER_ID)).thenReturn(userDO);

        // When
        Optional<User> result = userRepository.findById(TEST_USER_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TEST_USER_ID);
        assertThat(result.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("findById() - 用户不存在应返回空")
    void findById_withNonExistentUser_shouldReturnEmpty() {
        // Given
        when(userMapper.selectById("non-existent")).thenReturn(null);

        // When
        Optional<User> result = userRepository.findById("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== findByEmail() 测试 ====================

    @Test
    @DisplayName("findByEmail() - 邮箱存在应返回用户")
    void findByEmail_withExistingEmail_shouldReturnUser() {
        // Given
        UserDO userDO = createUserDO(TEST_USER_ID, TEST_EMAIL);
        when(userMapper.selectByEmail(TEST_EMAIL)).thenReturn(userDO);

        // When
        Optional<User> result = userRepository.findByEmail(TEST_EMAIL);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("findByEmail() - 邮箱不存在应返回空")
    void findByEmail_withNonExistentEmail_shouldReturnEmpty() {
        // Given
        when(userMapper.selectByEmail("nonexistent@example.com")).thenReturn(null);

        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== existsByEmail() 测试 ====================

    @Test
    @DisplayName("existsByEmail() - 邮箱存在应返回 true")
    void existsByEmail_withExistingEmail_shouldReturnTrue() {
        // Given
        when(userMapper.countByEmail(TEST_EMAIL)).thenReturn(1);

        // When
        boolean result = userRepository.existsByEmail(TEST_EMAIL);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByEmail() - 邮箱不存在应返回 false")
    void existsByEmail_withNonExistentEmail_shouldReturnFalse() {
        // Given
        when(userMapper.countByEmail("nonexistent@example.com")).thenReturn(0);

        // When
        boolean result = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isFalse();
    }

    // ==================== 辅助方法 ====================

    private UserDO createUserDO(String id, String email) {
        UserDO userDO = new UserDO();
        userDO.setId(id);
        userDO.setEmail(email);
        userDO.setPasswordHash("hashedPassword");
        userDO.setNickname("TestUser");
        userDO.setDeleted(false);
        return userDO;
    }
}
