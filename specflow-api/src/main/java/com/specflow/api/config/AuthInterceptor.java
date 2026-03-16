package com.specflow.api.config;

import com.specflow.common.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 认证拦截器 - 验证 Bearer Token
 *
 * <p>职责：
 * - 从请求头提取 Bearer Token
 * - 验证 Token 有效性
 * - 将 userId 存入请求属性供 Controller 使用
 *
 * <p>排除路径可通过配置扩展：
 * <pre>
 * specflow:
 *   auth:
 *     exclude-paths:
 *       - /api/v1/users/register
 *       - /api/v1/users/login
 * </pre>
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenProvider tokenProvider;
    private final List<String> excludePaths;

    // 框架内置排除路径
    private static final List<String> DEFAULT_EXCLUDE_PATHS = List.of(
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/error"
    );

    public AuthInterceptor(
            @NonNull TokenProvider tokenProvider,
            @Value("${specflow.auth.exclude-paths:}") List<String> customExcludePaths) {
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        List<String> merged = new ArrayList<>(DEFAULT_EXCLUDE_PATHS);
        if (customExcludePaths != null) {
            customExcludePaths.stream()
                    .filter(p -> p != null && !p.isBlank())
                    .forEach(merged::add);
        }
        this.excludePaths = Collections.unmodifiableList(merged);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String path = request.getRequestURI();

        if (isExcludedPath(path)) {
            return true;
        }

        String token = extractTokenFromRequest(request);
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("缺少认证令牌");
        }

        String userId = tokenProvider.getUserIdByToken(token);
        if (userId == null) {
            throw new AuthenticationException("无效的认证令牌");
        }
        request.setAttribute("userId", userId);

        return true;
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isExcludedPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return excludePaths.stream().anyMatch(excludePath -> {
            if (excludePath.endsWith("/")) {
                return path.startsWith(excludePath);
            }
            return path.equals(excludePath) || path.startsWith(excludePath + "/");
        });
    }
}
