package com.specflow.api.config;

import com.specflow.common.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthInterceptor 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthInterceptor 单元测试")
class AuthInterceptorTest {

    @Mock
    private TokenProvider tokenProvider;

    private AuthInterceptor authInterceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private static final String TEST_TOKEN = "token_abc123xyz";
    private static final String TEST_USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        // 使用自定义排除路径模拟业务模块配置
        authInterceptor = new AuthInterceptor(
                tokenProvider,
                List.of("/api/v1/public/test")
        );
    }

    private void setupValidToken() {
        lenient().when(tokenProvider.getUserIdByToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
    }

    // ==================== 排除路径测试 ====================

    @Test
    @DisplayName("preHandle() - Actuator 路径应跳过验证")
    void preHandle_withActuatorPath_shouldSkipValidation() {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - Swagger UI 路径应跳过验证")
    void preHandle_withSwaggerPath_shouldSkipValidation() {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - API Docs 路径应跳过验证")
    void preHandle_withApiDocsPath_shouldSkipValidation() {
        when(request.getRequestURI()).thenReturn("/v3/api-docs");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - 自定义排除路径应跳过验证")
    void preHandle_withCustomExcludePath_shouldSkipValidation() {
        when(request.getRequestURI()).thenReturn("/api/v1/public/test");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - Error 路径应跳过验证")
    void preHandle_withErrorPath_shouldSkipValidation() {
        when(request.getRequestURI()).thenReturn("/error");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    // ==================== Token 验证测试 ====================

    @Test
    @DisplayName("preHandle() - 有效 Token 应通过验证并注入 userId")
    void preHandle_withValidToken_shouldPassAndInjectUserId() {
        setupValidToken();
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider).getUserIdByToken(TEST_TOKEN);
        verify(request).setAttribute("userId", TEST_USER_ID);
    }

    @Test
    @DisplayName("preHandle() - 缺少 Authorization 头应抛出异常")
    void preHandle_withoutAuthHeader_shouldThrowException() {
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("缺少认证令牌");
    }

    @Test
    @DisplayName("preHandle() - 非 Bearer 格式的 Token 应抛出异常")
    void preHandle_withNonBearerToken_shouldThrowException() {
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Basic " + TEST_TOKEN);

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("缺少认证令牌");
    }

    @Test
    @DisplayName("preHandle() - Bearer 后无 Token 应抛出异常")
    void preHandle_withEmptyBearerToken_shouldThrowException() {
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("缺少认证令牌");
    }

    @Test
    @DisplayName("preHandle() - 无效 Token 验证失败时应抛出异常")
    void preHandle_withInvalidToken_shouldThrowException() {
        String invalidToken = "invalid_token";
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenProvider.getUserIdByToken(invalidToken)).thenReturn(null);

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("无效的认证令牌");
    }

    // ==================== 路径匹配测试 ====================

    @Test
    @DisplayName("preHandle() - 子路径也应正确匹配排除规则")
    void preHandle_withSubPath_shouldMatchExclusion() {
        when(request.getRequestURI()).thenReturn("/actuator/prometheus");

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider, never()).getUserIdByToken(anyString());
    }

    @Test
    @DisplayName("preHandle() - 受保护路径需要验证")
    void preHandle_withProtectedPath_shouldRequireValidation() {
        setupValidToken();
        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TEST_TOKEN);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(tokenProvider).getUserIdByToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("无自定义排除路径时只使用默认路径")
    void constructor_withEmptyCustomPaths_shouldUseDefaults() {
        AuthInterceptor interceptor = new AuthInterceptor(tokenProvider, List.of());

        when(request.getRequestURI()).thenReturn("/actuator/health");
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }
}
