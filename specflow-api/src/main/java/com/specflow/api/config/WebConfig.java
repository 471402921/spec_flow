package com.specflow.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

/**
 * Web MVC 配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @NonNull
    private final HandlerInterceptor traceIdInterceptor;

    @NonNull
    private final HandlerInterceptor authInterceptor;

    public WebConfig(@NonNull TraceIdInterceptor traceIdInterceptor,
                     @NonNull AuthInterceptor authInterceptor) {
        this.traceIdInterceptor = Objects.requireNonNull(traceIdInterceptor, "traceIdInterceptor must not be null");
        this.authInterceptor = Objects.requireNonNull(authInterceptor, "authInterceptor must not be null");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor);
        registry.addInterceptor(authInterceptor);
    }
}
