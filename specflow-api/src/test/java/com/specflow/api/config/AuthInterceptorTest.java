package com.specflow.api.config;

import com.specflow.api.config.TokenProvider;
import com.specflow.common.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthInterceptor 单元测试
 *
 * <p>测试范围：
 * - Token 提取和验证
 * - 排除路径处理
 * - userId 注入到请求属性
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthInterceptor 单元测试")
class AuthInterceptorTest {

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private static final String TEST_TOKEN = "token_abc123xyz";
    private static final String TEST_USER_ID = "user-123";

    private void setupValidToken() {
        lenient().when(tokenProvider.getUserIdByToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
    }

    // ==================== 排除路径测试 ====================

    @Test
    @DisplayName("preHandle() - 注册路径应跳过验证")
    void preHandle_withRegisterPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users/register");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - 登录路径应跳过验证")
    void preHandle_withLoginPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users/login");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - Session 相关路径应跳过验证")
    void preHandle_withSessionsPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/sessions/test-token");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - Actuator 路径应跳过验证")
    void preHandle_withActuatorPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - Swagger UI 路径应跳过验证")
    void preHandle_withSwaggerPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - API Docs 路径应跳过验证")
    void preHandle_withApiDocsPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/v3/api-docs");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    // ==================== Token 验证测试 ====================

    @Test
    @DisplayName("preHandle() - 有效 Token 应通过验证并注入 userId")
    void preHandle_withValidToken_shouldPassAndInjectUserId() {
        // Given
        setupValidToken();
        when(request.getRequestURI()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider).getUserIdByToken(TEST_TOKEN);
        verify(request).setAttribute("userId", TEST_USER_ID);
    }

    @Test
    @DisplayName("preHandle() - 缺少 Authorization 头应抛出异常")
    void preHandle_withoutAuthHeader_shouldThrowException() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("缺少认证令牌");
    }

    @Test
    @DisplayName("preHandle() - 非 Bearer 格式的 Token 应抛出异常")
    void preHandle_withNonBearerToken_shouldThrowException() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Basic " + TEST_TOKEN);

        // When & Then
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("缺少认证令牌");
    }

    @Test
    @DisplayName("preHandle() - Bearer 后无 Token 应抛出异常")
    void preHandle_withEmptyBearerToken_shouldThrowException() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When & Then
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("缺少认证令牌");
    }

    @Test
    @DisplayName("preHandle() - 无效 Token 验证失败时应抛出异常")
    void preHandle_withInvalidToken_shouldThrowException() {
        // Given
        String invalidToken = "invalid_token";
        when(request.getRequestURI()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenProvider.getUserIdByToken(invalidToken)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("无效的认证令牌");
    }

    // ==================== 路径匹配测试 ====================

    @Test
    @DisplayName("preHandle() - 子路径也应正确匹配排除规则")
    void preHandle_withSubPath_shouldMatchExclusion() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/sessions/test-token/validate");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - Swagger 子路径应跳过验证")
    void preHandle_withSwaggerSubPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/swagger-initializer.js");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - API Docs 子路径应跳过验证")
    void preHandle_withApiDocsSubPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/api-docs/swagger-config");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - Error 路径应跳过验证")
    void preHandle_withErrorPath_shouldSkipValidation() {
        // Given
        when(request.getRequestURI()).thenReturn("/error");

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - 普通受保护路径需要验证")
    void preHandle_withProtectedPath_shouldRequireValidation() {
        // Given
        setupValidToken();
        when(request.getRequestURI()).thenReturn("/api/v1/pets");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider).getUserIdByToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("preHandle() - 嵌套受保护路径需要验证")
    void preHandle_withNestedProtectedPath_shouldRequireValidation() {
        // Given
        setupValidToken();
        when(request.getRequestURI()).thenReturn("/api/v1/pets/pet-123/restore");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);

        // When
        boolean result = authInterceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
        verify(tokenProvider).getUserIdByToken(TEST_TOKEN);
    }
}
