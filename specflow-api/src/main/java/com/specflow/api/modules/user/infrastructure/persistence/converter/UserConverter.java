package com.specflow.api.modules.user.infrastructure.persistence.converter;

import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.infrastructure.persistence.UserDO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * User DO ↔ Entity 转换器
 *
 * <p>职责：
 * - 在 Infrastructure 层完成 DO 与 Entity 的双向转换
 * - 处理 LocalDateTime ↔ Instant 的时区转换
 * - 确保 Domain 层完全不知道 DO 的存在
 */
public class UserConverter {

    /**
     * DO 转 Domain Entity
     */
    public static User toDomain(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        return new User(
                userDO.getId(),
                userDO.getEmail(),
                userDO.getPasswordHash(),
                userDO.getNickname(),
                userDO.getAvatarUrl(),
                userDO.getDeleted() != null ? userDO.getDeleted() : false,
                toInstant(userDO.getDeletedAt()),
                toInstant(userDO.getCreatedAt()),
                toInstant(userDO.getUpdatedAt()),
                userDO.getEmailVerified() != null ? userDO.getEmailVerified() : false,
                userDO.getFailedLoginAttempts() != null ? userDO.getFailedLoginAttempts() : 0,
                toInstant(userDO.getLockedUntil())
        );
    }

    /**
     * Domain Entity 转 DO
     */
    public static UserDO toDataObject(User user) {
        if (user == null) {
            return null;
        }
        return new UserDO(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.isDeleted(),
                toLocalDateTime(user.getDeletedAt()),
                toLocalDateTime(user.getCreatedAt()),
                toLocalDateTime(user.getUpdatedAt()),
                user.isEmailVerified(),
                user.getFailedLoginAttempts(),
                toLocalDateTime(user.getLockedUntil())
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
