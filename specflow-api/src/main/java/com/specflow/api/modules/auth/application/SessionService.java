package com.specflow.api.modules.auth.application;

import com.specflow.api.modules.auth.domain.entity.Session;
import com.specflow.api.modules.auth.domain.repository.SessionRepository;
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Session 应用服务
 *
 * 职责：
 * - 定义业务用例（Use Cases）
 * - 声明事务边界
 * - 编排 Domain 对象和 Repository
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    /**
     * 会话过期天数配置（从配置文件读取）
     */
    @Value("${soulpal.session.expiration-days:30}")
    private int sessionExpirationDays;

    /**
     * 创建新会话
     *
     * @param userId 用户 ID
     * @return 创建的会话实体
     */
    public Session createSession(String userId) {
        log.info("Creating session for user: {}", userId);
        String token = generateToken();
        Session session = Session.create(userId, token, sessionExpirationDays);
        sessionRepository.save(session);
        log.debug("Session created: id={}, token={}***", session.getId(),
            token.length() > 16 ? token.substring(0, 16) : token);
        return session;
    }

    /**
     * 根据 Token 查询会话
     *
     * @param token 会话令牌
     * @return 会话实体
     * @throws NotFoundException 会话不存在
     */
    @Transactional(readOnly = true)
    public Session getSessionByToken(String token) {
        return sessionRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("会话不存在: " + token));
    }

    /**
     * 验证会话有效性
     *
     * @param token 会话令牌
     * @return 有效返回 true，否则抛出异常
     * @throws AuthenticationException 会话无效（不存在、已过期或已撤销）
     */
    @Transactional(readOnly = true)
    public boolean validateSession(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException("会话不存在"));

        if (!session.isValid()) {
            String reason = session.isRevoked() ? "会话已撤销" : "会话已过期";
            throw new AuthenticationException(reason);
        }

        return true;
    }

    /**
     * 撤销会话
     *
     * @param token 会话令牌
     * @throws NotFoundException 会话不存在
     */
    public void revokeSession(String token) {
        log.info("Revoking session: token={}***",
            token.length() > 16 ? token.substring(0, 16) : token);
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("会话不存在"));

        session.revoke();
        sessionRepository.save(session);
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
