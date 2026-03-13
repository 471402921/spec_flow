package com.specflow.api.modules.family.infrastructure.persistence.converter;

import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import com.specflow.api.modules.family.infrastructure.persistence.FamilyInvitationDO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * FamilyInvitation 转换器
 *
 * <p>职责：
 * - 实现 FamilyInvitation 领域实体与 FamilyInvitationDO 数据对象之间的双向转换
 * - 处理 LocalDateTime 与 Instant 之间的时区转换（使用 UTC）
 * - 静态方法工具类，无状态，无需 Spring 管理
 */
public class FamilyInvitationConverter {

    private FamilyInvitationConverter() {
        // 工具类禁止实例化
    }

    /**
     * 将 DO 转换为领域实体
     *
     * @param dataObject DO 对象
     * @return 领域实体
     */
    public static FamilyInvitation toDomain(FamilyInvitationDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setId(dataObject.getId());
        invitation.setFamilyId(dataObject.getFamilyId());
        invitation.setCode(dataObject.getCode());
        invitation.setCreatedBy(dataObject.getCreatedBy());
        invitation.setRevoked(Boolean.TRUE.equals(dataObject.getRevoked()));
        invitation.setExpiredAt(toInstant(dataObject.getExpiredAt()));
        invitation.setCreatedAt(toInstant(dataObject.getCreatedAt()));
        invitation.setUpdatedAt(toInstant(dataObject.getUpdatedAt()));
        return invitation;
    }

    /**
     * 将领域实体转换为 DO
     *
     * @param entity 领域实体
     * @return DO 对象
     */
    public static FamilyInvitationDO toDataObject(FamilyInvitation entity) {
        if (entity == null) {
            return null;
        }
        FamilyInvitationDO dataObject = new FamilyInvitationDO();
        dataObject.setId(entity.getId());
        dataObject.setFamilyId(entity.getFamilyId());
        dataObject.setCode(entity.getCode());
        dataObject.setCreatedBy(entity.getCreatedBy());
        dataObject.setRevoked(entity.isRevoked());
        dataObject.setExpiredAt(toLocalDateTime(entity.getExpiredAt()));
        dataObject.setCreatedAt(toLocalDateTime(entity.getCreatedAt()));
        dataObject.setUpdatedAt(toLocalDateTime(entity.getUpdatedAt()));
        return dataObject;
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}
