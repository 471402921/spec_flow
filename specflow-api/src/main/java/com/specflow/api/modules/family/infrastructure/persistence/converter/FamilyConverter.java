package com.specflow.api.modules.family.infrastructure.persistence.converter;

import com.specflow.api.modules.family.domain.entity.Family;
import com.specflow.api.modules.family.infrastructure.persistence.FamilyDO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Family 转换器
 *
 * <p>职责：
 * - 实现 Family 领域实体与 FamilyDO 数据对象之间的双向转换
 * - 处理 LocalDateTime 与 Instant 之间的时区转换（使用 UTC）
 * - 静态方法工具类，无状态，无需 Spring 管理
 */
public class FamilyConverter {

    private FamilyConverter() {
        // 工具类禁止实例化
    }

    /**
     * 将 DO 转换为领域实体
     *
     * @param dataObject DO 对象
     * @return 领域实体
     */
    public static Family toDomain(FamilyDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        Family family = new Family();
        family.setId(dataObject.getId());
        family.setName(dataObject.getName());
        family.setOwnerId(dataObject.getOwnerId());
        family.setCreatedAt(toInstant(dataObject.getCreatedAt()));
        family.setUpdatedAt(toInstant(dataObject.getUpdatedAt()));
        return family;
    }

    /**
     * 将领域实体转换为 DO
     *
     * @param entity 领域实体
     * @return DO 对象
     */
    public static FamilyDO toDataObject(Family entity) {
        if (entity == null) {
            return null;
        }
        FamilyDO dataObject = new FamilyDO();
        dataObject.setId(entity.getId());
        dataObject.setName(entity.getName());
        dataObject.setOwnerId(entity.getOwnerId());
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
