package com.specflow.api.modules.auth.domain.repository;

import com.specflow.api.modules.auth.domain.entity.Session;

import java.util.Optional;

/**
 * Session 仓储接口（定义在 Domain 层）
 *
 * 遵循依赖倒置原则（DIP）：
 * - Domain 层定义接口
 * - Infrastructure 层提供实现
 * - Domain 层不依赖 Infrastructure 层
 */
public interface SessionRepository {

    /**
     * 保存或更新会话
     *
     * @param session 会话实体
     */
    void save(Session session);

    /**
     * 根据 Token 查询会话
     *
     * @param token 会话令牌
     * @return Session 实体（如果存在）
     */
    Optional<Session> findByToken(String token);

    /**
     * 根据 ID 查询会话
     *
     * @param id 会话 ID
     * @return Session 实体（如果存在）
     */
    Optional<Session> findById(String id);

    /**
     * 删除会话
     *
     * @param id 会话 ID
     */
    void deleteById(String id);
}
