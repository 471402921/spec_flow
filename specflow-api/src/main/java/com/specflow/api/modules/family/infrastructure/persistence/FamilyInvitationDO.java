package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FamilyInvitation 数据对象（DO）- Infrastructure 层
 *
 * <p>职责：
 * - 承载 MyBatis-Plus 框架注解
 * - 对接数据库表结构
 * - 使用 LocalDateTime 对接 PostgreSQL TIMESTAMP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("family_invitations")
public class FamilyInvitationDO {

    /**
     * 邀请码 ID（UUID 自动生成）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 家庭 ID
     */
    private String familyId;

    /**
     * 邀请码（8位大写字母数字）
     */
    private String code;

    /**
     * 创建者 ID（家庭主人）
     */
    private String createdBy;

    /**
     * 是否已撤销
     */
    private Boolean revoked;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
