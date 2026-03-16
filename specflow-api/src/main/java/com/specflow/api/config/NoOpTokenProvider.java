package com.specflow.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认 TokenProvider 实现 — 拒绝所有 Token
 *
 * <p>当没有业务模块提供 TokenProvider 实现时自动生效。
 * 业务模块只需实现 TokenProvider 接口并注册为 Spring Bean 即可替代。
 */
@Component
@ConditionalOnMissingBean(value = TokenProvider.class, ignored = NoOpTokenProvider.class)
public class NoOpTokenProvider implements TokenProvider {

    @Override
    public String getUserIdByToken(String token) {
        return null;
    }
}
