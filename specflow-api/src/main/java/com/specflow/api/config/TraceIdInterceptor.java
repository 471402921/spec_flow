package com.specflow.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * TraceId 拦截器 - 为每个请求生成唯一的 traceId
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";
    private static final String X_TRACE_ID = "X-Trace-Id";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // 从请求头获取 traceId，如果没有则生成新的
        String traceId = request.getHeader(X_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }

        // 设置到 MDC 中供日志使用
        MDC.put(TRACE_ID, traceId);

        // 添加到响应头中
        response.addHeader(X_TRACE_ID, traceId);

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {
        // 请求结束后清理 MDC
        MDC.remove(TRACE_ID);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
