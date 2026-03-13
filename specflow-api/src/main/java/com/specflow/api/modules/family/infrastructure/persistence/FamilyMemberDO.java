package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FamilyMember 数据对象（DO）- Infrastructure 层
 *
 * <p>职责：
 * - 承载 MyBatis-Plus 框架注解
 * - 对接数据库表结构
 * - 使用 LocalDateTime 对接 PostgreSQL TIMESTAMP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("family_members")
public class FamilyMemberDO {

    /**
     * 成员关系 ID（UUID 自动生成）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 家庭 ID
     */
    private String familyId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 角色：OWNER（主人）或 MEMBER（成员）
     */
    private String role;

    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
