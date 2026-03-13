package com.specflow.api.modules.auth.application;

import com.specflow.api.modules.auth.domain.entity.Session;
import com.specflow.api.modules.auth.domain.repository.SessionRepository;
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SessionService 单元测试
 *
 * 测试策略：
 * - 使用 Mockito 模拟 SessionRepository
 * - 专注于业务逻辑测试，不涉及数据库
 * - 验证正常流程和异常场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService 单元测试")
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionService sessionService;

    private String testUserId;
    private String testToken;
    private Session mockSession;

    @BeforeEach
    void setUp() {
        // 设置 sessionExpirationDays 字段值（因为 @Value 注解在单元测试中不会生效）
        ReflectionTestUtils.setField(sessionService, "sessionExpirationDays", 30);

        testUserId = "test-user-123";
        testToken = "token_abc123def456";

        // 创建一个模拟的有效会话
        mockSession = Session.create(testUserId, testToken, 30);
    }

    @Test
    @DisplayName("创建会话 - 应返回有效会话对象")
    void createSession_shouldReturnValidSession() {
        // Given
        // save() 返回 void，无需 mock 返回值

        // When
        Session result = sessionService.createSession(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getToken()).startsWith("token_");
        assertThat(result.isValid()).isTrue();
        assertThat(result.isRevoked()).isFalse();

        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("根据 Token 查询会话 - 会话存在时应返回会话")
    void getSessionByToken_whenSessionExists_shouldReturnSession() {
        // Given
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.of(mockSession));

        // When
        Session result = sessionService.getSessionByToken(testToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getToken()).isEqualTo(testToken);

        verify(sessionRepository).findByToken(testToken);
    }

    @Test
    @DisplayName("根据 Token 查询会话 - 会话不存在时应抛出异常")
    void getSessionByToken_whenSessionNotExists_shouldThrowNotFoundException() {
        // Given
        when(sessionRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sessionService.getSessionByToken("non-existent-token"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("会话不存在");

        verify(sessionRepository).findByToken(anyString());
    }

    @Test
    @DisplayName("验证会话 - 有效会话应返回 true")
    void validateSession_whenSessionIsValid_shouldReturnTrue() {
        // Given
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.of(mockSession));

        // When
        boolean result = sessionService.validateSession(testToken);

        // Then
        assertThat(result).isTrue();
        verify(sessionRepository).findByToken(testToken);
    }

    @Test
    @DisplayName("验证会话 - 会话不存在时应抛出异常")
    void validateSession_whenSessionNotExists_shouldThrowAuthenticationException() {
        // Given
        when(sessionRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sessionService.validateSession("non-existent-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("会话不存在");
    }

    @Test
    @DisplayName("验证会话 - 已撤销的会话应抛出异常")
    void validateSession_whenSessionIsRevoked_shouldThrowAuthenticationException() {
        // Given
        mockSession.revoke();
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.of(mockSession));

        // When & Then
        assertThatThrownBy(() -> sessionService.validateSession(testToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("会话已撤销");
    }

    @Test
    @DisplayName("验证会话 - 已过期的会话应抛出异常")
    void validateSession_whenSessionIsExpired_shouldThrowAuthenticationException() {
        // Given
        // 创建一个已过期的会话（过期时间设为过去）
        Session expiredSession = Session.create(testUserId, testToken, -1);
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.of(expiredSession));

        // When & Then
        assertThatThrownBy(() -> sessionService.validateSession(testToken))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("会话已过期");
    }

    @Test
    @DisplayName("撤销会话 - 应成功撤销并保存")
    void revokeSession_shouldSuccessfullyRevoke() {
        // Given
        when(sessionRepository.findByToken(testToken)).thenReturn(Optional.of(mockSession));
        // save() 返回 void，无需 mock 返回值

        // When
        sessionService.revokeSession(testToken);

        // Then
        assertThat(mockSession.isRevoked()).isTrue();
        assertThat(mockSession.isValid()).isFalse();

        verify(sessionRepository).findByToken(testToken);
        verify(sessionRepository).save(mockSession);
    }

    @Test
    @DisplayName("撤销会话 - 会话不存在时应抛出异常")
    void revokeSession_whenSessionNotExists_shouldThrowNotFoundException() {
        // Given
        when(sessionRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sessionService.revokeSession("non-existent-token"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("会话不存在");
    }
}
