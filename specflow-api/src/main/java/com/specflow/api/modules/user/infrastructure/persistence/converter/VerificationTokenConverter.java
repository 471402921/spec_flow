package com.specflow.api.modules.user.infrastructure.persistence.converter;

import com.specflow.api.modules.user.domain.entity.VerificationToken;
import com.specflow.api.modules.user.infrastructure.persistence.VerificationTokenDO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * VerificationToken DO ↔ Entity 转换器
 *
 * <p>职责：
 * - 在 Infrastructure 层完成 DO 与 Entity 的双向转换
 * - 处理 LocalDateTime ↔ Instant 的时区转换
 * - 处理 Type 枚举与 String 的转换
 * - 确保 Domain 层完全不知道 DO 的存在
 */
public class VerificationTokenConverter {

    /**
     * DO 转 Domain Entity
     */
    public static VerificationToken toDomain(VerificationTokenDO tokenDO) {
        if (tokenDO == null) {
            return null;
        }
        return new VerificationToken(
                tokenDO.getId(),
                tokenDO.getToken(),
                tokenDO.getUserId(),
                toType(tokenDO.getType()),
                tokenDO.getEmail(),
                tokenDO.getUsed() != null ? tokenDO.getUsed() : false,
                toInstant(tokenDO.getExpiredAt()),
                toInstant(tokenDO.getCreatedAt()),
                toInstant(tokenDO.getUpdatedAt())
        );
    }

    /**
     * Domain Entity 转 DO
     */
    public static VerificationTokenDO toDataObject(VerificationToken token) {
        if (token == null) {
            return null;
        }
        return new VerificationTokenDO(
                token.getId(),
                token.getToken(),
                token.getUserId(),
                toString(token.getType()),
                token.getEmail(),
                token.isUsed(),
                toLocalDateTime(token.getExpiredAt()),
                toLocalDateTime(token.getCreatedAt()),
                toLocalDateTime(token.getUpdatedAt())
        );
    }

    // ==================== 枚举转换辅助方法 ====================

    private static VerificationToken.Type toType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return VerificationToken.Type.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String toString(VerificationToken.Type type) {
        return type == null ? null : type.name();
    }

    // ==================== 时间转换辅助方法 ====================

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}
