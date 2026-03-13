package com.specflow.api.modules.family.interfaces.dto;

import com.specflow.api.modules.family.domain.entity.Family;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 家庭信息响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyResponse {

    /**
     * 家庭 ID
     */
    private String id;

    /**
     * 家庭名称
     */
    private String name;

    /**
     * 当前主人 ID
     */
    private String ownerId;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 从领域实体转换
     */
    public static FamilyResponse fromDomain(Family family) {
        if (family == null) {
            return null;
        }
        return new FamilyResponse(
                family.getId(),
                family.getName(),
                family.getOwnerId(),
                family.getCreatedAt(),
                family.getUpdatedAt()
        );
    }
}
