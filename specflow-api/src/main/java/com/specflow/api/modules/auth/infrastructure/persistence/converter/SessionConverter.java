package com.specflow.api.modules.auth.infrastructure.persistence.converter;

import com.specflow.api.modules.auth.domain.entity.Session;
import com.specflow.api.modules.auth.infrastructure.persistence.SessionDO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Session DO ↔ Entity 转换器
 *
 * 职责：
 * - 在 Infrastructure 层完成 DO 与 Entity 的双向转换
 * - 处理 LocalDateTime ↔ Instant 的时区转换
 * - 确保 Domain 层完全不知道 DO 的存在
 */
public class SessionConverter {

    /**
     * DO 转 Domain Entity
     */
    public static Session toDomain(SessionDO sessionDO) {
        if (sessionDO == null) {
            return null;
        }
        return new Session(
                sessionDO.getId(),
                sessionDO.getUserId(),
                sessionDO.getToken(),
                toInstant(sessionDO.getExpiredAt()),
                sessionDO.getRevoked(),
                toInstant(sessionDO.getCreatedAt()),
                toInstant(sessionDO.getUpdatedAt())
        );
    }

    /**
     * Domain Entity 转 DO
     */
    public static SessionDO toDataObject(Session session) {
        if (session == null) {
            return null;
        }
        return new SessionDO(
                session.getId(),
                session.getUserId(),
                session.getToken(),
                toLocalDateTime(session.getExpiredAt()),
                session.isRevoked(),
                toLocalDateTime(session.getCreatedAt()),
                toLocalDateTime(session.getUpdatedAt())
        );
    }

    // ==================== 时间转换辅助方法 ====================

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}
