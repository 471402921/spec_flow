package com.specflow.api.modules.family.infrastructure.persistence.converter;

import com.specflow.api.modules.family.domain.entity.FamilyMember;
import com.specflow.api.modules.family.infrastructure.persistence.FamilyMemberDO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * FamilyMember 转换器
 *
 * <p>职责：
 * - 实现 FamilyMember 领域实体与 FamilyMemberDO 数据对象之间的双向转换
 * - 处理 LocalDateTime 与 Instant 之间的时区转换（使用 UTC）
 * - 处理 FamilyRole 枚举与 String 之间的转换
 * - 静态方法工具类，无状态，无需 Spring 管理
 */
public class FamilyMemberConverter {

    private FamilyMemberConverter() {
        // 工具类禁止实例化
    }

    /**
     * 将 DO 转换为领域实体
     *
     * @param dataObject DO 对象
     * @return 领域实体
     */
    public static FamilyMember toDomain(FamilyMemberDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        FamilyMember familyMember = new FamilyMember();
        familyMember.setId(dataObject.getId());
        familyMember.setFamilyId(dataObject.getFamilyId());
        familyMember.setUserId(dataObject.getUserId());
        familyMember.setRole(toRole(dataObject.getRole()));
        familyMember.setJoinedAt(toInstant(dataObject.getJoinedAt()));
        familyMember.setCreatedAt(toInstant(dataObject.getCreatedAt()));
        familyMember.setUpdatedAt(toInstant(dataObject.getUpdatedAt()));
        return familyMember;
    }

    /**
     * 将领域实体转换为 DO
     *
     * @param entity 领域实体
     * @return DO 对象
     */
    public static FamilyMemberDO toDataObject(FamilyMember entity) {
        if (entity == null) {
            return null;
        }
        FamilyMemberDO dataObject = new FamilyMemberDO();
        dataObject.setId(entity.getId());
        dataObject.setFamilyId(entity.getFamilyId());
        dataObject.setUserId(entity.getUserId());
        dataObject.setRole(entity.getRole() != null ? entity.getRole().name() : null);
        dataObject.setJoinedAt(toLocalDateTime(entity.getJoinedAt()));
        dataObject.setCreatedAt(toLocalDateTime(entity.getCreatedAt()));
        dataObject.setUpdatedAt(toLocalDateTime(entity.getUpdatedAt()));
        return dataObject;
    }

    private static FamilyMember.FamilyRole toRole(String role) {
        if (role == null) {
            return null;
        }
        try {
            return FamilyMember.FamilyRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}
