package com.specflow.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置
 *
 * <p>职责：
 * - 配置密码编码器（BCrypt）
 * - 集中管理安全相关 Bean
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码编码器 Bean
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
