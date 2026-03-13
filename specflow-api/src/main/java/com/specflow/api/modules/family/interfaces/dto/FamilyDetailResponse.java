package com.specflow.api.modules.family.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 家庭详情响应 DTO（含成员列表）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDetailResponse {

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
     * 成员列表
     */
    private List<FamilyMemberInfo> members;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    /**
     * 成员信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FamilyMemberInfo {
        /**
         * 用户 ID
         */
        private String userId;

        /**
         * 用户昵称
         */
        private String nickname;

        /**
         * 角色（OWNER/MEMBER）
         */
        private String role;

        /**
         * 加入时间
         */
        private Instant joinedAt;
    }
}
