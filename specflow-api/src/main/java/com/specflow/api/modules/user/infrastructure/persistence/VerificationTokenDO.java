package com.specflow.api.modules.user.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VerificationToken 数据对象（DO）- Infrastructure 层
 *
 * <p>职责：
 * - 承载 MyBatis-Plus 框架注解
 * - 对接数据库表结构
 * - 使用 LocalDateTime 对接 PostgreSQL TIMESTAMP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("verification_tokens")
public class VerificationTokenDO {

    /**
     * 令牌 ID（UUID 自动生成）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 令牌值（用于 URL 中的 token，全局唯一）
     */
    private String token;

    /**
     * 关联用户 ID
     */
    private String userId;

    /**
     * 令牌类型（EMAIL_VERIFICATION, PASSWORD_RESET, EMAIL_CHANGE）
     */
    private String type;

    /**
     * 目标邮箱（用于邮箱修改时存储新邮箱）
     */
    private String email;

    /**
     * 是否已使用
     */
    private Boolean used;

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
