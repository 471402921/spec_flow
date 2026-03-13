package com.specflow.api.modules.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.specflow.api.modules.auth.domain.entity.Session;
import com.specflow.api.modules.auth.domain.repository.SessionRepository;
import com.specflow.api.modules.auth.infrastructure.persistence.converter.SessionConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Session 仓储实现
 *
 * 职责：
 * - 使用 MyBatis-Plus 完成数据库操作
 * - 通过 Converter 完成 DO ↔ Entity 转换
 * - 实现 Domain 层定义的仓储接口
 */
@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepository {

    private final SessionMapper sessionMapper;

    @Override
    public void save(Session session) {
        SessionDO sessionDO = SessionConverter.toDataObject(session);
        if (sessionMapper.selectById(sessionDO.getId()) == null) {
            sessionMapper.insert(sessionDO);
        } else {
            sessionMapper.updateById(sessionDO);
        }
    }

    @Override
    public Optional<Session> findByToken(String token) {
        LambdaQueryWrapper<SessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionDO::getToken, token);
        SessionDO sessionDO = sessionMapper.selectOne(wrapper);
        return Optional.ofNullable(SessionConverter.toDomain(sessionDO));
    }

    @Override
    public Optional<Session> findById(String id) {
        SessionDO sessionDO = sessionMapper.selectById(id);
        return Optional.ofNullable(SessionConverter.toDomain(sessionDO));
    }

    @Override
    public void deleteById(String id) {
        sessionMapper.deleteById(id);
    }
}
