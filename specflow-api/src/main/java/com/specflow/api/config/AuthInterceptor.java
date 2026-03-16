package com.specflow.api.config;

import com.specflow.api.config.TokenProvider;
import com.specflow.common.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 认证拦截器 - 验证 Session Token
 *
 * <p>职责：
 * - 从请求头提取 Bearer Token
 * - 验证 Token 有效性
 * - 将 userId 存入请求属性供 Controller 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenProvider tokenProvider;

    // 排除路径列表（框架基础设施路径）
    private static final List<String> EXCLUDE_PATHS = List.of(
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs",
            "/error"
    );

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String path = request.getRequestURI();

        // 检查是否是排除路径
        if (isExcludedPath(path)) {
            return true;
        }

        // 提取 Token
        String token = extractTokenFromRequest(request);
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("缺少认证令牌");
        }

        // 验证 Token 并获取用户信息
        String userId = tokenProvider.getUserIdByToken(token);
        if (userId == null) {
            throw new AuthenticationException("无效的认证令牌");
        }
        request.setAttribute("userId", userId);

        return true;
    }

    /**
     * 从请求头提取 Bearer Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 检查路径是否在排除列表中
     */
    private boolean isExcludedPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return EXCLUDE_PATHS.stream().anyMatch(excludePath -> {
            if (excludePath.endsWith("/")) {
                return path.startsWith(excludePath);
            }
            return path.equals(excludePath) || path.startsWith(excludePath + "/");
        });
    }
}
