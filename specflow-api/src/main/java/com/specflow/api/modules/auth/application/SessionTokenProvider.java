package com.specflow.api.modules.auth.application;

import com.specflow.api.modules.auth.domain.entity.Session;
import com.specflow.api.modules.auth.domain.repository.SessionRepository;
import com.specflow.api.modules.user.domain.service.TokenProvider;
import com.specflow.api.util.LogMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * TokenProvider 的 Session 实现
 *
 * <p>职责：
 * - 基于 Session 领域模型实现 TokenProvider 接口
 * - 将 User 模块的认证需求与 Auth 模块的 Session 能力桥接
 *
 * <p>架构关系：
 * - Auth 模块（支撑域）依赖 User 模块（核心域）的 TokenProvider 接口
 * - 实现反向依赖，避免 User 模块直接依赖 Auth 模块
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SessionTokenProvider implements TokenProvider {

    private final SessionRepository sessionRepository;

    /**
     * 会话过期天数配置（从配置文件读取）
     */
    @Value("${soulpal.session.expiration-days:30}")
    private int sessionExpirationDays;

    @Override
    public String createToken(String userId) {
        log.info("Creating token for user: {}", userId);
        String token = generateToken();
        Session session = Session.create(userId, token, sessionExpirationDays);
        sessionRepository.save(session);
        log.debug("Token created: sessionId={}, token={}", session.getId(),
            LogMasker.maskToken(token));
        return token;
    }

    @Override
    public void revokeToken(String token) {
        log.info("Revoking token: token={}", LogMasker.maskToken(token));
        Session session = sessionRepository.findByToken(token)
                .orElse(null);
        if (session != null) {
            session.revoke();
            sessionRepository.save(session);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElse(null);
        return session != null && session.isValid();
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserIdByToken(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElse(null);
        if (session != null && session.isValid()) {
            return session.getUserId();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Instant getExpiredAtByToken(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElse(null);
        if (session != null && session.isValid()) {
            return session.getExpiredAt();
        }
        return null;
    }

    /**
     * 生成会话令牌
     *
     * @return 格式为 "token_<UUID>" 的令牌字符串
     */
    private String generateToken() {
        return "token_" + UUID.randomUUID().toString().replace("-", "");
    }
}
